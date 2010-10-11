(defproject r-var "0.0.1-SNAPSHOT"
  :description "Share and explore our genomic variability."
  :namespaces [rvar.servlet]
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.4.1"]
                 [hiccup "0.2.6"]
                 [scriptjure "0.1.17"]
                 [gaka "0.2.0"]
                 [ring/ring-jetty-adapter "0.2.5"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [appengine "0.4-SNAPSHOT"]]
  :dev-dependencies [[leiningen/lein-swank "1.2.0-SNAPSHOT"]
                     [lein-run "1.0.0"]
                     [com.google.appengine/appengine-api-1.0-sdk "1.3.7"]
                     [com.google.appengine/appengine-api-labs "1.3.7"]
                     [com.google.appengine/appengine-local-runtime "1.3.7"]
                     [com.google.appengine/appengine-api-stubs "1.3.7"]
                     [vimclojure/server "2.2.0"]
                     [ring/ring-core "0.2.5"]]
  :repositories [["maven-gae-plugin" "http://maven-gae-plugin.googlecode.com/svn/repository"]]
  :compile-path "war/WEB-INF/classes"
  :library-path "war/WEB-INF/lib")
