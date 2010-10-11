(comment "
  REST style functionality for retrieving variant data.
")

(ns rvar.variant
  (:use [clojure.contrib.json :as json]
        [rvar.model]))

(defn- clean-db-items [maps]
  "Remove db-specific keys and parents from a map."
  (for [cur-map maps]
    (-> cur-map
      (dissoc :key)
      (dissoc :parent))))

(defn var-list [request]
  "Produce a JSON list of variations for the current user."
  (let [user (.getEmail ((request :appengine/user-info) :user))
        params (:params request)
        rows (Integer/parseInt (get params "rows"))
        start (* rows (- 1 (Integer/parseInt (get params "page"))))
        vars (clean-db-items (get-user-variations user))
        cur-vars (take rows (drop start vars))]
    (println cur-vars)
    (json/json-str {:total (count cur-vars) :page (get params "page")
                    :records (count cur-vars)
                    :rows cur-vars})))

(defn trait-var-list [request]
  "Provide a list of variants associated with a phenotypic trait."
  (let [phenotype (-> request (:query-params) (get "phenotype"))
        vrns-db (get-phenotype-vrns phenotype)
        vrns (distinct (remove nil? (map #(:variation %) vrns-db)))]
    (json/json-str {:variations vrns})))
