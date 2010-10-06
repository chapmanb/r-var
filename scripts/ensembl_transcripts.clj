(comment "
  Create CSV files for SNP data mapping to genes and modifications.

  Usage:
      lein run scripts/ensembl_transcripts.clj <file of variations> <output directory>

  The variation file is a CSV file with a header where the first item is a SNP
  style variation name that will be looked up in Ensembl.

  This creates the following output files for table import:
    * gene --> Details about a gene keyed by the unique ensemble identifier
    * variation_tx --> specify a gene name/transcript and modification caused
        by a variation. Includes variation identifier.
 
  Upload:
      ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --kind VariationTranscript --filename=variation_tx.csv 
      ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --kind Gene --filename=genes.csv 
")

(use '[rvar.ensembl]
     '[clojure.java.io]
     '[clojure.contrib.str-utils :only (str-join re-split)])
(require '[clojure.contrib.duck-streams :as io])

(def gene-items [:gene_stable_id :name :description])

(def tx-items [:transcript_stable_id :gene_stable_id :variation :allele
               :peptide_allele_string :consequence_type])

(defn- header [items]
  "Retrieve list of items in the header of our variation phenotypes."
  (let [key-to-str #(str-join "" (rest (str %)))]
    (for [item items]
      (key-to-str item))))

(defn- line-items [cur-map items]
  (for [cur-key items]
    (cur-key cur-map)))

(defn- print-variations [gene-tx-map]
  "Print variation transcripts as a side effect."
  (doseq [txs (vals gene-tx-map)]
    (doseq [tx txs]
      (println (str-join "," (line-items tx tx-items))))))

(defn- genes-and-print-variations [vrns]
  "Return associated genes, printing variation information as a side effect."
  (lazy-seq
    (when-let [cur-vrn (first vrns)]
      (let [gene-txs (variation-genes cur-vrn)]
        (print-variations gene-txs)
        (flatten (cons (keys gene-txs) (genes-and-print-variations (rest vrns))))))))

(defn- file-variations [var-file]
  "Lazy sequence of unique variations in a file."
  ; The variation is the first item of a comma separated line
  (let [vrn-in-line (fn [cur-line]
                      (first (re-split #"," cur-line)))]
    (distinct
      ; Use rest to remove the first line header and iterate over remainder
      (for [line (rest (line-seq (reader var-file)))]
        (vrn-in-line line)))))

(defn- gene-map [var-file]
  "Retrieve map of unique genes in the listed variations."
  (reduce (fn [genes gene]
            (assoc genes (:gene_stable_id gene) gene))
    {} (genes-and-print-variations (file-variations var-file))))

(defn transcript-csv [var-file out-dir]
  (let [gene-file (str-join "/" [out-dir "genes.csv"])
        v-tx-file (str-join "/" [out-dir "variation_tx.csv"])]
    (io/with-out-writer v-tx-file
      (println (str-join "," (header tx-items)))
      (let [genes (gene-map var-file)]
        (io/with-out-writer gene-file
          (println (str-join "," (header gene-items)))
          (doseq [gene (vals genes)]
            (println (str-join "," (line-items gene gene-items)))))))))

(when *command-line-args*
  (apply transcript-csv *command-line-args*))
