(ns rvar.servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use [rvar.core]
        [appengine-magic.servlet :only [make-servlet-service-method]]))

(defn -service [this request response]
  ((make-servlet-service-method rvar-app) this request response))
