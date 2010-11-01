(comment "
  Generate HTML link outs for external data references.
")

(ns rvar.external
  (:use [hiccup.core]))

(defn vrn-links [vrn]
  "External links for variations."
  [[:a {:href (format "http://snpedia.com/index.php/%s" vrn) :target "_blank"}
        "SNPedia"]])
