#!/usr/bin/env perl
#
# Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
#
# Small filter which reads a sorted vcf file from stdin, merges duplicate lines (lines where chrom and pos are identical),
# and prints duplicate-free file to stdout it detects which column(s) differ in the duplicates and joins the entries with an '&'.
use strict;
use warnings;
use v5.10;

use constant DEBUG => 0; # set to 1 and it will only print out duplicates

my @fields;
my @pfields;
my $i;

$_ = <>;
chomp;
@pfields = split(/\t/);

while (<>) {
  chomp;
  @fields = split(/\t/);
  if ($fields[0] eq $pfields[0] && $fields[1] eq $pfields[1]) { # duplicate line
    for ($i=0; $i<@fields; $i++) { # join deviating fields from new line to corresponding field from old line
      $pfields[$i] .= '&'.$fields[$i] if ("$fields[$i]" ne "$pfields[$i]");
    }
    DEBUG && say "@pfields";
    next;
  }
  DEBUG || say join "\t", @pfields;
  @pfields = @fields;
}
say join "\t", @pfields;
