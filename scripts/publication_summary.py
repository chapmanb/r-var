#!/usr/bin/env python
"""Summarize publication information for a set of variations.

Attempt to provide an at-a-glance view of what the literature record says about
a SNP, emphasizing relationships to phenotypes of interest.

Usage:
    publication_summary.py <data directory>
"""
import sys
import os
import csv
import urllib, urllib2
import json
import time
import pprint
import collections

from Bio import Entrez
Entrez.email = "nospam@50mail.com"

def main(data_dir, start_info=None):
    if start_info:
        start_info = tuple(start_info.split(","))
    vrn_file = os.path.join(data_dir, "variation-phenotypes.csv")
    out_file = os.path.join(data_dir, "lit-kwds.csv")
    with open(out_file, "w" if not start_info else "a") as out_handle:
        writer = csv.writer(out_handle)
        seen_pubmedids = set()
        if not start_info:
            writer.writerow(["pubmed_id", "keywords"])
        ready = not start_info
        retriever = KeywordRetriever()
        for vrn, phenotype, ids in vrn_phenotype_ids(vrn_file):
            if ready:
                vrn_kwds = collections.defaultdict(int)
                for pubmed_id in ids:
                    kwds = retriever.get_kwds(pubmed_id)
                    for kwd in kwds:
                        vrn_kwds[kwd] += 1
                    if pubmed_id not in seen_pubmedids:
                        writer.writerow([pubmed_id, json.dumps(vrn_kwds)])
                print vrn, phenotype, len(ids), dict(vrn_kwds)
            elif (vrn, phenotype) == start_info:
                ready = True
            for pid in ids:
                seen_pubmedids.add(pid)

class KeywordRetriever:
    """Retrieve keywords for publications based on extraction of abstract text.
    """
    def __init__(self):
        self._cache = {}
        self._fb_cache = {}
        self._want_topics = set(["medicine", "biology", "measurement_unit",
                "education", "chemistry", "award", "physics"])
        self._avoid_topics = set(["location", "organization", "business", "book",
                "freebase", "internet", "aviation", "computer", "people",
                "fictional_universe", "law", "geography", "religion",
                "architecture", "language", "sports", "baseball",
                "metropolitan_transit", "media_common", "film", "event",
                "transportation", "food", "finance", "spaceflight"])

    def get_kwds(self, pubmed_id):
        try:
            return self._cache[pubmed_id]
        except KeyError:
            results = self._fetch_kwds(pubmed_id)
            self._cache[pubmed_id] = results
            return results

    def _fetch_kwds(self, pubmed_id):
        while 1:
            try:
                handle = Entrez.efetch(db="pubmed", id=pubmed_id, retmode="xml")
                recs = Entrez.read(handle)
                break
            except urllib2.URLError:
                print "Retrying url retrieval"
                time.sleep(5)
        article = recs[0]["MedlineCitation"]["Article"]
        text = " ".join([article["ArticleTitle"],
                         article["Abstract"]["AbstractText"]])
        return self._zemanta_keywords(text)

    def _zemanta_keywords(self, text):
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

        output = json.loads(self._safe_urlopen(gateway, args_enc))
        keywords = []
        for link in output["markup"]["links"]:
            pass_kwd = True
            fb_info = self._get_freebase_info(link)
            if fb_info and len(fb_info.get("type")) > 0:
                fb_ids = [t["id"].split("/")[1] for t in fb_info["type"]]
                if len([i for i in fb_ids if i in self._want_topics]) > 0:
                    pass_kwd = True
                elif len([i for i in fb_ids if i in self._avoid_topics]) > 0:
                    pass_kwd = False
                else:
                    print fb_info
                    print fb_info["type"]
                    print
            if pass_kwd:
                keywords.append(link["target"][0]["title"])
        return keywords

    def _get_freebase_info(self, link):
        """Retrieve information from Freebase linked with Zemanta results.
        """
        freebase_main = "http://rdf.freebase.com/ns"
        freebase_json = "http://www.freebase.com/experimental/topic/standard"
        cur_url = None
        for target in link['target']:
            if target['type'] in ['rdf'] and target['url'].startswith(freebase_main):
                cur_url = target['url'].replace(freebase_main, freebase_json)
        if cur_url:
            try:
                return self._fb_cache[cur_url]
            except KeyError:
                freebase_info = json.loads(self._safe_urlopen(cur_url)).get(
                        "result", None)
                self._fb_cache[cur_url] = freebase_info
                return freebase_info

    def _safe_urlopen(self, url, data=None):
        while 1:
            try:
                result = urllib2.urlopen(url, data).read()
                return result
            except urllib2.URLError:
                print "Retrying url retrieval"
                time.sleep(5)

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
