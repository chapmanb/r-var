;; Tests for retrieving tested variations from genomic regions.
;;

(ns rvar.test-region
  (:use [rvar.upload] :reload-all)
  (:use [clojure.test]
        [clojure.java.io]))

(deftest test-parse-23andme
  (let [var-file "test/rvar/files/genome_23andme.txt"
        var-lines (line-seq (reader var-file))
        var-iter (parse-23andme var-lines)]
    (is (= "AA" (:genotype (first var-iter))))))
