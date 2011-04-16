;; Functionality related to retrieval and parsing of HLA proteins
;; General approach
;; 1. Retrieve transcripts and sequence regions for HLA genes.
;; 2. Identify variations by rs number that map in those regions and
;;    are assayed by 23andme.
;; 3. Build table of rs number, associated HLA gene name and amino
;;    acid change.
;; 4. Associate changes with HLA genotypes.
;; 5. Use this association to genotype HLA

(ns rvar.hla
  (:use [clojure.contrib.duck-streams :only [reader]]
        [net.cgrand.enlive-html :only [html-resource select text]])
  (:require [biomart-client.query :as biomart]
            [biomart-client.utils :as biomart-utils]))

(defn ensembl-from-hgnc [hgnc]
  "Retrieve Ensembl gene ID from HGNC name using BioMart."
  (let [mart-url "http://www.ensembl.org/biomart"
        ds-name "hsapiens_gene_ensembl"]
    (-> (biomart-utils/martservice-url mart-url)
        (biomart/query {:dataset ds-name
                        :filter {:chromosome_name "6" :hgnc_symbol hgnc}
                        :attrs ["ensembl_gene_id" "hgnc_symbol"]})
        first
        (get "ensembl_gene_id"))))

(defn hla-names []
  "Retrieve list of HLA names from online repository by screen scraping."
  (let [hla-url "http://hla.alleles.org/genes/index.html"
        html (-> hla-url reader html-resource)]
    (map text (-> html
                  (select [:table.tablesorter])
                  (select [:em])))))

(defn -main []
  (println (first (hla-names))))
