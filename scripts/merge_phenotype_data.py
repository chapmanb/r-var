#!/usr/bin/env python
"""Combine variant data from multiple phenotypes into ready to upload CSVs.

Usage:
    merge_phenotype_data.py <data_dir> [<list of phenotypes>]
"""
import sys
import os
import csv
import glob

def main(data_dir, phenotypes=None):
    if phenotypes is None:
        phenotypes = [f for f in os.listdir(data_dir) if
                      os.path.isdir(os.path.join(data_dir, f))]
    else:
        phenotypes = phenotypes.split(",")
    combine_files("genes.csv", phenotypes, data_dir, _gene_unique)
    combine_files("tx-variation.csv", phenotypes, data_dir, _tx_unique)
    combine_files("variation-phenotypes.csv", phenotypes, data_dir,
            _phenotype_unique, _phenotype_merge, "variation-*.csv")

# -- determine the unique key for various types of data

def _gene_unique(parts):
    if parts[0]:
        return parts[0]

def _tx_unique(parts):
    if parts[0] and parts[2]:
        return (parts[0], parts[2])

def _phenotype_unique(parts):
    return tuple(parts)
    #return (parts[0], parts[1])

# -- ability to merge together data with shared keys and overlapping info

def _phenotype_merge(orig_parts, new_parts):
    assert orig_parts == new_parts, (orig_parts, new_parts)
    return orig_parts

def combine_files(out_file, phenotypes, data_dir, unique_fn, merge_fn=None,
        in_file_glob=None):
    """Combine multiple phenotype files into a single CSV.
    """
    if in_file_glob is None:
        in_file_glob = out_file
    with open(os.path.join(data_dir, out_file), "w") as out_handle:
        writer = csv.writer(out_handle)
        header, info = _get_unique_values(data_dir, phenotypes, unique_fn,
                merge_fn, in_file_glob)
        writer.writerow(header)
        for line in info:
            writer.writerow(line)

def _get_unique_values(data_dir, phenotypes, unique_fn, merge_fn, in_file_glob):
    """Read through phenotype directories, collecting unique lines to write.
    """
    seen_order = []
    info = dict()
    for phenotype in phenotypes:
        in_files = glob.glob(os.path.join(data_dir, phenotype, in_file_glob))
        assert len(in_files) > 0
        for in_file in in_files:
            with open(in_file) as in_handle:
                reader = csv.reader(in_handle)
                header = reader.next()
                for parts in reader:
                    unique_bit = unique_fn(parts)
                    if unique_bit:
                        if info.has_key(unique_bit):
                            if merge_fn is not None:
                                new_data = merge_fn(info[unique_bit], parts)
                        else:
                            info[unique_bit] = parts
                            seen_order.append(unique_bit)
    return header, [info[k] for k in seen_order]

if __name__ == "__main__":
    main(*sys.argv[1:])
