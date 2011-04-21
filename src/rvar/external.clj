(comment "
  Generate HTML link outs for external data references.
")

(ns rvar.external
  (:use [clojure.string :only [lower-case]]
        [hiccup.core]
        [ring.util.codec :only [url-encode]]
        [rvar.model])
  (:require [clojure.contrib.str-utils2 :as str2]))

(defn- ext-link [url content]
  [:a {:href url :target "_blank"} content])

(defn vrn-links [vrn]
  "External links for variations."
  [(ext-link (format "http://snpedia.com/index.php/%s" vrn) "SNPedia")
   (ext-link (format "http://www.ensembl.org/Homo_sapiens/Variation/Summary?source=dbSNP;v=%s"
                     vrn) "Ensembl")
   (ext-link (format "http://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=%s"
                     (str2/replace-first vrn #"rs" "")) "dbSNP")])

(defn vrn-providers [vrn]
  "Links to personal genome providers associated with variations."
  (for [pro (sort-by lower-case (get-vrn-providers vrn))]
    (case pro
      "23andMe" (ext-link (format "https://www.23andme.com/you/explorer/snp/?snp_name=%s" vrn) pro)
      "deCODEme" (ext-link "http://demo.decodeme.com/snp-look-up" pro)
      "Navigenics" (ext-link "http://www.navigenics.com/" pro))))

(defn wikipedia-link [term]
  "Link to wikipedia articles on terms of interest."
  (-> term
    (str2/replace " " "_")
    (url-encode)
    (#(ext-link (format "http://en.wikipedia.org/wiki/%s" %) term))))

(defn biogps-link [eid text]
  "Link to gene portal at BioGPS."
  (let [url "http://biogps.gnf.org/?query=%s"
        link-text (if (= text " ") eid text)]
    (ext-link (format url eid) link-text)))
