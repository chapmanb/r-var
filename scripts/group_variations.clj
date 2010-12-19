(comment "
  Group together variants based on affected genes.

  Approach:
    - Sort variants into gene based groups based on locations in or close to
      affected genes.
    - Group together related genes sharing a large percentage of variants.
    - Associate one group with each variation.
    - Combine scores of all variations in a gene for a group score.
    - Output CSV of groups, scores and associated variations for each phenotype.

  Usage:
    lein run scripts/group_variations.clj <data directory>
")

(ns rvar.scripts.group-variations
  (:use [clojure.java.io]
        [clojure.set])
  (:require [clojure-csv.core :as csv]
            [clojure.contrib.str-utils2 :as str2]))

(defn- parse-gene-vrns [rdr]
  "Parse genes and variations from the tx-variation mapping file."
  (for [line (rest (line-seq rdr))]
    (let [[_ gene vrn] (take 3 (first (csv/parse-csv line)))]
      [gene vrn])))

(defn map-vrns-by-gene [in-file]
  "Provide a map of variations sorted by affected genes."
  (letfn [(combine-by-gene [gene-map [gene vrn]]
             (assoc gene-map gene
                    (cons vrn (get gene-map gene []))))]
    (with-open [rdr (reader in-file)]
      (reduce combine-by-gene {} (parse-gene-vrns rdr)))))

(defn vrns-overlap? [v1 v2]
  "Test overlap between two lists of variations."
  (let [ol-pct 0.75
        ol (count (intersection (set v1) (set v2)))]
    (or (>= ol (* ol-pct (count v1)))
        (>= ol (* ol-pct (count v2))))))

(defn map-gene-groups [groups]
  "Map gene groups for easy looking up gene."
  (reduce (fn [gene-map gs]
            (into gene-map (for [g gs] [g gs])))
          {} groups))

(defn group-genes [vrns-by-gene]
  {:post [(= (-> % keys count) (-> vrns-by-gene keys count))]}
  "Group genes together based on shared variations."
  (let [order-v-by-g (sort-by #(-> % second count) > vrns-by-gene)]
    (loop [g-vs order-v-by-g final [] finished #{}]
      (if (empty? g-vs)
        (map-gene-groups final)
        (let [[g1 v1] (first g-vs)
              g-v2s (filter #(vrns-overlap? v1 (second %)) (rest g-vs))
              g-group (filter #(nil? (get finished %)) (cons g1 (keys g-v2s)))]
          (recur (rest g-vs) 
                 (conj final g-group)
                 (into finished g-group)))))))

(defn- uniquify-vrns [vrns-by-gene]
  "Provide unique list of variations for each gene."
  (into {} (for [[g vrns] vrns-by-gene]
             [g (distinct vrns)])))

(defn- map-genes-by-vrn [tx-file]
  "Generate a map of all genes associated with a variation."
  (letfn [(combine-by-vrn [vrn-map [gene vrn]]
            (assoc vrn-map vrn
                   (cons gene (get vrn-map vrn []))))]
    (with-open [rdr (reader tx-file)]
      (reduce combine-by-vrn {} (parse-gene-vrns rdr)))))

(defn map-groups-by-vrn [tx-file org-genes]
  "Generate mapping of variations to a gene group."
  (loop [v-gs (map-genes-by-vrn tx-file) vrn-map {}]
    (if (empty? v-gs)
      vrn-map
      (let [[v gs] (first v-gs)
            ; pick the group with the largest number of genes
            group (last (sort-by count
                                  (distinct (map #(get org-genes %) gs))))]
        (recur (rest v-gs)
               (assoc vrn-map v group))))))

(defn parse-vrn-by-phn [rdr]
  "Parse CSV file of variations, organizing by phenotype."
  (group-by first
    (for [line (rest (line-seq rdr))]
      (let [[vrn phn _ _ rank] (first (csv/parse-csv line))]
        [phn vrn rank]))))

(defn create-groups [vrns groups-by-vrn]
  "Sort a list of variations into groups, keeping track of scores."
  (loop [vrns vrns groups {} scores {}]
    (if (empty? vrns)
      [groups scores]
      (let [[_ vrn score-str] (first vrns)
            score (Integer/parseInt score-str)
            g (get groups-by-vrn vrn (list vrn))]
        (recur (rest vrns)
               (assoc groups g (cons [vrn score] (get groups g [])))
               (assoc scores g (cons score (get scores g []))))))))

(defn write-group-scores [w vrn-info groups-by-vrn]
  "Write variants as group, including a score variable for ranking."
  (doseq [[phn items] vrn-info]
    (let [[vrn-groups scores] (create-groups items groups-by-vrn)]
      (doseq [[i [g vrn-scores]] (map-indexed vector vrn-groups)]
        (let [vrns (->> vrn-scores
                    (sort-by second >)
                    (map first))]
          ; XXX ToDo -- retrieve group identifiers matching previous
          ; database for consistency instead of using defaults here
          (.write w (str (csv/write-csv
                           [[phn (str i) (str2/join ";" g) 
                             (str (apply + (get scores g)))
                             (str2/join ";" vrns)]]))))))))

(when *command-line-args*
;(defn -main [& args]
  (let [data-dir (first *command-line-args*)
        vrn-file (file data-dir "variation-scores.csv")
        tx-file (file data-dir "tx-variation.csv")
        out-file (file data-dir "variation-groups.csv")
        groups-by-vrn (->> tx-file
                        map-vrns-by-gene
                        uniquify-vrns
                        group-genes
                        (map-groups-by-vrn tx-file))]
    (with-open [rdr (reader vrn-file)
                w (writer out-file)]
      (.write w (str (csv/write-csv 
                       [["phenotype" "gid" "group" "score" "variations"]])))
      (write-group-scores w (parse-vrn-by-phn rdr) groups-by-vrn))))
