;; Investigate data from the literature related to Multiple Sclerosis.
;;
;; Papers of interest:
;; - Genetic risk and a primary role for cell-mediated immune mechanisms
;;   in multiple sclerosis
;;   http://dx.doi.org/10.1038/nature10251

(ns rvar.literature.ms
  (:use [clojure.java.io]
        [clojure.string :only [split]]
        [rvar.upload :only [parse-23andme]])
  (:require [clojure-csv.core :as csv]
            [biomart-client.query :as biomart]
            [biomart-client.utils :as biomart-utils]))

;; Retrieve associated variant data from BioMart
(def *mart-url* "http://uswest.ensembl.org/biomart")

(defn ensembl-gene-info [gene-name]
  "Retrieve Ensembl gene ID from the provided symbol"
  (let [ds-name "hsapiens_gene_ensembl"]
    (-> (biomart-utils/martservice-url *mart-url*)
        (biomart/query {:dataset ds-name
                        :filter {:hgnc_symbol gene-name}
                        :attrs ["ensembl_gene_id" "strand"]})
        first)))

(defn dbsnp-info [rs-id gene-name]
  "Retrieve variation details given a dbSNP rs name."
  (let [ds-name "hsapiens_snp"
        gene-info (ensembl-gene-info gene-name)]
    (-> (biomart-utils/martservice-url *mart-url*)
        (biomart/query {:dataset ds-name
                        :filter {:snp_filter rs-id}
                        :attrs ["refsnp_id" "allele" "chrom_strand"]})
        first
        (#(merge % gene-info)))))

(defn parse-ms-nature-sup [rdr]
  "Parse supplemental table from MS Nature paper"
  (let [line-info [:chr :rsid :start :gene :risk-allele]]
    (->> rdr
         line-seq
         rest
         (map csv/parse-csv)
         (map first)
         (map (partial take 5))
         (map #(zipmap line-info %)))))

(defn lookup-by-rsid [xs]
  "Prepare map allowing selection by the variation rsid"
  (zipmap (map :rsid xs) xs))

;; Ensure risk-alleles are on the same strand as dbSNP reference 

(defn- complement-base [b]
  (let [b-map {"A" "T"
               "T" "A"
               "G" "C"
               "C" "G"}]
    (get b-map b)))

(defn- are-complements? [a b]
  (= (complement-base a) b))

(defn assess-risk-allele [risk-allele dbsnp-allele]
  "Retrieve correct strand risk allele given reference dbSNPs."
  (let [dbsnp-alleles (set (split dbsnp-allele #"/"))
        comp-risk (complement-base risk-allele)]
    (if-not (apply are-complements? dbsnp-alleles)
      (cond (contains? dbsnp-alleles risk-allele) risk-allele
            (contains? dbsnp-alleles comp-risk) comp-risk))))

(defn -main [lit-file snp-file]
  "Compare literature CSV file with 23andme SNPs."
  (with-open [lit-rdr (reader lit-file)
              snp-rdr (reader snp-file)]
    (let [ms-map (lookup-by-rsid (parse-ms-nature-sup lit-rdr))]
      (doseq [snp-info (->> (parse-23andme (line-seq snp-rdr))
                            (filter #(contains? ms-map (:rsid %))))]
        (let [ms-info (ms-map (:rsid snp-info))
              dbsnp-map (dbsnp-info (:rsid snp-info) (:gene ms-info))
              risk-allele (assess-risk-allele (:risk-allele ms-info)
                                              (get dbsnp-map "allele"))
              snp-alleles (set (map str (seq (:genotype snp-info))))]
          (if (nil? risk-allele)
            (println "Could not evaluate" snp-info ms-info dbsnp-map))
          (if (contains? snp-alleles risk-allele)
            (do 
              (println snp-info ms-info dbsnp-map)
              (println "----"))))))))
