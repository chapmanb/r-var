#!/usr/bin/python
"""Parse SNPedia text information into structured diseases and descriptions.

Derived from the SNPedia parsing script in:

http://github.com/tomclegg/trait-o-matic

Requires a Zemanta API key (http://developer.zemanta.com/) which should be set
in your environmental variables as ZEMANTA_KEY.
"""
import re
import os
import sys
import time
import urllib, urllib2
import json
from xml.etree.ElementTree import parse
from optparse import OptionParser

categories = ["Has_genotype", "Is_a_snp", "Is_a_genoset"]
category_members_url = "http://snpedia.com/api.php?format=xml&action=query&list=categorymembers&cmtitle=Category:%s&cmlimit=500&cmcontinue=%s"
content_url = "http://snpedia.com/api.php?format=xml&action=query&prop=revisions&titles=%s&rvprop=content"

pmid_re = re.compile(r"PMID[ |=](\d+)")
infotable_re = re.compile(r"{{ ?[Rr]snum ?[\r\n]*([^}]*)[\r\n]*}}")

#association_re = re.compile(r"(associated|association) with \[\[([^\]]+)\]\]")
#rsid_re = re.compile(r"\| ?rsid ?= ?(\d+)")
#chromosome_re = re.compile(r"\| ?[Cc]hromosome ?= ?(.*)")
#position_re = re.compile(r"\| ?position ?= ?(\d+)")
#missense_re = re.compile(r"\| ?MissenseAllele ?= ?(\w+)")
summary_re = re.compile(r"\| ?Summary ?= ?([^|]*)")

genotypes_re = [
	re.compile(r"\| ?geno1 ?= ?\(([ACGT]+);([ACGT]+)\)"),
	re.compile(r"\| ?geno2 ?= ?\(([ACGT]+);([ACGT]+)\)"),
	re.compile(r"\| ?geno3 ?= ?\(([ACGT]+);([ACGT]+)\)")
]

def get_content(title):
    url = content_url % title
    api = parse(urllib.urlopen(url)).getroot()
    for element in api.findall("query/pages/page"):
        title = element.get("title")
        content = element.findtext("revisions/rev")
        return content

def title_list(category):
    continue_title = "|"
    while continue_title:
        url = category_members_url % (category, continue_title)
        api = parse(urllib.urlopen(url)).getroot()
        for element in api.findall("query/categorymembers/cm"):
            yield element.get("title").lower()
        element = api.find("query-continue/categorymembers")
        if element is not None:
            continue_title = element.get("cmcontinue")
        else:
            continue_title = None

def genotype_effects(var_id, content):
    effects = []
    for i, g_re in enumerate(genotypes_re):
        g_match = re.search(g_re, content)
        if g_match is None:
            continue
        gcontent = get_content("%s(%s;%s)" % (var_id, g_match.group(1),
            g_match.group(2)))
        if gcontent:
            table_end = gcontent.find("}}")
            # split the table into parts, generating a dictionary of genotype info
            genotype = {}
            for gpart in (p for p in gcontent[:table_end].split("\n") if p.find("=") > 0):
                key, val = gpart.split("=")
                genotype[key.replace("|", "").strip()] = val.strip()
            for torem in ["rsid"]:
                try:
                    del genotype[torem]
                except KeyError:
                    pass
            effects.append(genotype)
    return effects

def process_variant_content(var_id, content):
    # try to parse out PubMed sources
    try:
        pmids = ["pubmed/%s" % p for p in re.findall(pmid_re, content)]
    except AttributeError:
        pmids = []
    try:
        infotable = re.search(infotable_re, content).group(1)
    except AttributeError:
        infotable = ""
    try:
        summary = re.search(summary_re, infotable).group(1).strip()
    except AttributeError:
        summary = ""
    genotypes = genotype_effects(var_id, infotable)
    effect_text = "\n".join(g.get("summary", "") for g in genotypes)
    diseases, description = query_zemanta(var_id, content + effect_text)
    #diseases, description = [], ""
    description = description or summary
    out_info = dict(variation=var_id, diseases=diseases,
            genotypes=genotypes, refs=pmids, description=description)
    print json.dumps(out_info)
    #print var_id, diseases, genotypes, pmids, description

def query_zemanta(search_name, search_text):
    gateway = 'http://api.zemanta.com/services/rest/0.0/'
    args = {'method': 'zemanta.suggest',
            'api_key': os.environ["ZEMANTA_KEY"],
            'text': search_text,
            'return_categories': 'dmoz',
            'return_images': 0,
            'return_rdf_links' : 1,
            'format': 'json'}
    args_enc = urllib.urlencode(args)

    raw_output = urllib2.urlopen(gateway, args_enc).read()
    output = json.loads(raw_output)
    description = ""
    diseases = []
    for item, freebase_info in _get_freebase_info(output):
        # save any referenced diseases from the raw text
        for fb_type in freebase_info["type"]:
            if fb_type["id"] == "/medicine/disease":
                diseases.append(freebase_info["text"])
        # save high level description
        if item.lower() == search_name.lower():
            description = freebase_info["description"].replace("\n", " ")
    return diseases, description

def _get_freebase_info(output):
    """Retrieve information from Freebase linked with Zemanta results.
    """
    freebase_main = "http://rdf.freebase.com/ns"
    freebase_json = "http://www.freebase.com/experimental/topic/standard"
    for link in output['markup']['links']:
        for target in link['target']:
            if target['type'] in ['rdf'] and target['url'].startswith(freebase_main):
                cur_url = target['url'].replace(freebase_main, freebase_json)
                #print target['title'], cur_url
                freebase_info = json.loads(urllib2.urlopen(cur_url).read())["result"]
                yield target['title'], freebase_info

def main(in_snps=None, snp_start=None):
    if in_snps:
        snps = in_snps.split(",")
    else:
        snps = title_list(categories[0])
    delay_start = snp_start is not None
    for snp in snps:
        if delay_start:
            if snp == snp_start:
                delay_start = False
        else:
            content = get_content(snp)
            info = process_variant_content(snp, content.encode('utf-8'))
            time.sleep(2)

if __name__ == "__main__":
    parser = OptionParser()
    parser.add_option("-s", "--start", dest="snp_start")
    options, args = parser.parse_args()
    kwargs = dict(snp_start=options.snp_start)
    main(*args, **kwargs)
