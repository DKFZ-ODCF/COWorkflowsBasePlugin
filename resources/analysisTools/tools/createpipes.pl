#!/usr/bin/env perl

# create a command with a bunch of pipes to use ngs2/trunk/tools/annotate_vcf.pl for deepAnnotation

use strict;
use warnings;
use String::Util qw(trim);

if (@ARGV < 5)
{
	die "USAGE: 1.basis file to which annotations will be added 2.config file 3.path to annotate_vcf.pl script 4. identifier of pipe to create 5. full path of tabix binary \n";
}

my $basis = shift;
my $filelist = shift;
my $overlapper = shift;
my $pipename = shift;
my $tabix_bin = shift;
# my $result = shift;

# all files to overlap with are listed in $filelist:
# name=/path/name.bed.gz or gff.gz or gvf.gz
# e.g.
# CpGislands=/ibios/co02/annotation/hg19/CpGIslands.bed.gz

open (FL, $filelist) or die "could not open $filelist: $!\n";
#my @help = ();
my ($column, $file, @options);
my $files = 0;
my $stem = "perl $overlapper -a $basis";
if($basis =~ /.gz$/){$stem = "zcat $basis | perl $overlapper -a -";}
my $commandlist = "";
my $type = "";

my $config_start = 0;

while (<FL>)
{
  $config_start || (/^#<PIPE_CONFIG:$pipename\s*$/ ? ($config_start=1)  : next);
  last if (/^#>PIPE_CONFIG/);
  
	if ($_ =~ /^#/)
	{
		next;
	}
	chomp;
	# remove quotation marks necessary for passing parameters with whitespaces through the shell
	$_ =~ tr /"//d;
	($column, $file) = /([^\s=]+)=(.+)/;
	$column=trim( $column );
	($file, @options) = split(':', $file);
	($type) = $file =~ /\.(\w+)\.gz$/;
	# bed, vcf, or gff3, but some are called "gvf" and are gff3
	if ($type =~ /bed/)
	{
		$type = "bed";
	}
	elsif ($type =~ /vcf/)
	{
		$type="vcf";
	}
	else
	{
		$type="gff3";
	}
	$commandlist.="$stem -b $file --bFileType=$type --columnName=$column --tabix_bin=$tabix_bin ";
	# additional columns to report? (default is only the "name" one)
	if (defined($options[0]))
	{
		$commandlist.=" --bAdditionalColumns=$options[0]";
	}
	# for dbSNP and cosmic, look for exact match; if not exact, report all
	# default is "2"
	if (defined($options[1]))
	{
		$commandlist.=" --reportLevel=$options[1]";
	}
	if (defined($options[2]))
	{
		$commandlist .= " $options[2]"; # for passing through options to annotate_vcf.pl
	}
	$files++;
	$stem = " | perl $overlapper -a -";
}
close FL;
#$commandlist.=" > $result";
print STDERR "$files files to overlap with.\n";
# print STDERR "The command for the overlap pipe is: $commandlist\n";
print $commandlist;
#my $returncode  = `$commandlist`;
#if ($returncode)
#{
#	die "return code != 0: $returncode\n";
#}
exit;
