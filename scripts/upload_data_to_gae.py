#!/usr/bin/env python
"""Upload all prepared CSV data to an app engine instance.

Usage:
    upload_data_to_gae.py <data directory> <server base URL> <username> <password>
"""
import os
import sys
import subprocess
import operator
import glob

def main(data_dir, base_url, user, passwd):
    work_dir = os.getcwd()
    appcfg_path = os.path.join(os.environ["HOME"],
                   "install/gae/google_appengine/appcfg.py")
    config_file = os.path.join(os.path.dirname(__file__), "bulkloader.yaml")
    full_url = "http://%s/remote_api" % base_url
    app_name = "our-var"
    uploads = [("Phenotype", "phenotypes.csv"),
               ("Gene", "genes.csv"),
               ("VariationTranscript", "tx-variation.csv"),
               ("VariationLit", "variation-lit.csv"),
               ("VariationProviders", "variation-providers.csv"),
               ("VariationScore", "variation-scores.csv"),
               ("VariationGroup", "variation-groups.csv")]
    for kind, fname in uploads:
        fname = os.path.join(data_dir, fname)
        upload_file(appcfg_path, config_file, full_url, app_name,
                kind, fname, user, passwd)
        cleanup(work_dir)

def cleanup(work_dir):
    for fname in reduce(operator.add,
            [glob.glob(os.path.join(work_dir, "%s*" % b)) for b in
             "bulkloader-log-", "bulkloader-progress-"]):
        os.remove(fname)

def upload_file(appcfg_path, config_file, full_url, app_name,
                kind, fname, user, passwd):
    cl = [appcfg_path, "upload_data", "--config_file=%s" % config_file,
          "--application=%s" % app_name, "--url=%s" % full_url,
          "--filename=%s" % fname, "--kind=%s" % kind,
          "--email=%s" % user, "--passin"]
    print " ".join(cl)
    proc = subprocess.Popen(cl, stdin=subprocess.PIPE)
    proc.communicate("%s\n" % passwd)

if __name__ == "__main__":
    main(*sys.argv[1:])
