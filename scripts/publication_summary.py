#!/usr/bin/env python
"""Summarize publication information for a set of variations.

Attempt to provide an at-a-glance view of what the literature record says about
a SNP, emphasizing relationships to phenotypes of interest.

Usage:
    publication_summary.py <variation phenotype file>
"""
import sys
import os
import csv
import urllib, urllib2
import json
import pprint
import collections

from Bio import Entrez
Entrez.email = "nospam@50mail.com"

def main(vrn_file, start_info=None):
    if start_info:
        start_info = tuple(start_info.split(","))
    out_file = os.path.join(os.path.dirname(vrn_file), "variation-lit.csv")
    with open(out_file, "w" if not start_info else "a") as out_handle:
        writer = csv.writer(out_handle)
        if not start_info:
            writer.writerow(["variation", "phenotype", "numrefs", "keywords"])
        ready = not start_info
        for vrn, phenotype, ids in vrn_phenotype_ids(vrn_file):
            if ready:
                vrn_kwds = collections.defaultdict(int)
                for pubmed_id in ids:
                    handle = Entrez.efetch(db="pubmed", id=pubmed_id, retmode="xml")
                    recs = Entrez.read(handle)
                    article = recs[0]["MedlineCitation"]["Article"]
                    text = " ".join([article["ArticleTitle"],
                                     article["Abstract"]["AbstractText"]])
                    for kwd in zemanta_keywords(text):
                        vrn_kwds[kwd] += 1
                print vrn, phenotype, len(ids), vrn_kwds
                writer.writerow([vrn, phenotype, len(ids), json.dumps(vrn_kwds)])
            elif (vrn, phenotype) == start_info:
                ready = True

def zemanta_keywords(text):
    gateway = 'http://api.zemanta.com/services/rest/0.0/'
    args = {'method': 'zemanta.suggest',
            'api_key': os.environ["ZEMANTA_KEY"],
            'text': text.encode('utf-8'),
            'return_categories': 'dmoz',
            'articles_limit': 0,
            'return_images': 0,
            'markup_limit': 25,
            'return_rdf_links' : 1,
            'format': 'json'}
    args_enc = urllib.urlencode(args)

    raw_output = urllib2.urlopen(gateway, args_enc).read()
    output = json.loads(raw_output)
    keywords = []
    for link in output["markup"]["links"]:
        keywords.append(link["target"][0]["title"])
    return keywords

def vrn_phenotype_ids(in_file):
    order = []
    ids = dict()
    with open(in_file) as in_handle:
        reader = csv.reader(in_handle)
        reader.next() # header
        for vrn, phenotype, pubmed_ids in (p[:3] for p in reader):
            pubmed_ids = [i.split("pubmed/")[-1] for i in pubmed_ids.split(";")
                          if i.startswith("pubmed/")]
            key = (vrn, phenotype)
            try:
                ids[key].extend(pubmed_ids)
            except KeyError:
                ids[key] = pubmed_ids
                order.append(key)
    for key in order:
        yield key[0], key[1], sorted(list(set(ids[key])))

if __name__ == "__main__":
    main(*sys.argv[1:])
