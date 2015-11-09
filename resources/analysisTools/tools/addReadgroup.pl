#!/usr/bin/env perl

use strict;
use warnings;
use Getopt::Std;

my %opts = (l=>"tumor",p=>"ILLUMINA",s=>"tumor",c=>"DKFZ");
getopts("i:s:d:l:p:u:c:h", \%opts);

my $file = $opts{i};
my $SM= $opts{s};
my $ID= $opts{d};
my $LB = $opts{l};
my $PL = $opts{p};
my $PU= $opts{u};
my $CN = $opts{c};


#print STDERR "ID:$ID\tSM:$SM\tLB:$LB\tPL:$PL\tPU:$PU\tCN=$CN\n";

if (defined $opts{h} || ! defined $file || ! defined $ID || ! defined $PU)
{
	die "USAGE: $0 [options]
	-i FILE	SAM file, required (set to - for pipe from STDIN)
	-s STRING	sample tag (SM) (default tumor)
	-d STRING	unique read group identifier (ID), required
	-l STRING	sample tag (SM) (default tumor)
	-p STRING	sequencing platform (SP) (default ILLUMINA)
	-u STRING	platform unit (PU = lane ID or run barcode), required
	-c STRING	sequencing center (CN), default DKFZ
	-h help\n";
}

open (FH, $file) or die "could not open $file: $!\n";
my $header = "";
while ($header = <FH>)
{
	last if ($header !~ /^\@/); # first line with a read
	print "$header";
}
# between the last line of the header, i.e. with @, print the @RG line in such a way:
# @RG     ID:131031_SN7001393_0107_AH79AKADXX_L001        PL:ILLUMINA     LB:ES_00A_TD_ES_00A_TD_ES_00A_BD        SM:sample_ES_00A_TD_ES_00A_TD_ES_00A_BD
print "\@RG\tID:$ID\tSM:$SM\tLB:$LB\tPL:$PL\tPU:$PU\tCN=$CN\n";

# afterwards, attach the RG tag to each line in form of RG:Z:$RG
#HWI-ST1393:107:H79AKADXX:1:2203:3847:85658      99      1       10000   0       101M    =       10007   108     ACAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAAC  CCCFFFFFHHHHHJJJJJJJJJJJIJJIJJIGIIGIJIJJIFHGGECHIJIGEGHCGEGFHGHE@DFDCCEBD?A?=BB(9<??BBCCA8AADD<A?8ACA -X0:i:1  X1:i:334        MD:Z:1T99       PG:Z:MarkDuplicates.3   RG:Z:131031_SN7001393_0107_AH79AKADXX_L001      XG:i:0  AM:i:0  NM:i:1  SM:i:0  XM:i:1  XN:i:1-XO:i:0  XT:A:U

# $header contains now the first read!
chomp $header;
print $header, "\tRG:Z:$ID\n";

while (<FH>)
{
	chomp;
	print $_, "\tRG:Z:$ID\n";
}
close FH;
exit;
