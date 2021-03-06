(defproject r-var "0.0.1-SNAPSHOT"
  :description "Share and explore our genomic variability."
  :aot [rvar.servlet]
  :keep-non-project-classes true
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.5.3"]
                 [hiccup "0.3.1"]
                 [gaka "0.2.0"]
                 [ring/ring-core "0.3.5"]
                 [ring/ring-jetty-adapter "0.3.5"]]
  :dev-dependencies [[appengine-magic "0.4.0-SNAPSHOT"]
                     [swank-clojure "1.2.1"]
                     [mysql/mysql-connector-java "5.1.6"]
                     [enlive "1.0.0"]
                     [biomart-client "1.0.0-SNAPSHOT"]
                     [clojure-csv/clojure-csv "1.3.2"]
                     [midje "1.1.1"]]
  :jvm-opts ["-Xmx1512M"]
  :run-aliases {:hla-get rvar.hla
                :lit-ms rvar.literature.ms})
