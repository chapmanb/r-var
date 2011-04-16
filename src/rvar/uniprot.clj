;; Retrieve variation information from UniProt records

(ns rvar.uniprot
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            [clojure.contrib.str-utils2 :as str2])
  (:use [clojure.contrib.zip-filter.xml :only [xml-> xml1->]]
        [clojure.contrib.duck-streams :only [reader]]))

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
         (map parse-dbsnp)
         (remove nil?)
         (map rs-only)
         )))

(defn uniprot-vrns [uniprot-id]
  "Retrieve rs variation from a uniprot identifier."
  (vrns-from-zipper (uniprot-zipper uniprot-id)))
