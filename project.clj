(defproject r-var "0.0.1-SNAPSHOT"
  :description "Share and explore our genomic variability."
  :aot [rvar.servlet]
  :keep-non-project-classes true
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.4.1"]
                 [hiccup "0.2.6"]
                 [scriptjure "0.1.17"]
                 [gaka "0.2.0"]
                 [ring/ring-core "0.2.5"]
                 [ring/ring-jetty-adapter "0.2.5"]]
  :dev-dependencies [[appengine-magic "0.4.0-SNAPSHOT"]
                     [vimclojure/server "2.2.0"]
                     [mysql/mysql-connector-java "5.1.6"]
                     [clojure-csv/clojure-csv "1.2.0"]]
  :jvm-opts ["-Xmx1512M"])
