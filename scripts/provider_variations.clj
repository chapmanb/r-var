(comment "
  Organize variants that are queried by various personal genome providers.

  Usage:
    lein run provider_variations.clj <Provider example description file> <out dir>

  The provider description file is a comma separated file with names of providers
  and links to example files with the variants that are queried.
 
  Upload:
    ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --filename=data/variation-providers.csv --kind VariationProviders
")

(ns rvar.scripts.provider-variations
  (:use [clojure.java.io])
  (:require [clojure.contrib.str-utils2 :as str2]))

(defn vrns-from-file [rdr has-header vrns-to-get]
  "Retrieve all variations from the provider download file."
  (let [not-comment #(not (.startsWith % "#"))
        noc-lines (filter not-comment (line-seq rdr))
        work-lines (if (= has-header "yes") (rest noc-lines) noc-lines)]
    (filter #(contains? vrns-to-get %)
      (for [line work-lines]
        (-> line
          (#(first (str2/split % #"\t")))
          (#(first (str2/split % #",")))
          (#(first (str2/split % #"\("))))))))

(defn providers-by-vrn [p-info vrns-to-get]
  "Organize map with variations as keys and providers as values."
  (loop [p-info p-info, vrn-map {}]
    (if (empty? p-info)
      vrn-map
      (let [[provider fname has-hdr] (first p-info)]
        (with-open [prdr (reader fname)]
          (let [cur-map (reduce (fn [vmap vrn]
                                  (assoc vmap vrn (cons provider (get vmap vrn))))
                                vrn-map (vrns-from-file prdr has-hdr vrns-to-get))]
            (recur (rest p-info) cur-map)))))))

(defn parse-providers [rdr in-file]
  "Retrieve provider and example files from config."
  (let [dir (str2/join "/" (pop (apply vector (str2/split in-file #"/"))))]
    (for [line (line-seq rdr)]
      (let [[p f h] (str2/split line #",")]
        [p (format "%s/%s" dir f) h]))))

(defn vrns-of-interest [in-file]
  (with-open [rdr (reader in-file)]
    (set (distinct
      (for [line (line-seq rdr)]
        (first (str2/split line #",")))))))

(when *command-line-args*
  (let [provider-file (first *command-line-args*)
        data-dir (second *command-line-args*)
        vrns-to-get (vrns-of-interest (file data-dir "variation-phenotypes.csv"))]
    (with-open [rdr (reader provider-file)
                w (writer (file data-dir "variation-providers.csv"))]
      (.write w "variation,providers\n")
      (let [p-info (parse-providers rdr provider-file)]
        (doseq [[vrn providers] (providers-by-vrn p-info vrns-to-get)]
          (.write w (format "%s,%s\n" vrn (str2/join ";" providers))))))))
