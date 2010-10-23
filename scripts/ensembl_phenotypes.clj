(comment "
  Create a CSV file of phenotype data from ensembl, ready for GAE upload.


  1. ~/install/gae/google_appengine/appcfg.py create_bulkloader_config 
     --filename=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var

  2. Edit the bulkloader.yaml to match our output files.

  3. lein run scripts/ensembl_phenotypes.clj phenotypes.csv script

  4. ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --filename=data/variation-phenotypes.csv --url=http://localhost:8080/remote_api --application=our-var --kind VariationPhenotype
     ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --filename=data/phenotypes.csv --url=http://localhost:8080/remote_api --application=our-var --kind Phenotype
")

(use '[rvar.ensembl]
     '[clojure.java.io]
     '[clojure.contrib.duck-streams :only (with-out-writer)])
(require '[clojure.contrib.str-utils2 :as str2])

(def csv-items [:variation :phenotype :study :study_type :associated_gene
                :associated_variant_risk_allele :risk_allele_freq_in_controls
                :p_value])

(defn phenotype-header []
  "Retrieve list of items in the header of our variation phenotypes."
  (let [key-to-str #(str2/join "" (rest (str %)))]
    (for [item csv-items]
      (key-to-str item))))

(defn phenotype-out [vrn]
  "Retrieve output details in the current variation."
  (for [item csv-items]
    (let [str-item (get vrn item "")]
      (if (contains? {:associated_gene ""} item)
        (str2/join ";" str-item)
        str-item))))

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

(defn phenotypes-to-csv [in-file out-dir]
  "Write out our defined phenotypes of interest to CSV for uploading."
  (doseq [[phenotype ensembl] (file-phenotypes in-file)]
    (let [p-file (str2/join "/" [out-dir (str2/replace phenotype " " "_")
                                 "variation-ensembl.csv"])]
      (make-parents p-file)
      (with-out-writer p-file
        (println (str2/join "," (phenotype-header)))
        (doseq [var (apply phenotype-variations (cons phenotype ensembl))]
          (println (str2/join "," (phenotype-out var))))))))

(when *command-line-args*
  (apply phenotypes-to-csv *command-line-args*))

; select * from variation_annotation WHERE phenotype_id = 9;
; +-------------------------+--------------+--------------+-----------+-----------------+------------+-----------------+-----------------+--------------------------------+-----------------+------------------------------+-----------+
; | variation_annotation_id | variation_id | phenotype_id | source_id | study           | study_type | local_stable_id | associated_gene | associated_variant_risk_allele | variation_names | risk_allele_freq_in_controls | p_value   |
; +-------------------------+--------------+--------------+-----------+-----------------+------------+-----------------+-----------------+--------------------------------+-----------------+------------------------------+-----------+
;|                   58299 |       938985 |            9 |         5 | pubmed/19525955 | GWAS       | NULL            | CD58            | rs1335532-A                    | rs1335532       | 0.87                         | 1E-7      |
                ;

; tables to select
;
; Variation information:
; select * from variation WHERE name = 'rs3094315';
; +--------------+-----------+-----------+-----------------------------------+------------------+
; | variation_id | source_id | name      | validation_status                 | ancestral_allele |
; +--------------+-----------+-----------+-----------------------------------+------------------+
; |      2467246 |         1 | rs3094315 | cluster,freq,doublehit,1000Genome | G                |
; +--------------+-----------+-----------+-----------------------------------+------------------+
; select * from variation_feature WHERE variation_id=2467246;
; +----------------------+---------------+------------------+----------------+-------------------+--------------+---------------+----------------+------------+-----------+-----------+-----------------------------------+------------------------+
; | variation_feature_id | seq_region_id | seq_region_start | seq_region_end | seq_region_strand | variation_id | allele_string | variation_name | map_weight | flags     | source_id | validation_status                 | consequence_type       |
; +----------------------+---------------+------------------+----------------+-------------------+--------------+---------------+----------------+------------+-----------+-----------+-----------------------------------+------------------------+
; |             11854140 |         27511 |           752566 |         752566 |                 1 |      2467246 | G/A           | rs3094315      |          1 | genotyped |         1 | cluster,freq,doublehit,1000Genome | WITHIN_NON_CODING_GENE |
; +----------------------+---------------+------------------+----------------+-------------------+--------------+---------------+----------------+------------+-----------+-----------+-----------------------------------+------------------------+
; select * from transcript_variation WHERE variation_feature_id = 11854140;
; +-------------------------+----------------------+----------------------+------------+----------+-----------+---------+-------------------+-----------------+-----------------------+------------------------+
; | transcript_variation_id | transcript_stable_id | variation_feature_id | cdna_start | cdna_end | cds_start | cds_end | translation_start | translation_end | peptide_allele_string | consequence_type       |
; +-------------------------+----------------------+----------------------+------------+----------+-----------+---------+-------------------+-----------------+-----------------------+------------------------+
; |                75192010 | ENST00000435300      |             11854140 |       NULL |     NULL |      NULL |    NULL |              NULL |            NULL | NULL                  | WITHIN_NON_CODING_GENE |
; |                75192222 | ENST00000326734      |             11854140 |       NULL |     NULL |      NULL |    NULL |              NULL |            NULL | NULL                  | UPSTREAM               |
; +-------------------------+----------------------+----------------------+------------+----------+-----------+---------+-------------------+-----------------+-----------------------+------------------------+
; select * from variation_annotation WHERE variation_id = 2467246;
;
; --> Population allele frequency
;
;  select * from allele WHERE variation_id = 2467246;
; +-----------+--------------+-----------+--------+-----------+-----------+
; | allele_id | variation_id | subsnp_id | allele | frequency | sample_id |
; +-----------+--------------+-----------+--------+-----------+-----------+
; |   9343799 |      2467246 |   9839052 | A      |     0.845 |       679 |
; |   9343801 |      2467246 |   9839052 | A      |       0.9 |       680 |
; |   9343803 |      2467246 |   9839052 | A      |     0.852 |       681 |
;
; > select * from population_genotype WHERE variation_id = 2467246;
; +------------------------+--------------+-----------+----------+----------+-----------+-----------+
; | population_genotype_id | variation_id | subsnp_id | allele_1 | allele_2 | frequency | sample_id |
; +------------------------+--------------+-----------+----------+----------+-----------+-----------+
; |                 607422 |      2467246 |   9839052 | A        | A        |     0.136 |       682 |
; |                 607417 |      2467246 |   9839052 | A        | G        |       0.2 |       680 |
; |                 607419 |      2467246 |   9839052 | A        | G        |     0.295 |       681 |
; |                 607415 |      2467246 |   9839052 | A        | G        |      0.31 |       679 |
; |                 607421 |      2467246 |   9839052 | A        | G        |     0.373 |       682 |
;
; --> Individual alleles
;
; select * from individual_population WHERE population_sample_id = 679;
; +----------------------+----------------------+
; | individual_sample_id | population_sample_id |
; +----------------------+----------------------+
; |                11901 |                  679 |
; |                11902 |                  679 |
; |                11904 |                  679 |
;
;
;
; --> Exploring by phenotype
; select * from phenotype WHERE name like '%MS%';
; +--------------+------+--------------------+
; | phenotype_id | name | description        |
; +--------------+------+--------------------+
; |            9 | MS   | Multiple Sclerosis |
; +--------------+------+--------------------+
; select * from variation_annotation WHERE phenotype_id = 9;
; +-------------------------+--------------+--------------+-----------+-----------------+------------+-----------------+-----------------+--------------------------------+-----------------+------------------------------+-----------+
; | variation_annotation_id | variation_id | phenotype_id | source_id | study           | study_type | local_stable_id | associated_gene | associated_variant_risk_allele | variation_names | risk_allele_freq_in_controls | p_value   |
; +-------------------------+--------------+--------------+-----------+-----------------+------------+-----------------+-----------------+--------------------------------+-----------------+------------------------------+-----------+
; |                      51 |      1570308 |            9 |         6 | NULL            | GWAS       | EGAS00000000022 | NULL            | NULL                           | rs2107732       | NULL                         | NULL      |
; |                      52 |      3391685 |            9 |         6 | NULL            | GWAS       | EGAS00000000022 | NULL            | NULL                           | rs4986790       | NULL                         | NULL      |
; |                      53 |      4618456 |            9 |         6 | NULL            | GWAS       | EGAS00000000022 | NULL            | NULL                           | rs7313899       | NULL                         | NULL      |
;
; > select * from phenotype WHERE description like '%mmune%';
; +--------------+------+----------------------------+
; | phenotype_id | name | description                |
; +--------------+------+----------------------------+
; |            8 | AITD | Autoimmune Thyroid Disease |
; +--------------+------+----------------------------+
