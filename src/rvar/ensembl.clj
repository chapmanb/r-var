(comment "
  Provide access to variation data through the Ensembl public MySQL database.
")
(ns rvar.ensembl
  (:use [clojure.contrib.sql])
  (:require [clojure.contrib.str-utils2 :as str]))

(defn human-variation-db []
  "Connect to the public Ensembl variation database."
  (let [db-host "ensembldb.ensembl.org"
        db-port 5306
        db-name "homo_sapiens_variation_59_37d"]
    {:classname "com.mysql.jdbc.Driver"
     :subprotocol "mysql"
     :subname (str "//" db-host ":" db-port "/" db-name)
     :user "anonymous"
     :password ""}))

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
    (for [ph phenotypes]
      (let [pid (phenotype-id ph)
            sql "select * from variation_annotation where phenotype_id=?"]
        (with-query-results rs [sql pid]
          (doall (map #(clean-annotation base-name %) rs))))))))
