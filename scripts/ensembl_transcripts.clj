(comment "
  Create CSV files for SNP data mapping to genes and modifications.

  Usage:
      lein run scripts/ensembl_transcripts.clj <data directory>

  The variation file is a CSV file with a header where the first item is a SNP
  style variation name that will be looked up in Ensembl.

  This creates the following output files for table import:
    * gene --> Details about a gene keyed by the unique ensemble identifier
    * variation_tx --> specify a gene name/transcript and modification caused
        by a variation. Includes variation identifier.
 
  Upload:
      ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --kind VariationTranscript --filename=data/tx-variation.csv 
      ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --kind Gene --filename=data/genes.csv 
")

(use '[rvar.ensembl]
     '[clojure.java.io])
(require '[clojure.contrib.duck-streams :as io]
         '[clojure.contrib.str-utils2 :as str2])

(def gene-items [:gene_stable_id :name :description])

(def tx-items [:transcript_stable_id :gene_stable_id :variation :allele
               :peptide_allele_string :consequence_type])

(defn- header [items]
  "Retrieve list of items in the header of our variation phenotypes."
  (let [key-to-str #(str2/join "" (rest (str %)))]
    (for [item items]
      (key-to-str item))))

(defn- line-items [cur-map items]
  (for [cur-key items]
    (cur-key cur-map)))

(defn- print-variations [gene-tx-map]
  "Print variation transcripts as a side effect."
  (doseq [txs (vals gene-tx-map)]
    (doseq [tx txs]
      (println (str2/join "," (line-items tx tx-items))))))

(defn- genes-and-print-variations [vrns]
  "Return associated genes, printing variation information as a side effect."
  (lazy-seq
    (when-let [cur-vrn (first vrns)]
      (let [gene-txs (variation-genes cur-vrn)]
        (print-variations gene-txs)
        (flatten (cons (keys gene-txs) (genes-and-print-variations (rest vrns))))))))

(defn- file-variations [var-base]
  "Lazy sequence of unique variations in a CSV variation files."
  ; The variation is the first item of a comma separated line
  (let [vrn-in-line (fn [cur-line]
                      (first (str2/split cur-line #",")))
        exts ["ensembl.csv" "snpedia.csv"]]
    (distinct (flatten
      (for [ext exts]
        (let [var-file (str2/join "" [var-base ext])]
          ; Use rest to remove the first line header and iterate over remainder
          (for [line (rest (line-seq (reader var-file)))]
            (vrn-in-line line))))))))

(defn- gene-map [var-base]
  "Retrieve map of unique genes in the listed variations."
  (reduce (fn [genes gene]
            (assoc genes (:gene_stable_id gene) gene))
    {} (genes-and-print-variations (file-variations var-base))))

(defn- file-phenotypes [p-file]
  "Retrieve caegories and specific phenotypes of interest from CSV file."
  (let [line-info (fn [cur-line]
                    (let [parts (str2/split cur-line #",")
                          phenotype (first parts)
                          ensembl (str2/split (second parts) #";")]
                      [phenotype ensembl]))]
    ; Use rest to remove the first line header and iterate over remainder
    (for [line (rest (line-seq (reader p-file)))]
      (line-info line))))

(defn transcript-csv [data-dir]
  (let [in-file (file data-dir "phenotypes.csv")]
    (doseq [[phenotype _] (file-phenotypes in-file)]
      (let [phen-dir (file data-dir (str2/replace phenotype #"[' ]" "_"))
            gene-file (file phen-dir "genes.csv")
            v-tx-file (file phen-dir "tx-variation.csv")
            var-base (file phen-dir "variation-")]
        (io/with-out-writer v-tx-file
          (println (str2/join "," (header tx-items)))
          (let [genes (gene-map var-base)]
            (io/with-out-writer gene-file
              (println (str2/join "," (header gene-items)))
              (doseq [gene (vals genes)]
                (println (str2/join "," (line-items gene gene-items)))))))))))

(when *command-line-args*
  (apply transcript-csv *command-line-args*))
