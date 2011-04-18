;; Retrieve variation information from UniProt records
;;
;; Some HLA records of interest
;; P30460 -- HLA-B*8
;; P01912 -- HLA-DRB1*03
;; P30508 -- test example, nil description

(ns rvar.uniprot
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            [clojure.contrib.str-utils2 :as str2])
  (:use [clojure.java.io]
        [clojure.contrib.zip-filter.xml :only [xml-> xml1-> attr]]
        [rvar.upload :only [file-23andme]]))

(defn- uniprot-zipper [uniprot-id]
  "Retrieve a zipper object for parsing from a uniprot ID."
  (let [uniprot-url (str "http://www.uniprot.org/uniprot/" uniprot-id ".xml")]
    (-> uniprot-url
        reader
        org.xml.sax.InputSource.
        xml/parse
        zip/xml-zip)))

(defn- vrns-from-zipper [uniprot-zip]
  "Retrieve rs variations from zippered Uniport XML."
  (let [features (map (juxt
                       #(xml1-> % (attr :type))
                       #(xml1-> % (attr :description)))
                      (xml-> uniprot-zip :entry :feature))
        parse-dbsnp (fn [feature-attrs]
                      (->> feature-attrs
                           second
                           (re-seq #"dbSNP:rs\d+")
                           first))
        rs-only (fn [dbsnp-str] (second (str2/split dbsnp-str #":")))]
    (->> features
         (filter #(= "sequence variant" (first %)))
         (filter #(not (nil? (second %))))
         (map parse-dbsnp)
         (remove nil?)
         (map rs-only)
         )))

(defn uniprot-vrns [uniprot-id]
  "Retrieve rs variation from a uniprot identifier."
  (vrns-from-zipper (uniprot-zipper uniprot-id)))

(defn uniprot-23anmdme-snps [fname-23anmdme uniprot-id]
  "Retrieve 23andme SNPs present in uniprot record."
  (let [target-vrns (uniprot-vrns uniprot-id)]
    (filter #(.contains target-vrns (:rsid %))
            (file-23andme fname-23anmdme))))

(defn scan-uniprot-ids [uniprot-id-file fname-23anmdme]
  "Provided file of uniprot IDs, search for ones with 23andme assayed SNPs."
  (doseq [line-id (line-seq (reader uniprot-id-file))]
    (let [vrns (uniprot-23anmdme-snps fname-23anmdme line-id)]
      (println line-id)
      (if (seq vrns)
        (println vrns)))))
