(comment "
  Rank full list of keywords for each variation to emphasize literature details.

  Usage:
    lein run scripts/pub_refine_keywords.clj <data directory>
  
  Upload:
    ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --filename=data/variation-lit.csv --kind VariationLit
")

(ns rvar.scripts.pub-refine-keywords
  (:use [clojure.java.io])
  (:require [clojure.contrib.json :as json]
            [clojure.contrib.str-utils2 :as str2]
            [clojure-csv.core :as csv]))

(defn sum-kwd-count [pids kwds-by-pid]
  "Summed values of counts for all keywords in a list of pubmed ids."
  (letfn [(build-count [k-count [k v]]
                       (assoc k-count k (+ v (get k-count k 0))))
          (k-vals [pids kwds-by-pid]
                  (partition 2
                    (flatten
                      (for [pid pids]
                        (for [[k v] (get kwds-by-pid pid)]
                          [k v])))))]
    (reduce build-count {} (k-vals pids kwds-by-pid))))

(defn parse-vrn-phn [rdr]
  "Retrieve variations, phenotypes and pubmed IDs."
  (for [line (rest (line-seq rdr))]
    (let [[vrn phn pid-strs] (take 3 (first (csv/parse-csv line)))
          pids (for [pid-str (str2/split pid-strs #";")] (str2/replace pid-str "pubmed/" ""))]
      [vrn phn pids])))

(defn build-pids-by-phn [vrn-phn-file]
  "Accumulate PubMed identifiers keyed by phenotypes."
  (with-open [rdr (reader vrn-phn-file)]
    (loop [vrn-info (parse-vrn-phn rdr) phn-pids {}]
      (if (empty? vrn-info)
        (reduce (fn [clean [k v]] (assoc clean k (distinct (flatten v))))
                {} phn-pids)
        (let [[_ phn pids] (first vrn-info)]
          (recur (next vrn-info) (assoc phn-pids phn 
                                        (cons pids (get phn-pids phn [])))))))))

(defn build-kwds-by-phn [vrn-phn-file kwds-by-pid]
  "Mapping of keyword counts summarized by phenotype class."
  (reduce (fn [phn-to-kwd [phn pids]] (assoc phn-to-kwd phn 
                                             (sum-kwd-count pids kwds-by-pid)))
          {} (build-pids-by-phn vrn-phn-file)))

(defn build-general-kwd-filter [kwds-by-phn]
  "Retrieve keywords which are broadly applicable and less useful."
  (let [filter-pct 0.75
        kwds (reduce (fn [kwd-count k] (assoc kwd-count k (+ 1 (get kwd-count k 0))))
                     {} (flatten (for [m (vals kwds-by-phn)] (keys m))))
        kwds-rm (set (keys (filter 
                        (fn [[k v]] (> v (* (count kwds-by-phn) filter-pct))) 
                        kwds)))]
    (fn [to-filter]
      (reduce (fn [final [k v]] (if-not (contains? kwds-rm k) 
                                  (assoc final k v)
                                  final))
              {} to-filter))))

(defn parse-kwds-by-pid [kwd-file]
  "Retrieve a mapping of pubmed references to keyword lists."
  (letfn [(kwd-reader [rdr]
                      (for [line (rest (line-seq rdr))]
                        (let [[pid kwd-str] (first (csv/parse-csv line))
                              kwds (json/read-json kwd-str)]
                          [pid kwds])))
          (map-by-pid [pid-to-kwds [pid kwds]]
                      (assoc pid-to-kwds pid kwds))]
    (with-open [rdr (reader kwd-file)]
      (reduce map-by-pid {} (kwd-reader rdr)))))

(defn calc-kwd-score [orig-kwds phn-kwds gen-filter]
  "Calculate normalized keyword score based on uniqueness versus phenotype tags"
  (letfn [(norm-score [base overall]
                      (/ base overall))]
    (gen-filter
      (reduce (fn [norm-vals [k v]] (assoc norm-vals k (norm-score v (get phn-kwds k))))
              {} orig-kwds))))

(when *command-line-args*
  (let [data-dir (first *command-line-args*)
        kwd-file (file data-dir "lit-kwds.csv")
        out-file (file data-dir "variation-lit.csv")
        vrn-phn-file (file data-dir "variation-phenotypes.csv")
        kwds-by-pid (parse-kwds-by-pid kwd-file)
        kwds-by-phn (build-kwds-by-phn vrn-phn-file kwds-by-pid)
        gen-filter (build-general-kwd-filter kwds-by-phn)]
    (with-open [rdr (reader vrn-phn-file)
                w (writer out-file)]
      (.write w "variation,phenotype,numrefs,keywords\n")
      (doseq [[vrn phn pids] (parse-vrn-phn rdr)]
        (let [kwd-score (calc-kwd-score (sum-kwd-count pids kwds-by-pid)
                                        (get kwds-by-phn phn) gen-filter)]
          (.write w (str (csv/write-csv [[vrn phn (str (count pids))
                                          (json/json-str kwd-score)]]))))))))
