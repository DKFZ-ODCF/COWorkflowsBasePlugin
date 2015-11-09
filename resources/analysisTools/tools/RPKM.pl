#!/usr/bin/perl

# calculate RPKM

use strict;
use warnings;

if (@ARGV < 2)
{
	die "$0 (1)BED file with read numbers and coordinates to calculate RPKM - (2)total number of mapped reads\n";
}

my $file = shift;
my $total = shift;

# in million:
$total = $total/1_000_000;

my $ctr = 0;
my @help = ();
my $fpkm = 0;
my $rncol = 0;
open (FH, $file) or die "could not open $file: $!\n";
while (<FH>)
{
	$ctr++;
	#if ($ctr > 3){last;}
	chomp $_;
	@help = split ("\t", $_);
	# the column with the read count is the 3rd-to-last one
	$rncol = $#help-3;
	# fragments per kilobase of feature (exon/repeat/...) per million mapped reads
	$fpkm = sprintf ("%.4f", ($help[$rncol]/(($help[2]-$help[1])/1000)/$total));
	print $_, "\t", $fpkm, "\n";
}
close FH;
print STDERR "$ctr lines\n";
