(comment "
  High level summary data for variants to feed into web displays.
")

(ns rvar.variant
  (:use [clojure.contrib.json :as json]
        [rvar.model])
  (:require [clojure.contrib.str-utils2 :as str2]))

(defn- clean-db-items [maps]
  "Remove db-specific keys and parents from a map."
  (for [cur-map maps]
    (-> cur-map
      (dissoc :key)
      (dissoc :parent))))

(defn vrn-list [request]
  "A JSON list of variations for the current user."
  (let [user (.getEmail ((request :appengine/user-info) :user))
        params (:params request)
        rows (Integer/parseInt (get params "rows"))
        start (* rows (- 1 (Integer/parseInt (get params "page"))))
        vars (clean-db-items (get-user-variations user))
        cur-vars (take rows (drop start vars))]
    (println cur-vars)
    (json/json-str {:total (count cur-vars) :page (get params "page")
                    :records (count cur-vars)
                    :rows cur-vars})))

(defn trait-vrn-list [request]
  "A list of variants associated with a phenotypic trait."
  (let [params (:query-params request)
        extra-check 1
        phenotype (-> params (get "phenotype"))
        limit (-> params (get "limit" "10") (Integer/parseInt))
        start (-> params (get "start" "0") (Integer/parseInt))]
    (->> (take (+ limit extra-check) (drop start (get-phenotype-vrns phenotype)))
      (map #(:variation %))
      (remove nil?)
      (distinct)
      (#(json/json-str {:variations (take limit %)
                        :hasless (> start 0) :hasmore (> (count %) limit)})))))

(defn- mod-view [mod-type mods]
  "Viewable details on an Ensembl database modification."
  (let [pep-changes (fn [mods] 
                      (str2/join "," (distinct (map #(:peptide_allele_string %) mods))))]
    (case mod-type
      "UPSTREAM" ["Near gene" ""]
      "DOWNSTREAM" ["Near gene" ""]
      "INTERGENIC" ["Near gene" ""]
      "INTRONIC" ["In gene, no protein change" ""]
      "5PRIME_UTR" ["In gene, no protein change" ""]
      "3PRIME_UTR" ["In gene, no protein change" ""]
      "PARTIAL_CODON" ["In gene, no protein change" ""]
      "SYNONYMOUS_CODING" ["In gene, no protein change" ""]
      "WITHIN_MATURE_miRNA" ["In non-coding gene"]
      "WITHIN_NON_CODING_GENE" ["In non-coding gene"]
      "REGULATORY_REGION" ["Gene regulatory region" ""]
      "SPLICE_SITE" ["Gene splicing" ""]
      "ESSENTIAL_SPLICE_SITE" ["Gene splicing" ""]
      "NON_SYNONYMOUS_CODING" ["Protein change" (pep-changes mods)]
      "FRAMESHIFT_CODING" ["Protein change" (pep-changes mods)]
      "STOP_LOST" ["Protein change" (pep-changes mods)]
      "STOP_GAINED" ["Protein change" (pep-changes mods)])))

(defn vrn-gene-changes [vrn]
  "A summary of genes affected by a variant, with change details."
  (let [mods-by-gene (group-by #(:gene_stable_id %) (get-vrn-transcripts vrn))]
    (for [gene (keys mods-by-gene)]
      (let [[gname gdesc] (get-gene gene)
            allele (:allele (first (get mods-by-gene gene)))
            mods-by-type (group-by #(:consequence_type %) (get mods-by-gene gene))
            mod-details (distinct (map #(apply mod-view %) mods-by-type))]
        [gname gdesc allele mod-details]))))
