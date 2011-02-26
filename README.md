=== Interactive development

* Startup:

    % rm -rf classes
    % lein repl
    (require '[appengine-magic.core :as ae])
    (require '[rvar.core :as rvar] :reload-all)
    (ae/serve rvar/rvar-app)

* Whenever you edit, do:

    (require '[rvar.core :as rvar] :reload-all)

* Memcache clear:

    (require '[appengine-magic.services.memcache :as mc])
    (mc/clear-all!)

=== Local server

    % lein appengine-prepare
    % ~/install/gae/appengine-java-sdk-1.4.0/bin/dev_appserver.sh resources

=== Data file generation

* Hand edit scripts/data/phenotypes.csv specifying health traits of interest and
  references at Ensembl and SNPedia.
* `lein run scripts/ensembl_phenotypes.clj scripts/data`
* `python2.6 scripts/snpedia_to_disease.py scripts/data`
* `lein run scripts/ensembl_transcripts.clj scripts/data`
* `python2.6 scripts/merge_phenotype_data.py scripts/data`
* `python2.6 scripts/publication_summary.py scripts/data`
* `lein run scripts/pub_refine_keywords.clj scripts/data`
* `lein run scripts/rate_variants.clj scripts/data`
* `lein run scripts/provider_variations.clj ../genomes/provider_details.csv scripts/data`
* `lein run scripts/group_variations.clj scripts/data`

=== Upload

* `python2.6 scripts/upload_data_to_gae.py scripts/data localhost:8080 test@example.com ""`

* `~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --filename=data/phenotypes.csv --url=http://localhost:8080/remote_api --application=our-var --kind Phenotype`
* `~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --kind Gene --filename=data/genes.csv`
* `~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --kind VariationTranscript --filename=data/tx-variation.csv`
* `~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --filename=data/variation-lit.csv --kind VariationLit`
* `~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --filename=data/variation-providers.csv --kind VariationProviders`
* `~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --filename=data/variation-scores.csv --kind VariationScore`
* `~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --url=http://localhost:8080/remote_api --application=our-var --filename=data/variation-groups.csv --kind VariationGroup`



* ` ~/install/gae/google_appengine/appcfg.py upload_data --config_file=bulkloader.yaml --filename=data/variation-phenotypes.csv --url=http://localhost:8080/remote_api --application=our-var --kind VariationPhenotype`
