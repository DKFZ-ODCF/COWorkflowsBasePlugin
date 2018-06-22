#! /usr/bin/env perl
#
# Copyright (c) 2018 German Cancer Research Center (DKFZ).
#
# Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/COWorkflowsBasePlugin/LICENSE).
#

use strict;
use warnings;

# evaluate result of indel_annotate_with_germline_mpileup.pl

if (@ARGV < 3)
{
	die "USAGE: (1)vcf formatted file for confidence classification - (2)label of column with mpileup padding information - (3)label of column with preliminary annotation (ANNOTATION_...)\n";
}

my $file = shift;
my $mpilecollabel = shift;
my $annocollabel = shift;

open (FH, $file) or die "Could not open $file: $!\n";

my $header = "";
while ($header = <FH>)
{
	last if ($header =~ /^\#CHROM/); # that is the line with the column names
	print "$header";
}
die "No header found in file $file" if (! $header);
chomp $header;

# complement header for the new colum
my $addannoinfo = $annocollabel;
$addannoinfo =~ s/ANNOTATION_//;	# RNA / ...
$addannoinfo = "REANNOTATION_WITH_PADDING_".$addannoinfo;
print $header, "\t$addannoinfo\n";

my @help = (split "\t", $header);
my $mpcol = 0;
my $annocol = 0;
for (my $c = 0; $c < @help; $c++)
{
	if ($help[$c] eq "$mpilecollabel")
	{
		$mpcol = $c;
	}
	if ($help[$c] =~ /$annocollabel/)
	{
		$annocol = $c;
	}
}

if ($mpcol == 0)
{
	die "could not find label '$mpilecollabel' in header!\n";
}

if ($annocol == 0)
{
	die "could not find label matching '$annocollabel' in header!\n";
}

my $entries = 0;	# line counter
my $present= 0;	# present in the padding region (may well have been already at the exact position)
my $line = "";	# current line
my ($gindel, $gmism) = (0, 0);	# padding information: # indels and # mismatches.
# a MPILEUP padding entry looks like that: COV_TLEFT=39,38;COV_TRIGHT=0,0;INDEL=1:0,0,21,15,271302,271308;MISMATCH=5:22,18,0,1,271294/22,16,0,1,271296/19,16,1,2,271298/18,15,3,2,271302/20,14,0,2,271308
# i.e. 1 Indel: DP4, pos and end; 5 Mismatches: each DP4, pos
# => re-classify "not_present" as "present" if there is >= 1 indel or 5 mismatches

my $newannotation= "";	# take original annotation; if indel found in padding region, change according to type (RNA or WGS)
while (<FH>)
{
	$line = $_;
	$entries++;
	chomp $line;
	@help = split ("\t", $line);
	$newannotation= $help[$annocol];
	($gindel, $gmism) = $help[$mpcol] =~ /INDEL=(\d+).*;MISMATCH=(\d+)/;
	if ($gindel > 0 || $gmism >= 5)
	{
		if ($annocollabel =~ /RNA/)
		{
			$newannotation= "var_expressed";
		}
		else	# WGS
		{
			$newannotation= "var_present";
		}
		$present++;
	}
	print "$line\t$newannotation\n";
}
close FH;
print STDERR "$entries entries, $present are present in the padding region.\n";
exit;
