#!/usr/bin/env perl

use strict;
use warnings;
use v5.10;

my $pad = shift;
my (@fields, $chr, $start, $end);
while (<>) {
  next if (/^\#/);
  @fields = split(/\t/);
  $chr = $fields[0];
  $start = ($fields[1]-$pad-1 > 0) ? $fields[1]-$pad-1 : 0;
  $end = $fields[1] + length($fields[3]) + $pad -1;# TODO: prevent exceeding contig border
  say join "\t", $chr, $start, $end;
}
