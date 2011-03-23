;; Functionality related to retrieval and parsing of HLA proteins
(ns rvar.hla
  (:use [clojure.contrib.duck-streams :only [reader]]
        [net.cgrand.enlive-html :only [html-resource select text]])
  (:require [biomart-client.core :as biomart]))

(defn ensembl-from-hgnc [hgnc]
  "Retrieve Ensembl gene ID from HGNC name using BioMart."
  (let [mart-url "http://www.ensembl.org/biomart"
        ds-name "hsapiens_gene_ensembl"]
    (-> (biomart/martservice mart-url)
        (biomart/dataset ds-name)
        (biomart/query :filter {:chromosome_name "6" :hgnc_symbol hgnc}
                       :attrs ["ensembl_gene_id" "hgnc_symbol"])
        ffirst)))

(defn hla-names []
  "Retrieve list of HLA names from online repository by screen scraping."
  (let [hla-url "http://hla.alleles.org/genes/index.html"
        html (-> hla-url reader html-resource)]
    (map text (-> html
                  (select [:table.tablesorter])
                  (select [:em])))))

(defn -main []
  (println (first (hla-names))))