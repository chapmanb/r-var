(comment "
  Create CSV files for SNP data mapping to genes and modifications.

  Usage:
      lein run scripts/ensembl_transcripts.clj <file of variations> <output directory>

  This creates the following output files for table import:
    * gene --> Details about a gene keyed by the unique ensemble identifier
    * variation_tx --> specify a gene name/transcript and modification caused
        by a variation. Includes variation identifier.
")

(use '[rvar.ensembl]
     '[clojure.java.io]
     '[clojure.contrib.str-utils :only (str-join)])
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

(defn- genes-and-print-variations [var-file]
  "Return associated genes, printing variation information as a side effect."
  (reduce (fn [genes cur-vrn]
            (doseq [[gene txs] (seq (variation-genes cur-vrn))]
              (doseq [tx txs]
                (println (str-join "," (line-items tx tx-items))))
              (assoc genes (:gene_stable_id gene) gene))
            genes)
    {} (line-seq (reader var-file))))

(defn transcript-csv [var-file out-dir]
  (let [gene-file (str-join "/" [out-dir "genes.csv"])
        v-tx-file (str-join "/" [out-dir "variation_tx.csv"])]
    (println var-file)
    (io/with-out-writer v-tx-file
      (println (str-join "," (header tx-items)))
      (let [genes (genes-and-print-variations var-file)]
        (io/with-out-writer gene-file
          (println (str-join "," (header gene-items)))
          (doseq [gene (vals genes)]
            (println (str-join "," (line-items gene gene-items)))))))))

(when *command-line-args*
  (apply transcript-csv *command-line-args*))
