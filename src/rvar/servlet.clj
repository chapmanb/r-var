(comment "
  Base server class providing web routing details.
")
(ns rvar.servlet
  (:use [compojure.core]
        [hiccup.core]
        [ring.util.servlet]
        [rvar.ring.multipart-params :as multipart]
        [rvar.templates]
        [rvar.variance])
  (:require [compojure.route :as route]
            [appengine.users :as users])
  (:gen-class :extends javax.servlet.http.HttpServlet))

(defroutes upload-routes
  (POST "/upload/23andme" request (upload-23andme request)))

(wrap! upload-routes
  multipart/wrap-multipart-params-memory
  users/wrap-with-user-info)

(defroutes r-var-web
  (GET "/" request (html (index-template request)))
  (GET "/personal" request (html (personal-template request)))
  (GET "/data/variations" request (var-list request))
  upload-routes
  (route/not-found "Page not found"))

(wrap! r-var-web
  users/wrap-with-user-info)

(defservice r-var-web)
