(comment "
 Data store models representing objects of interest with key/val pairs.
")
(ns rvar.model
  (:require [clojure.contrib.json :as json]
            [clojure.contrib.str-utils2 :as str2]
            [appengine-magic.services.datastore :as ds]
            [appengine-magic.services.memcache :as mc]))

; Entity definitions for items in the appengine datastore
(ds/defentity Phenotype [name ensembl snpedia])
(ds/defentity VariationScore [variation phenotype genescore refscore rank])
(ds/defentity VariationGroup [phenotype gid group score variations])
(ds/defentity VariationTranscript [transcript_stable_id gene_stable_id
                                   variation allele peptide_allele_string
                                   consequence_type])
(ds/defentity VariationProviders [variation providers])
(ds/defentity VariationLit [variation phenotype numrefs keywords])
(ds/defentity Gene [gene_stable_id name description])
(ds/defentity UserVariationGroup [user filename])
(ds/defentity UserVariation [user-group variation genotype])

; Allow retrieval by memcached to reduce datastore access
(defn- memcache-fn [f n & args]
  "Wrapper storing results in memcached based on arguments and namespace."
  (let [k (str2/join "," args)]
    (if (mc/contains? k :namespace n)
      (mc/get k :namespace n)
      (let [result (apply f args)]
        (mc/put! k result :namespace n)
        result))))

; High level access functions instead of direct datastore access

(defn get-phenotypes []
  "Retrieve top level phenotypes from the datastore."
  (memcache-fn 
    (fn []
      (sort
        (for [p-data (ds/query :kind Phenotype)]
          (:name p-data))))
    "get-phenotypes"))

(defn get-phenotype-vrn-groups [phn start n]
  "Retrieve variation groups associated with a phenotype."
  (memcache-fn
    (fn [phn start n]
      (->> (for [phn-grp (ds/query :kind VariationGroup :filter (= :phenotype phn)
                                   :sort [[:score :desc]])]
             phn-grp)
        (drop start)
        (take n)
        (map #(select-keys % [:group :variations :gid]))
        doall))
    "get-phenotype-vrn-groups" phn start n))

(defn get-vrn-transcripts [vrn]
  "Retrieve transcripts associated with a variation."
  (memcache-fn
    (fn [vrn]
      (doall
        (for [vrn-tx (ds/query :kind VariationTranscript :filter (= :variation vrn))]
          (select-keys vrn-tx [:gene_stable_id :variation :allele
                               :peptide_allele_string :consequence_type]))))
    "get-vrn-transcripts" vrn))

(defn get-vrn-providers [vrn]
  "Companies that provide genotyping of a variation."
  (memcache-fn
    (fn [vrn]
      (let [vrn-pro (first (ds/query :kind VariationProviders 
                                     :filter (= :variation vrn)))]
        (if-not (nil? vrn-pro)
          (:providers vrn-pro))))
    "get-vrn-providers" vrn))

(defn get-gene [gene-id]
  "Gene name and description via the ensembl stable gene id."
  (memcache-fn
    (fn [gene-id]
      (first
        (for [gene (ds/query :kind Gene :filter (= :gene_stable_id gene-id))]
          (select-keys gene [:name :description]))))
    "get-gene" gene-id))

(defn get-group-vrns [phn gid]
  "Retrieve variations associated with a phenotype and group."
  (memcache-fn
    (fn [phn gid]
      (let [group (first (ds/query :kind VariationGroup 
                                   :filter [(= :phenotype phn) (= :gid gid)]))]
        (:variations group)))
    "get-group-vrns" phn gid))

(defn get-variant-keywords [vrn]
  (memcache-fn
    (fn [vrn]
      (let [db-item (first (ds/query :kind VariationLit
                                     :filter (= :variation vrn)))]
        (json/read-json (.getValue (:keywords db-item)))))
    "get-variant-keywords" vrn))

(defn get-phenotype-vrns [phn]
  "Retrieve variation data associated with the phenotype."
  (for [phn-var (ds/query :kind VariationScore :filter (= :phenotype phn)
                          :sort [[:rank :desc]])]
      phn-var))

(defn get-variant-rank [vrn]
  "Retrieve the rank score for a variant"
  (let [db-item (first (ds/query :kind VariationScore 
                                 :filter (= :variation vrn)))]
    (if-not (nil? db-item)
      (:rank db-item)
      0.0)))

(defn- delete-user-variants [user fname]
  "Delete existing variants for a user and filename."
  (if-let [ugroup (first (ds/query :kind UserVariationGroup
                                   :filter [(= :user user) (= :filename fname)]))]
    (do
      (ds/delete! (ds/query :kind UserVariation
                            :filter (= :user-group ugroup)))
      (ds/delete! ugroup))))

(defn load-user-variants [user fname variants]
  "Load a lazy stream of variance information into the datastore."
  (delete-user-variants user fname)
  (let [ugroup (ds/save! (UserVariationGroup. user fname))]
    (ds/with-transaction
      (doseq [cur-var (take 1 variants)]
        (ds/save! (UserVariation. ugroup (:id cur-var) (:genotype cur-var)))))))

; Support for uploaded variations for a user. Needs to be reworked.
;
;(defn get-user [email]
;  "Get or create a database user with the given email address."
;  (let [user-query (select "user" where (= :email email))]
;    (if (empty? user-query)
;      (create-entity {:kind "user" :email email})
;      (first user-query))))
;
;(defn get-user-variations [email]
;  "Retrieve a lazy list of variation objects for the given user."
;  (let [user (get-user email)]
;    (flatten
;      (for [var-group (select "vargroup" where (= :parent (:key user)))]
;        (for [cur-var (select "variation" where (= :parent (:key var-group)))]
;          cur-var)))))
