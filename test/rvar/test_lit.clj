(ns rvar.test-lit
  (:use [midje.sweet]
        [rvar.literature.ms]))

(facts "Ensure risk alleles match reference dbSNP alleles"
  (assess-risk-allele "T" "A/T") => nil
  (assess-risk-allele "G" "A/G") => "G"
  (assess-risk-allele "C" "A/G") => "G")
