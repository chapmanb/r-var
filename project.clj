(defproject r-var "0.0.1-SNAPSHOT"
  :description "Share and explore our genomic variability."
  :namespaces [rvar.servlet]
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.4.1"]
                 [hiccup "0.2.6"]
                 [scriptjure "0.1.13"]
                 [ring/ring-jetty-adapter "0.2.5"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [appengine "0.4-SNAPSHOT"]]
  :dev-dependencies [[leiningen/lein-swank "1.2.0-SNAPSHOT"]
                     [lein-run "1.0.0-SNAPSHOT"]
                     [ring/ring-core "0.2.5"]]
  :repositories [["maven-gae-plugin" "http://maven-gae-plugin.googlecode.com/svn/repository"]]
  :compile-path "war/WEB-INF/classes"
  :library-path "war/WEB-INF/lib")
