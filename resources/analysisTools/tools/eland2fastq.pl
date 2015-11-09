#!/usr/bin/perl

# convert ELAND format to fastq

use strict;
use warnings;
use feature 'switch';	# to spare a long list of elsif

if (@ARGV < 1)
{
	die "Usage: ELAND output file (s_1_export.txt) to convert to fastq\n";
}

my $file = shift;
open (FH, $file) or die "Could not open $file: $!\n";

my @help = ();	# will cost time to split so many lines ...
# count how many reads in total
my $all = 0;
#and how many are "QC" at pos[10], these obviouisly failed quality check (which would be a flag in sam), looks like that applies to the all-N ones
my $qcfail = 0;
# the "NM" ones; N and Y = ?? maybe ambiguous
my $unmapped = 0;
# mapped: mm_ref at pos[10]; could even make a SAM out of the mapped ones
my $mapped = 0;
# and the ones with numbers (I saw 255:255:255, too) - what do they mean? mapped to filter? can have N or Y just as the NM ones
my $other = 0;

# Postepska data:
#HWI-ST164  198 1   1   1222    1007   0   1    NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN    BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB    QC N
#HWI-ST164  198 1  68   17674  200305  0   1    CTAATAAGAATAAATATAGGGAAGAGTGGTAGGGGA    ^_BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB    NM Y
#HWI-ST164  198 1  68   17531  200309  0   1    GATCCAGTATGGGGAAAGGGTAGATTTTGAGAATGG    _W_^OI_BBBBBBBBBBBBBBBBBBBBBBBBBBBBB    NM N
#HWI-ST164  198 1  68    6593  200511  0   1    GTTTGTGTGGGTGGTTTTTTGTTTTTTNTNNNGGTT    BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB    0:0:1 Y
#HWI-ST164  198 1  68    6165  200511  0   1    GATTTTTGTTTGGGGGGGTTTTTTGTTTTNNNGGTG    aaaKa__ODGIW_]_BBBBBBBBBBBBBBBBBBBBB    0:1:0 N

# if a read is mapped
#HWI-ST164  198 1  68   18853  200252  0  1  GATCATTTTTAAATTTAATTTTAATTGTTAGTGGGT b\b_bbabaaBBBBBBBBBBBBBBBBBBBBBBBBBB  mm_ref_chr11.fa  22637293  R 23C12  5 Y
#HWI-ST164  198 1  68   19359  200251  0  1 AAAGTAATTGTTGATAAATTTTATTTTTGTTGTTAG  cc^cacccca__BBBBBBBBBBBBBBBBBBBBBBBB   mm_ref_chrX.fa   107229515 R  13C1T20 11 Y


# Yu et al. data from GEO:
#HWI-EAS159      207B8AAXX       8       1       920     661                     TCTTCACACTTAAACAGATTAATAAAGACTAGAAAT    [[[[[[[[[[[[[[[[[[[[[[[[[[Z[[[XKXXXP    hs_ref_chr7.fa          105260077       R       36        65
#HWI-EAS159      207B8AAXX       8       1       894     795                     GGAGGATCGCTTGATCCCCCAAGGCAGTGTTTTCAT    [[[[[[[[YY[[PXCXYYYXYMOXXLLEUEVVPPII    NM                                            Y


# a fastq name would be:
#@GAPC01_0008:3:1:1040:11492#0/1
#would be in _export:
#GAPC01  8  3   1   1040  11492  0   1 TCTAAGAAGANATTTGGGACATACAAATANAAAGTT   bbbbbbbbbbEbbbbaaaaabbbbbbbbbFa`a```    chr3   22529522   R 10A18G6 98 Y
# the problem ist that leading zeros are deleted!
#http://perldoc.perl.org/functions/sprintf.html
# # Format number with up to 8 leading zeroes
# $result = sprintf("%08d", $number);
#printf '<%06s>', 12;   # prints "<000012>"

while (<FH>)
{
	@help = split (/\t/, $_);
	$all++;
	#if ($unmapped > 10) {last;}
	# the super comfortable switch feature that spares a lot of elsifs
	# this also has a continue (to "fall through" to the next when), but no break (not necessary)
	given ($help[10])
	{
		when ("NM")
		{
			$unmapped++;
		}
		when ("QC")
		{
			$qcfail++;
			next;	# can spare printing it!
		}
		when ($_ =~ /^ref/)
		{
			$mapped++;
			# construct entry
		}
		default
		{
			$other++;
		}
	}
	# print fastq
	print "\@${help[0]}_";
	printf '%04s', $help[1];
	print ":$help[2]:$help[3]:$help[4]:$help[5]#0/1\n$help[8]\n+\n$help[9]\n";

}
close FH;
print STDERR "$all reads; $mapped mapped, $unmapped unmapped, $other with 'number', $qcfail failed QC\n";
exit;
