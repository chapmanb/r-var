(comment "
  Provide access to variation data through the Ensembl public MySQL database.
")
(ns rvar.ensembl
  (:use [clojure.contrib.sql])
  (:require [clojure.contrib.str-utils2 :as str]))

(def ensembl-version "59_37d")

(defn- ensembl-db [db-name]
  "Connection to Ensembl database with the given name."
  (let [db-host "ensembldb.ensembl.org"
        db-port 5306]
    {:classname "com.mysql.jdbc.Driver"
     :subprotocol "mysql"
     :subname (str "//" db-host ":" db-port "/" db-name)
     :user "anonymous"
     :password ""}))

(defn human-variation-db []
  "Connect to the public Ensembl variation database."
  (ensembl-db (str/join "_" ["homo_sapiens_variation" ensembl-version])))

(defn human-core-db []
  "Connnect to the Ensembl human core database."
  (ensembl-db (str/join "_" ["homo_sapiens_core" ensembl-version])))

(defn- phenotype-id [phenotype]
  "Fetch the identifier for a phenotype from Ensembl."
  (let [sql "select phenotype_id from phenotype where description=?"]
    (with-query-results rs [sql phenotype]
      (:phenotype_id (first rs)))))

(defn- clean-annotation [phenotype result]
  "Remove internal identifiers from an Ensembl annotation result."
  (for [vr (str/split (:variation_names result) #",")]
    (let [genes (map str/trim 
                     (str/split (str (:associated_gene result)) #","))]
      (-> (zipmap (keys result) (vals result))
        (assoc :variation vr)
        (assoc :phenotype phenotype)
        (assoc :associated_gene genes)
        (dissoc :variation_annotation_id)
        (dissoc :variation_id)
        (dissoc :phenotype_id)
        (dissoc :source_id)
        (dissoc :local_stable_id)
        (dissoc :variation_names)))))

(defn phenotype-variations [base-name & phenotypes]
  "Retrieve variation information for phenotypes."
  (with-connection (human-variation-db)
   (flatten
    (doall (for [ph phenotypes]
      (let [pid (phenotype-id ph)
            sql "select * from variation_annotation where phenotype_id=?"]
        (with-query-results rs [sql pid]
          (doall (map #(clean-annotation base-name %) rs)))))))))

(defn- variation-feature [vname]
  (let [sql "SELECT * from variation_feature WHERE variation_name=?"]
    (with-query-results rs [sql vname]
      (let [feat (first rs)]
        [(:variation_feature_id feat)
         (:allele_string feat)
         (:validation_status feat)]))))

(defn- clean-transcript [allele val-status vname result]
  "Provide a dictionary with transcript information."
  (-> (zipmap (keys result) (vals result))
    (assoc :allele allele)
    (assoc :validation_status val-status)
    (assoc :variation vname)
    ; Grab only the first consequence if multiple are listed
    (#(assoc % :consequence_type (first (str/split (:consequence_type %) #","))))
    (dissoc :transcript_variation_id)
    (dissoc :variation_feature_id)
    (dissoc :cds_start)
    (dissoc :cds_end)
    (dissoc :cdna_start)
    (dissoc :cdna_end)
    (dissoc :translation_start)
    (dissoc :translation_end)))

(defn- variation-transcripts [vname]
  "Retrieve transcripts changed by a genomic variation."
  (with-connection (human-variation-db)
    (let [[vf-id allele val-status] (variation-feature vname)
          sql "SELECT * from transcript_variation WHERE variation_feature_id=?"]
      (with-query-results rs [sql vf-id]
        (doall (map #(clean-transcript allele val-status vname %) rs))))))

(defn- ensembl-tx-id [tx-stable-id]
  "Retrieve internal ensembl transcript ID from a public ID."
  (let [sql "select transcript_id FROM transcript_stable_id WHERE stable_id=?"]
    (with-query-results rs [sql tx-stable-id]
      (:transcript_id (first rs)))))

(defn- ensembl-gene-stable [gene-id]
  "Retrieve a stable ensembl identifier from an internal gene id."
  (let [sql "SELECT stable_id FROM gene_stable_id WHERE gene_id=?"]
    (with-query-results rs [sql gene-id]
      (:stable_id (first rs)))))

(defn- ensembl-gene-id [tx-stable-id]
  "Retrieve internal ensembl gene id related to a transcript."
  (let [tx-id (ensembl-tx-id tx-stable-id)
        sql "select gene_id FROM transcript WHERE transcript_id=?"]
    (with-query-results rs [sql tx-id]
      (:gene_id (first rs)))))

(defn- ensembl-gene-data [tx-stable-id]
  "Retrieve gene information given a transcript."
  (let [gene-id (ensembl-gene-id tx-stable-id)
        gene-stable-id (ensembl-gene-stable gene-id)
        sql "select x.display_label, x.description 
             FROM xref x, object_xref ox, external_db e
             WHERE ox.xref_id = x.xref_id AND e.external_db_id = x.external_db_id 
             AND ox.ensembl_id=? AND ox.ensembl_object_type=? AND e.type=?"]
    (with-query-results rs [sql gene-id "Gene" "PRIMARY_DB_SYNONYM"]
      {:name (:display_label (first rs))
       :description (:description (first rs))
       :gene_stable_id gene-stable-id})))

(defn- transcript-and-genes [vname]
  "Return a dictionary of transcript and genes for a variant."
  (let [var-txs (variation-transcripts vname)]
    (with-connection (human-core-db)
      (doall
        (for [var-tx var-txs]
          [var-tx (ensembl-gene-data (:transcript_stable_id var-tx))])))))

(defn variation-genes [vname]
  "Identify genes and changes associated with a variation."
  ; Map gene identifiers to transcripts and clean up commas
  (let [clean-commas (fn [input]
                       (if input
                         (str/replace input "," "")
                         input))
        xref-gene (fn [var-tx gene]
                     [(-> var-tx
                        (assoc :gene_stable_id (:gene_stable_id gene)))
                      (-> gene
                        (assoc :description (clean-commas (:description gene))))])]
    ; Organize as map of genes to transcripts
    (reduce (fn [all-genes [var-tx gene]] 
              (assoc all-genes gene (cons var-tx (get all-genes gene))))
            {} (map #(apply xref-gene %) (transcript-and-genes vname)))))
