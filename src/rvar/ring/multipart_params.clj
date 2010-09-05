(comment "
  Adjusted from ring multipart_params code to work on GAE, which can't use
  disk file storage. This will only work with uploaded text files; need
  to think about how to handle binary files.

  Uses fileupload ideas from twister to avoid writing to disk:
    http://github.com/garyburd/clj-twister
")
(ns rvar.ring.multipart-params
  "Parse multipart upload into params."
  (:use [clojure.contrib.def :only (defvar-)]
        [clojure.java.io]
        [ring.middleware.params :only (assoc-param)])
  (:import (org.apache.commons.fileupload
             FileUpload RequestContext FileItemIterator FileUploadException FileItemStream)))

(defn- multipart-form?
  "Does a request have a multipart form?"
  [request]
  (if-let [#^String content-type (:content-type request)]
    (.startsWith content-type "multipart/form-data")))

(defn- request-context
  "Create a RequestContext object from a request map."
  {:tag RequestContext}
  [request encoding]
  (proxy [RequestContext] []
    (getContentType []       (:content-type request))
    (getContentLength []     (:content-length request))
    (getCharacterEncoding [] encoding)
    (getInputStream []       (:body request))))

(defn data-iter [item]
  (with-open [stream (.openStream item)]
    (let [buf (java.io.BufferedReader. (java.io.InputStreamReader. stream))]
      (line-seq buf))))

(defn- file-map [item]
  (with-meta
    {:filename     (.getName item)
     :content-type (.getContentType item)
     :data         (data-iter item)}
    {:file-item item}))

(defn file-items [iter]
  (lazy-seq
      (if (.hasNext iter)
        (cons (.next iter) (file-items iter)))))

(defn parse-multipart-params
  "Parse a map of multipart parameters from the request."
  [request encoding]
  (reduce
    (fn [param-map item]
      (assoc-param param-map
        (.getFieldName item)
        (if (.isFormField item)
          (if (.get item) (.getString item))
          (file-map item))))
    {}
    (let [context (request-context request encoding)
          ^FileItemIterator iter (.getItemIterator (FileUpload.) context)]
      (file-items iter))))

(defn wrap-multipart-params-memory
  "Middleware to parse multipart parameters from a request. Adds the
  following keys to the request map:
    :multipart-params - a map of multipart parameters
    :params           - a merged map of all types of parameter
  Takes an optional configuration map. Recognized keys are:
    :encoding - character encoding to use for multipart parsing. If not
                specified, uses the request character encoding, or \"UTF-8\"
                if no request character encoding is set."
  [handler & [opts]]
  (fn [request]
    (let [encoding (or (:encoding opts)
                       (:character-encoding request)
                       "UTF-8")
          params   (if (multipart-form? request)
                     (parse-multipart-params request encoding)
                     {})
          request  (merge-with merge request
                     {:multipart-params params}
                     {:params params})]
      (handler request))))
