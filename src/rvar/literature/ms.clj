;; Investigate data from the literature related to Multiple Sclerosis.
;;
;; Papers of interest:
;; - Genetic risk and a primary role for cell-mediated immune mechanisms
;;   in multiple sclerosis
;;   http://dx.doi.org/10.1038/nature10251

(ns rvar.literature.ms
  (:use [clojure.java.io]
        [rvar.upload :only [parse-23andme]])
  (:require [clojure-csv.core :as csv]
            [biomart-client.query :as biomart]
            [biomart-client.utils :as biomart-utils]))

;; Retrieve associated variant data from BioMart
(def *mart-url* "http://www.ensembl.org/biomart")

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

(defn -main [lit-file snp-file]
  "Compare literature CSV file with 23andme SNPs."
  (with-open [lit-rdr (reader lit-file)
              snp-rdr (reader snp-file)]
    (let [ms-map (lookup-by-rsid (parse-ms-nature-sup lit-rdr))]
      (doseq [snp-info (->> (parse-23andme (line-seq snp-rdr))
                            (filter #(contains? ms-map (:rsid %))))]
        (let [ms-info (ms-map (:rsid snp-info))]
          (println snp-info ms-info)
          (println (dbsnp-info (:rsid snp-info) (:gene ms-info))))))))
