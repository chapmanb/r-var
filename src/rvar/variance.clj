(comment "
Uploading, process and store details on genomic variance.
")

(ns rvar.variance
  (:use [clojure.java.io]
        [clojure.contrib.str-utils])
  (:require [rvar.model :as model]
            [appengine.datastore :as ds])
  (:import (java.io InputStreamReader
      ByteArrayOutputStream ByteArrayInputStream ObjectInputStream)))

(defn parse-23andme [line-iter]
  "Lazily produce hash-map of variances from 23andMe text file."
  (let [line-info [:rsid :chr :start :genotype]]
    (for [line line-iter :when (not= \# (first line))]
      (zipmap line-info (re-split #"\t" line)))))

(defn variances-23andme [line-iter]
  "Convert raw 23andme data into standard variance objects."
  (for [raw (parse-23andme line-iter)]
    (let [start (Integer/parseInt (:start raw))]
      {:id (:rsid raw) :chr (:chr raw) :start start
       :end (+ 1 start) :genotype (:genotype raw)})))

(defn upload-23andme [request]
  "Upload a file of 23 and me SNPs."
  (let [file-upload (get (request :multipart-params) "ufile")
        user (model/get-user (.getEmail ((request :appengine/user-info) :user)))
        fname (file-upload :filename)
        variances (variances-23andme (file-upload :data))]
    (str (model/load-variances user fname variances))))
