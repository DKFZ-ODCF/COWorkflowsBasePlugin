#!/usr/bin/env perl
#
# Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
#

use strict;
use warnings;
use v5.10;

if (@ARGV < 1) {
    die "need directory path as argument\n";
}

my $dir = $ARGV[0];
my $fastq_suffix = $ARGV[1];
my $fastq_prefix = $ARGV[2];
opendir(my $dh, $dir) or die "Could not open directory $dir";
my @lanes = readdir($dh);
closedir $dh;
if (defined($fastq_suffix)) {
    @lanes = grep {/$fastq_suffix$/} @lanes;
}
if (defined($fastq_prefix)) {
    @lanes = grep {/^$fastq_prefix/} @lanes;
}

# now find the block where the read index is; this is the rightmost block which is not unique (and matches /^R?[1-3]/)
my @blocks;
my @allblocks;
for my $lane (@lanes) {
    #$lane = (split(/\./,$lane))[0]; # get rid of filename suffices
    @blocks = reverse split(/_/, $lane); # now get blocks, starting from the right side
    map {$allblocks[$_]->{$blocks[$_]} = 1} (0 .. $#blocks);
}
my ($i, $index, $is_index);
for $i (0 .. $#allblocks) {
    $index = $i;
    if (scalar keys %{$allblocks[$i]} > 1) {
        $is_index = 1;
        for (keys %{$allblocks[$i]}) {
            $is_index = 0 if ($_ !~ /^R?[1-3]/);
        }
        last if ($is_index);
    }
}
$index = -1 * ($index + 1);
my %lanes;
@lanes{@lanes} = (1) x @lanes;
my @lanepairs;
my @pair;
my $pair;
my $laneID;

foreach my $lane (@lanes) {
    @blocks = split(/_/, $lane);
    if ($blocks[$index] =~ /^R?1/) {
        @pair = @blocks;
        $pair[$index] =~ s/1/2/;
        $pair = join '_', @pair;
        $laneID = join('_', @pair[0 .. $#pair + $index]);
        if (defined($lanes{$pair})) {
            push(@lanepairs, "$lane:$pair:$laneID");
        }
        else {
            warn "Unpaired lane detected: $lane";
        }
    }
}
print join ' ', @lanepairs;
