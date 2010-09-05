(comment "
  Tests for extracting and storing genomic variance data.
")
(ns rvar.variance-test
  (:use [rvar.variance] :reload-all)
  (:use [clojure.test]
        [clojure.java.io]))

(deftest test-parse-23andme
  (let [var-file "test/rvar/files/genome_23andme.txt"
        var-lines (line-seq (reader var-file))
        var-iter (parse-23andme var-lines)]
    (is (= "AA" (:genotype (first var-iter))))))
