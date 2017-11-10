#!/usr/bin/env perl

##
# Copyright (c) 2017 eilslabs.
#
# Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
##


# This script takes an snv file and a number of annovar result files as input.
# Annofiles should be passed as ANNOCOLUMN=ANNOFILE(column[1-based]).
# It adds for each annovar file one column (header ANNOCOLUMN) to the snv file.
# These colunms get its input from the column of the annofile which was specified in the argument.
# From each file only one line is read at a time, so it is safe to use a large number of annofiles.
# All files must be sorted lexicographically
# The SNV file is expected to contain chromosomes as 'chr[\dxy]{1,2}'; the annofiles as '[\dxy]{1,2}'
### TODO Not chr-safe, chr-order dependent, cannot update columns
use strict;
use warnings;
use v5.10;

my $snvfile = shift;
open(S, $snvfile) || die "Could not open SNV file $snvfile\n";

my %anno;
my %annofile;
my $column;
my %annofh;
my %annoline;
my %colnr;
my @annocolumns; # need also array to preserve caller-defined input order
my %chrhash = (                    # this value + coordinate give unique genome-wide coordinate for easy comparison
               10 => 10000000000,
               11 => 20000000000,
               12 => 30000000000,
               13 => 40000000000,
               14 => 50000000000,
               15 => 60000000000,
               16 => 70000000000,
               17 => 80000000000,
               18 => 90000000000,
               19 => 100000000000,
                1 => 110000000000,
               20 => 120000000000,
               21 => 130000000000,
               22 => 140000000000,
                2 => 150000000000,
                3 => 160000000000,
                4 => 170000000000,
                5 => 180000000000,
                6 => 190000000000,
                7 => 200000000000,
                8 => 210000000000,
                9 => 220000000000,
                X => 230000000000,
                Y => 240000000000,
                M => 250000000000
              );
    
my ($file, $colname, $colnr, $fh);
# annofiles should be passed as ANNOCOLUMN=ANNOFILE(column[1-based])
# currently only annotation from one column is supported (but this is easy to change)
foreach my $arg (@ARGV) {
  $arg =~ /([^=]+)=([^\[]+)\[(\d+)/ or die "Arg $arg does not match";
  ($colname, $file, $colnr) = ($1, $2, $3);
  $colnr{$colname} = $colnr-1; # bring to 0-based system
  $annofile{$colname} = $file;
  push(@annocolumns, $colname);
}

foreach $column (@annocolumns) {
  open ($annofh{$column}, "<", $annofile{$column}) or die "Could not open annotation file $annofile{$column}";
}

my $header = <S>;
chomp $header;
say $header, "\t", join "\t", @annocolumns;

my $sline;
my %acoord;
$acoord{$_} = -1 foreach (@annocolumns);

my $scoord;
SNVFILE_LOOP: while ($sline=<S>) {
  chomp($sline);
  $sline  =~ /^chr(\w+)\t(\d+)/  or die "SNV line did not match for coordinate extraction";
  $scoord = $chrhash{$1}+$2;
  ANNOCOLUMNS_LOOP: foreach $column (@annocolumns) {
      if ($scoord < $acoord{$column}) {
        $anno{$column} = '.';
        next;
      }
      if ($scoord == $acoord{$column}) {
        $anno{$column} = (split(/\t/,$annoline{$column}))[$colnr{$colname}];
        next;
      }
      $fh = $annofh{$column}; # the diamond operator does not like filehandles in hashes...
      while ($annoline{$column}=<$fh>) {
        chomp($annoline{$column});
        $annoline{$column} =~ /^[^\t]+\t[^\t]+\t(\w+)\t(\d+)/ || die "Anno line $annoline{$column} did not match for coordinate extraction";
        $acoord{$column} = $chrhash{$1}+$2;
        if ($scoord == $acoord{$column}) {
          $anno{$column} = (split(/\t/,$annoline{$column}))[$colnr{$colname}];
          next ANNOCOLUMNS_LOOP;
        }
        if ($scoord < $acoord{$column}) {
          $anno{$column} = '.';
          next ANNOCOLUMNS_LOOP;
        }
      }
      $anno{$column} = '.'; # when this is reached the anno file has come to its end so everything remaining in the SNV file is not covered
    }
  say "$sline\t", join "\t", @anno{@annocolumns};
}
close S;
foreach (@annocolumns) {
  close $annofh{$_};
}
