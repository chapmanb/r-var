(comment "
  Tests for retrieving variation information from Ensembl.
")
(ns rvar.ensembl-test
  (:use [rvar.ensembl] :reload-all)
  (:use [clojure.test]
        [clojure.contrib.sql]))

(deftest test-get-variation
  (let [var-id  "rs6897932"
        db (human-variation-db)]
    (with-connection db
      (with-query-results rs ["select * from variation WHERE name=?" var-id]
        (is (= "cluster,freq,doublehit,hapmap,1000Genome,precious"
               (:validation_status (first rs))))))))

(deftest test-var-by-phenotype
  (let [phenotype "Primary biliary cirrhosis"]
      (let [vars (phenotype-variations phenotype phenotype)]
        (is (> (count vars) 50))
        (is (= "rs4679904" (:variation (first vars)))))))

(deftest test-variance-genes
  (let [vname "rs6897932"]
  ;(let [vname "rs1815739"]
    (println (variation-genes vname))))
