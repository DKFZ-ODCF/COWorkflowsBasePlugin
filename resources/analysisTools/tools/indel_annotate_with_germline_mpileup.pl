#!/usr/bin/env perl

use strict;
use warnings;
use v5.10;
use Getopt::Long;
use List::Util qw (sum min);

use constant DEBUG => 0; # set to 1 and it will also print mpileup lines

my %opts;
GetOptions( 'tumorFile=s' => \$opts{tumorfile},
            'germlineBam=s' => \$opts{germlinebam},
            'columnName=s' => \$opts{columnname},
            'padding=i' => \$opts{padding},
            'samtools_bin=s' => \$opts{samtools_bin},
            'referenceGenome=s' => \$opts{reference},
            'toolsDir=s' => \$opts{tools_dir},
            'scratchDir=s' => \$opts{scratch_dir},
            'mpileupOptions=s' => \$opts{mpileup_options},
          );

$opts{scratch_dir} = '.' if (!$opts{scratch_dir});

my $newcol = $opts{columnname};
my $pad = $opts{padding};
my $bash = '/bin/bash';

sub prepare_regionFile {
  my $regionfile = (split('/', $opts{tumorfile}))[-1];
  $regionfile .= "regionFile.bed.gz";
  my $cmd = "$opts{tools_dir}/vcf2padded_bed.pl $pad $opts{tumorfile} |  bgzip > $opts{scratch_dir}/$regionfile";
  system($cmd) and die "Error in bgzip for regionFile (command $cmd) $!";
  $cmd = "tabix -p bed $opts{scratch_dir}/$regionfile";
  system($cmd) and die "Error with tabix $!";
  return "$opts{scratch_dir}/$regionfile";
}

my $regionFile = prepare_regionFile();

sub open_mpileup {
  my $chr = $_[0];
  my $regionFile = $_[1];
  my $mpileup_cmd;
  $mpileup_cmd = "$opts{samtools_bin} mpileup $opts{mpileup_options} -l <(tabix $regionFile $chr) -r $chr -f $opts{reference} -ug $opts{germlinebam} | bcftools view -";
  open(my $mp_fh, '-|',$bash, '-c', "$mpileup_cmd") || die "Could not open mpileup with command $mpileup_cmd ($!)";
  return $mp_fh;
}


open(T, $opts{tumorfile}) || die "Could not open tumor file $opts{tumorfile}\n";
my $newcolidx;
my %t_fields;
my @columns;
my $chr='';
  
my $header;
while ($header = <T>) {
  last if ($header =~ /^\#CHROM/); # that is the line with the column names
  print $header; # print out every preceeding line
  die "No header found in file $opts{tumorfile}" if ($header !~ /^\#/);
}
$header || die "No header found in file $opts{tumorfile}";
chomp $header;
$header =~ s/^\#CHROM/CHROM/;
@columns = split(/\t/, $header);
$newcolidx = 0;
foreach (@columns) {
  if ($_ eq $newcol) {
    last;
  }
  $newcolidx++;
}
if ($newcolidx == @columns) {
  say '#', $header, "\t", "$newcol";
  push @columns, $newcol;
} else {
  say '#', $header;
}
  
my $t_line;
my ($t_left, $t_right);
my $t_chr_changed = 0;
my @mp_fields;
my %mpileups;
my $next_mpileup;
my ($mp_linectr, $mp_linenr);
my $mp_line;
my %stats;
my ($i_start, $i_end);
my (@alts, @homlens, $ri, $rl, $ai, $alt, $homlen);
my $mp_fh;

while ($t_line=<T>) {
  chomp($t_line);
  @t_fields{@columns} = split(/\t/, $t_line);
  if ($t_fields{CHROM} ne $chr) {
    $t_chr_changed = 1;
    $chr = $t_fields{CHROM};
    $mp_fh = open_mpileup($chr, $regionFile);
    %mpileups = ();
  } else {
    $t_chr_changed = 0;
  }
  $t_left = $t_fields{POS};
  #($t_right) = $t_fields{INFO} =~ /END=(\d+)/;
  $t_right = $t_fields{POS} + length($t_fields{REF}) -1;

  if ($t_chr_changed || (! defined($next_mpileup->{pos}) || ($next_mpileup->{pos}-$pad <= $t_right))) {
    # read new mpileup_lines until we have one where the coordinate is higher than t_right+$pad
    while ($mp_line=<$mp_fh>) {
      DEBUG && say $mp_line;
      next if ($mp_line =~ /^\#/);
      chomp($mp_line);
      @mp_fields = split(/\t/, $mp_line);
      if ($t_chr_changed) {
        next if ($mp_fields[0] ne $chr);
        $t_chr_changed = 0;
      }
      #DEBUG && say $mp_line;
      $mp_linectr++;
      if (defined($next_mpileup->{pos})) {
        $mpileups{$mp_linectr} = $next_mpileup;
        $next_mpileup = {};
      }
      $next_mpileup->{chr} = $mp_fields[0];
      $next_mpileup->{pos} = $mp_fields[1];
      $next_mpileup->{ref} = $mp_fields[3];
      $next_mpileup->{alt} = $mp_fields[4];
      $next_mpileup->{info}=$mp_fields[7];
      $mp_fields[7] =~ /DP=(\d+)/;
      $next_mpileup->{dp} = $1;
      $mp_fields[7] =~ /I16=([\d\,]+)/ || die "Could not parse I16";
      $next_mpileup->{i16} = [split(',',$1)];
      last if ($next_mpileup->{pos}-$pad > $t_right);
    }
    if (defined($next_mpileup->{pos}) && $next_mpileup->{pos}-$pad <= $t_right ) { # while loop terminated because mp_fh reached its end
      $mpileups{$mp_linectr} = $next_mpileup;
      $next_mpileup = {};
    }
  }
  %stats = ();
  foreach (qw(indel_cnt mm_cnt cov_t_left covq13_t_left cov_t_right covq13_t_right)) {
    $stats{$_}=0;
  }
  $stats{indel} = [];
  $stats{mm} = [];
  # Now compare t_line to all mpileup_lines
  # All mpileup positions which are in the distance of $pad from the breakpoints (t_left, t_right) are used for evaluation
  foreach $mp_linenr (sort keys %mpileups) {
    ($mpileups{$mp_linenr}{chr} eq $chr) || warn "Chromosomes between tumor and germline file (MP: $mpileups{$mp_linenr}{chr}:$mpileups{$mp_linenr}{pos}, VCF: $chr:$t_left-$t_right) do not match";

    if ( $mpileups{$mp_linenr}{pos}+$pad < $t_left-1 ) {
      delete($mpileups{$mp_linenr});
      next;
    }
    if ( ! (abs($mpileups{$mp_linenr}{pos}-$t_left) <= $pad || abs($mpileups{$mp_linenr}{pos}-$t_right) <= $pad)) {
      # not in target range
      next;
    }
    if ($mpileups{$mp_linenr}{info} !~ /INDEL/) {
      if ($mpileups{$mp_linenr}{pos} == $t_left) {
        $stats{cov_t_left} = $mpileups{$mp_linenr}{dp};
        $stats{covq13_t_left} = sum(@{$mpileups{$mp_linenr}{i16}}[0..3]);
      } 
      if ($mpileups{$mp_linenr}{pos} == $t_right) {
        $stats{cov_t_right} = $mpileups{$mp_linenr}{dp};
        $stats{covq13_t_right} = sum(@{$mpileups{$mp_linenr}{i16}}[0..3]);
      }
    }
    if (sum(@{$mpileups{$mp_linenr}{i16}}[0..3]) == 0) {
      # no coverage here with Q13 reads
      next;
    }
    if ($mpileups{$mp_linenr}{alt} eq 'X') {
      # no real variation here
      next;
    }
    if ($mpileups{$mp_linenr}{info} =~ /INDEL/) {
      $i_start = $mpileups{$mp_linenr}{pos};

      ### For determining Indel end it needs first to be left aligned
      @alts = split(/,/,$mpileups{$mp_linenr}{alt});
      $ri = $rl = length($mpileups{$mp_linenr}{ref}) -1;
      @homlens = ();
      foreach $alt (@alts) {
	$ri = $rl;
	$ai = length($alt) -1;
    
	while ($ri > 0 && $ai > 0) {
	  last if uc(substr($mpileups{$mp_linenr}{ref}, $ri, 1)) ne uc(substr($alt, $ai, 1));
	  $ri--;
	  $ai--;
	}
	#say $rl-$ri;
	push(@homlens, $rl - $ri);
	#say "@homlens";
      }

      $homlen = min(@homlens);
      if ($homlen) {
	$mpileups{$mp_linenr}{ref} = substr($mpileups{$mp_linenr}{ref}, 0, -$homlen);
	foreach $alt (@alts) {
	  $alt = substr($alt, 0, -$homlen);
	}
	$mpileups{$mp_linenr}{alt} = join ',',@alts;
      }
      ### End leftaligning indel

      $i_end = $i_start + length($mpileups{$mp_linenr}{ref}) - 1;

      $stats{indel_cnt}++;
      push(@{$stats{indel}}, join(',',@{$mpileups{$mp_linenr}{i16}}[0..3], $i_start, $i_end));
    } else {
      $stats{mm_cnt}++;
      push(@{$stats{mm}}, join(',',@{$mpileups{$mp_linenr}{i16}}[0..3], $mpileups{$mp_linenr}{pos}));
    }
  } # foreach $mp_linenr (sort keys %mpileups)
  $t_fields{$newcol} = 'COV_TLEFT='. $stats{cov_t_left}. ','. $stats{covq13_t_left}.';'.'COV_TRIGHT='. $stats{cov_t_right}. ','. $stats{covq13_t_right}. ';';
  $t_fields{$newcol} .= 'INDEL='.$stats{indel_cnt};
  if ($stats{indel_cnt}) {
    $t_fields{$newcol} .= ':'. join '/', @{$stats{indel}};
  }
  $t_fields{$newcol} .= ';MISMATCH='.$stats{mm_cnt};
  if ($stats{mm_cnt}) {
    $t_fields{$newcol} .= ':'. join '/', @{$stats{mm}};
  }
 
  say join "\t", @t_fields{@columns};
} # while ($t_line=<T>)
say STDERR "Processed $mp_linectr lines of mpileup output";
close T;
close $mp_fh || die "Could not close mpileup filehandle (broken pipe)";
unlink ($regionFile);
unlink($regionFile . ".tbi");
