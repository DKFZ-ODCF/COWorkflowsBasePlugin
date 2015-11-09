#!/usr/bin/env perl

# this script replaces emtpy fields in a tab-separated file with '.' (only the dot, not the quotes...)
# the file to be processed can either be piped into it, or given as command line arg
# output is written to stdout
use strict;
use warnings;

while (<>) {
  s/(^|\t)(?=\t|\n|$)/${1}./g;
  print;
}
