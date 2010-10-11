(comment "
 Data store models representing objects of interest with key/val pairs.
")
(ns rvar.model
  (:use [appengine.datastore]))

(defn get-user [email]
  "Get or create a database user with the given email address."
  (let [user-query (select "user" where (= :email email))]
    (if (empty? user-query)
      (create-entity {:kind "user" :email email})
      (first user-query))))

(defn load-var-group [user fname]
  (with-commit-transaction
    (create-entity {:kind "vargroup" :parent (:key user) :filename fname})))

(defn get-user-variations [email]
  "Retrieve a lazy list of variation objects for the given user."
  (let [user (get-user email)]
    (flatten
      (for [var-group (select "vargroup" where (= :parent (:key user)))]
        (for [cur-var (select "variation" where (= :parent (:key var-group)))]
          cur-var)))))

(defn load-variances [user fname variances]
  "Load a lazy stream of variance information into the datastore."
  (let [group (load-var-group user fname)]
    (with-commit-transaction
      (doseq [cur-var (take 1 variances)]
        (create-entity {:kind "variation" :parent (:key group)
                        :start (:start cur-var) :end (:end cur-var)
                        :chrom (:chr cur-var) :genotype (:genotype cur-var)
                        :id (:id cur-var)
                        })))
    group))

(defn get-phenotypes []
  "Retrieve top level phenotypes from the datastore."
  (for [p-data (select "Phenotype")]
    (:name p-data)))

(defn get-phenotype-vrns [phenotype]
  "Retrieve variation data associated with the phenotype."
  (for [phenotype-var (select "VariationPhenotype" where (= :phenotype phenotype))]
    phenotype-var))

(defn get-vrn-transcripts [vrn]
  "Retrieve transcripts associated with a variation."
  (for [vrn-tx (select "VariationTranscript" where (= :variation vrn))]
    vrn-tx))

;(defentity User ()
;  ((email)
;   (name)))
;
;(defentity Vargroup (User)
;  ((name)
;   (reference)
;   (filename)))
;
;(defentity Variation (Vargroup)
;  ((id)
;   (chrom)
;   (start)
;   (end)
;   (genotype)))
;
;(defentity Comment (Variation)
;  ((note)))
;
;(defentity VariationPhenotype
; ((variation)
;  (phenotype)
;  (study)
;  (study_type)
;  (associated_gene)
;  (associated_variant_risk_allele)
;  (risk_allele_freq_in_controls)
;  (p_value)))
;
;(defentitry Phenotype
; ((name)
;  (phenotypes)))
