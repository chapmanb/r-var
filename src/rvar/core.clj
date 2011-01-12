(comment "
  Base server class providing web routing details.
")
(ns rvar.core
  (:use [compojure.core]
        [hiccup.core]
        [ring.util.servlet]
        [rvar.templates]
        [rvar.variant]
        [rvar.upload]
        [appengine-magic.multipart-params :only [wrap-multipart-params]])
  (:require [compojure.route :as route]
            [appengine-magic.core :as ae]))

(defroutes r-var-web
  (GET "/" request (html (index-template request)))
  (GET "/varview" request (html (variation-template request)))
  (GET "/health" request (html (health-template request)))
  (GET "/health/variations" request (html (trait-vrn-list request)))
  (GET "/about" request (html (about-template request)))
  (GET "/personal" request (html (personal-template request)))
  (GET "/personal/genotype" request (user-vrn-genotypes request))
  (GET "/personal/upload" request (html (personal-upload request)))
  (POST "/upload/23andme" request (wrap-multipart-params upload-23andme))
  (route/not-found "Page not found"))

(ae/def-appengine-app rvar-app #'r-var-web)
