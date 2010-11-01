(comment "
  Rank variants based on publication information and variants per gene.

  Complex diseases have a large number of associated variations, and this
  attempts to assign rankings to those most likely to be of interest.

  Other interesting things we could do with this:
    - Group variants based on gene association or publication details
 
  Upload:
  ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --filename=data/variation-scores.csv --kind VariationScore

  Usage:
         lein run rate_variants.clj [data directory]
")

(ns rvar.scripts.rate-variants
  (:use [clojure.java.io])
  (:require [clojure.contrib.str-utils2 :as str2]))

(defn- gene-to-vrns [tx-vrn-file]
  "Provide a mapping of genes to variations."
  (letfn [(rm-dups [cur-map]
                   (reduce (fn [final-map [k v]]
                             (assoc final-map k (distinct v)))
                           {} cur-map))]
    (with-open [rdr (reader tx-vrn-file)]
      (rm-dups
        (reduce (fn [gene-map line]
                  (let [[_ gene vrn & _] (str2/split line #",")]
                    (assoc gene-map gene (cons vrn (get gene-map gene)))))
                {} (rest (line-seq rdr)))))))

(defn gene-score [gene-vrns]
  "Provide counts of variations per gene, keyed by variations."
  (reduce (fn [count-map [vrn cnt]]
            (assoc count-map vrn (max cnt (get count-map vrn 0))))
          {} (partition 2 (flatten
                            (for [[_ vrns] gene-vrns] 
                              (for [vrn vrns] [vrn (count vrns)]))))))

(defn vrn-ref-counts [vrn-ref-file]
  "Retrieve counts of papers referencing variations."
  (with-open [rdr (reader vrn-ref-file)]
    (reduce (fn [count-map line]
              (let [[vrn _ cnt _] (str2/split line #",")]
                (assoc count-map vrn (Integer/parseInt cnt))))
            {} (rest (line-seq rdr)))))

(defn- vrn-rank [scores]
  "Provide a total rank from a set of scores."
  (apply + scores))

(defn- vrn-scores [vrn gene-score ref-score]
  "Provide summarized score for variations based on gene and ref counts."
  (let [scores [(get gene-score vrn 0) (get ref-score vrn 0)]
        rank (vrn-rank scores)]
    (cons vrn (conj scores rank))))

(defn write-ratings [out-file vrn-file gene-score ref-score]
  "Write out a set of ratings for each variation."
  (let [header ["variation" "genescore" "refscore" "rank"]
        vrns (fn [rdr]
               (distinct
                  (for [line (rest (line-seq rdr))]
                    (first (str2/split line #",")))))]
    (with-open [rdr (reader vrn-file)
                w (writer out-file)]
      (.write w (str (str2/join "," header) \newline))
      (doseq [vrn (vrns rdr)]
        (let [score-line (vrn-scores vrn gene-score ref-score)]
          (.write w (str (str2/join "," score-line) \newline)))))))

(when *command-line-args*
  (let [data-dir (first *command-line-args*)
        out-file (file data-dir "variation-scores.csv")
        vrn-phn-file (file data-dir "variation-phenotypes.csv")
        gene-vrns (gene-to-vrns (file data-dir "tx-variation.csv"))
        gene-score (gene-score gene-vrns)
        ref-score (vrn-ref-counts (file data-dir "variation-lit.csv"))]
    (write-ratings out-file vrn-phn-file gene-score ref-score)))
