(comment "
 Data store models representing objects of interest with key/val pairs.
")
(ns rvar.model
  (:use [appengine.datastore])
  (:require [clojure.contrib.json :as json]))

(defn get-phenotypes []
  "Retrieve top level phenotypes from the datastore."
  (sort
    (for [p-data (select "Phenotype")]
      (:name p-data))))

(defn get-phenotype-vrns [phn]
  "Retrieve variation data associated with the phenotype."
  (for [phn-var (select "VariationScore" where (= :phenotype phn)
                        order-by (:rank :desc))]
      phn-var))

(defn get-phenotype-vrn-groups [phn]
  "Retrieve variation groups associated with a phenotype."
  (for [phn-grp (select "VariationGroup" where (= :phenotype phn)
                        order-by (:score :desc))]
    phn-grp))

(defn get-vrn-phenotypes [vrn]
  "Retrieve phenotypes associated with a variation."
  (distinct
    (for [var-phn (select "VariationScore" where (= :variation vrn))]
      (:phenotype var-phn))))

(defn get-vrn-transcripts [vrn]
  "Retrieve transcripts associated with a variation."
  (for [vrn-tx (select "VariationTranscript" where (= :variation vrn))]
    vrn-tx))

(defn get-vrn-providers [vrn]
  "Companies that provide genotyping of a variation."
  (let [vrn-pro (first (select "VariationProviders" where (= :variation vrn)))]
    (if-not (nil? vrn-pro)
      (:providers vrn-pro))))

(defn get-gene [gene-id]
  "Gene name and description via the ensembl stable gene id."
  (first
    (for [gene (select "Gene" where (= :gene_stable_id gene-id))]
      [(:name gene) (:description gene)])))

(defn get-variant-rank [vrn]
  "Retrieve the rank score for a variant"
  (let [db-item (first (select "VariationScore" where (= :variation vrn)))]
    (if-not (nil? db-item)
      (Integer/parseInt (:rank db-item))
      0)))

(defn get-variant-keywords [vrn]
  (let [db-item (first (select "VariationLit" where (= :variation vrn)))]
    (json/read-json (.getValue (:keywords db-item)))))

; Support for uploaded variations for a user. Needs to be reworked.
;
;(defn get-user [email]
;  "Get or create a database user with the given email address."
;  (let [user-query (select "user" where (= :email email))]
;    (if (empty? user-query)
;      (create-entity {:kind "user" :email email})
;      (first user-query))))
;
;(defn load-var-group [user fname]
;  (with-commit-transaction
;    (create-entity {:kind "vargroup" :parent (:key user) :filename fname})))
;
;(defn get-user-variations [email]
;  "Retrieve a lazy list of variation objects for the given user."
;  (let [user (get-user email)]
;    (flatten
;      (for [var-group (select "vargroup" where (= :parent (:key user)))]
;        (for [cur-var (select "variation" where (= :parent (:key var-group)))]
;          cur-var)))))
;
;(defn load-variances [user fname variances]
;  "Load a lazy stream of variance information into the datastore."
;  (let [group (load-var-group user fname)]
;    (with-commit-transaction
;      (doseq [cur-var (take 1 variances)]
;        (create-entity {:kind "variation" :parent (:key group)
;                        :start (:start cur-var) :end (:end cur-var)
;                        :chrom (:chr cur-var) :genotype (:genotype cur-var)
;                        :id (:id cur-var)
;                        })))
;    group))
