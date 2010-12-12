(comment "
 Data store models representing objects of interest with key/val pairs.
")
(ns rvar.model
  (:require [clojure.contrib.json :as json]
            [appengine-magic.services.datastore :as ds]))

; Entity definitions for all of the items stored in the appengine datastore

(ds/defentity Phenotype [name ensembl snpedia])
(ds/defentity VariationScore [variation phenotype genescore refscore rank])
(ds/defentity VariationGroup [phenotype gid group score variations])
(ds/defentity VariationTranscript [transcript_stable_id gene_stable_id
                                   variation allele peptide_allele_string
                                   consequence_type])
(ds/defentity VariationProviders [variation providers])
(ds/defentity VariationLit [variation phenotype numrefs keywords])
(ds/defentity Gene [gene_stable_id name description])

; High level access functions instead of direct datastore access

(defn get-phenotypes []
  "Retrieve top level phenotypes from the datastore."
  (sort
    (for [p-data (ds/query :kind Phenotype)]
      (:name p-data))))

(defn get-phenotype-vrns [phn]
  "Retrieve variation data associated with the phenotype."
  (for [phn-var (ds/query :kind VariationScore :filter (= :phenotype phn)
                          :sort [[:rank :desc]])]
      phn-var))

(defn get-phenotype-vrn-groups [phn]
  "Retrieve variation groups associated with a phenotype."
  (for [phn-grp (ds/query :kind VariationGroup :filter (= :phenotype phn)
                          :sort [[:score :desc]])]
    phn-grp))

(defn get-group-vrns [phn gid]
  "Retrieve variations associated with a phenotype and group."
  (let [group (first (ds/query :kind VariationGroup 
                               :filter [(= :phenotype phn) (= :gid gid)]))]
    (:variations group)))

(defn get-vrn-transcripts [vrn]
  "Retrieve transcripts associated with a variation."
  (for [vrn-tx (ds/query :kind VariationTranscript :filter (= :variation vrn))]
    vrn-tx))

(defn get-vrn-providers [vrn]
  "Companies that provide genotyping of a variation."
  (let [vrn-pro (first (ds/query :kind VariationProviders 
                                 :filter (= :variation vrn)))]
    (if-not (nil? vrn-pro)
      (:providers vrn-pro))))

(defn get-gene [gene-id]
  "Gene name and description via the ensembl stable gene id."
  (first
    (for [gene (ds/query :kind Gene :filter (= :gene_stable_id gene-id))]
      (select-keys gene [:name :description]))))

(defn get-variant-rank [vrn]
  "Retrieve the rank score for a variant"
  (let [db-item (first (ds/query :kind VariationScore 
                                 :filter (= :variation vrn)))]
    (if-not (nil? db-item)
      (:rank db-item)
      0.0)))

(defn get-variant-keywords [vrn]
  (let [db-item (first (ds/query :kind VariationLit
                                 :filter (= :variation vrn)))]
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
