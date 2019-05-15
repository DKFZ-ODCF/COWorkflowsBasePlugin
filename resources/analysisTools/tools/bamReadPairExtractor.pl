#!/usr/bin/env perl
#
# Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
#

use strict;
use warnings;
use v5.14;

BEGIN {
    use Getopt::Long;
    Getopt::Long::Configure("pass_through");

    my %opts = (                               # opts are OPTIONAL
        'mqt'                  => 1,           #0, # set to 0 to disable selection by low mapping quality; otherwise reads with mq < mqt will be selected
        'maxMem'               => 30000000000, #not yet implemented
        'maxReadCache'         => 40000000,
        'debug'                => '',
        'count'                => 1,
        'unmapped'             => 1,
        'noProperPair'         => 1,
        'softClipped'          => 1,
        'noiseOverlap'         => 1,
        'fastq'                => 1,
        'scarf'                => 0,
        'trimQualityThreshold' => 0, # 13
        'minReadLength'        => 0, # 50
        'noiseIntervals'       => '',
        'gzip'                 => 1,

    );

    my %params = ( # params are REQUIRED
        'input' => '',
    );

    GetOptions('mappingQualityThreshold|mqt:i' => \$opts{'mqt'},
        'maxMem:i'                             => \$opts{'maxMem'}, #not yet implemented
        'maxReadCache:i'                       => \$opts{'maxReadCache'},
        'input=s'                              => \$params{'input'},
        'noiseIntervals:s'                     => \$opts{'noiseIntervals'},
        'debug!'                               => \$opts{'debug'},
        'count!'                               => \$opts{'count'},
        'unmapped!'                            => \$opts{'unmapped'},
        'noProperPair!'                        => \$opts{'noProperPair'},
        'softClipped!'                         => \$opts{'softClipped'},
        'noiseOverlap!'                        => \$opts{'noiseOverlap'},
        'fastq!'                               => \$opts{'fastq'},
        'scarf!'                               => \$opts{'scarf'},
        'gzip!'                                => \$opts{'gzip'},
        'trimQualityThreshold|tqt:i'           => \$opts{'trimQualityThreshold'},
        'minReadLength|mrl:i'                  => \$opts{'minReadLength'},
        'help|?'                               => \&usage,
    );


    #my $lthreshold = 50; ### make option
    #my $qthreshold = 13; ### make option


    #check if required params are given
    my @missing;
    for my $key (keys %params) {
        #say $key;
        push(@missing, $key) if (!$params{$key});
    }
    if (@missing) {
        say "Missing parameter(s): @missing";
        usage();
    }

    if (@ARGV) {
        print "Invalid option: @ARGV\n";
        usage();
    }

    sub usage {
        # TODO make this nicer with pod2usage
        print "usage: $0 ";
        for (keys %opts) {
            print '[--', $_;
            print "=", uc($_) if (defined $opts{$_});
            print '] ';
        }
        for (keys %params) {
            print '--', $_;
            print "=", uc($_) if (defined $params{$_});
            print ' ';
        }
        exit;
    }

    require constant;
    # Now make constants
    for (keys %opts) {
        constant->import(uc($_), $opts{$_});
    }
    for (keys %params) {
        constant->import(uc($_), $params{$_});
    }

}

#  my $p;
#  my $ctr = 0;
my $pfx = INPUT() =~ s/\.bam$//r; # if input_bam ends with .bam remove the suffix
#  my $samsort = "samtools sort -m " . MAXMEM . " -no " . INPUT . " sortpfx | samtools view - ";
my $samview = "samtools view " . INPUT;
DEBUG && say "samview command: $samview";

open(my $bamfh, "$samview |");
my $readHash;

my $noiseHash;
if (NOISEOVERLAP()) {
    $noiseHash = initialize_noiseHash(NOISEINTERVALS());
}

## open output files: three fastq and three scarf (for each mate number one file and one for unpaired reads)
my (%fq, %sc); # hashes with refs to fastq and scarf filehandles
my $cmd = (GZIP()) ? '| gzip -c > ' : '>';
my $sfx = (GZIP()) ? '.gz' : '';

for (qw(1 2 u r)) {
    open($fq{$_}, $cmd . $pfx . "_extracted_$_.fastq" . $sfx) if (FASTQ() > 0);
    open($sc{$_}, $cmd . $pfx . "_extracted_$_.scarf" . $sfx) if (SCARF() > 0);
}

my $selected;
my ($fields, $fields2);
my ($flags, $flags2);
my ($matenr, $matenr2);
my %count;
my $hs = 0; #
my $maxhs = 0;

while (<$bamfh>) {
    $selected = 0;
    #      last if ++$ctr > 10;
    COUNT && $count{total_reads}++;
    $fields = [ (split(/\t/)) ];
    $flags = get_flags($fields->[1]);
    next if ($flags->{NOT_PRIMARY}); #skip secondary alignments
    COUNT && $count{primary_alignments}++;

    #      printf "Flags_paired: %s\n",$flags->{PAIRED};
    #      $p = $flags->{PAIRED} ? 'paired' : 'unpaired';
    #      print join(" ", ("1st:", $fields->[0], $fields->[1]), $p) . "\n";
    if ($readHash->{$fields->[0]}) {
        # pair complete
        $fields2 = $readHash->{$fields->[0]}[0];
        $flags2 = $readHash->{$fields->[0]}[1];

        $flags2->{'PAIRED'} || warn "First read is paired but second is not in input line $.";
        $selected = check_selection($fields, $flags, $noiseHash);
        if (!$selected) {
            $selected = check_selection($fields2, $flags2, $noiseHash);
        }
        if (!$selected) {
            delete($readHash->{$fields->[0]});
            $hs--;
            next;
        }
        else {
            $matenr = (!$flags->{PAIRED}) ? 1 : ($flags->{FIRST_MATE}) ? 1 : 2;
            $matenr2 = (!$flags2->{PAIRED}) ? 1 : ($flags2->{FIRST_MATE}) ? 1 : 2;
            ($matenr != $matenr2) || die "Mate number clash in input line $.";

            if ((!$flags->{UNMAPPED}) && $flags->{REVERSED}) {
                ($fields->[9] = reverse $fields->[9]) =~ tr/ACGTacgt/TGCAtgca/;
                $fields->[10] = reverse $fields->[10];
                if ($flags->{M_UNMAPPED}) {
                    ($fields2->[9] = reverse $fields2->[9]) =~ tr/ACGTacgt/TGCAtgca/;
                    $fields2->[10] = reverse $fields2->[10];
                }
            }

            if (!$flags2->{UNMAPPED} && $flags2->{REVERSED}) {
                ($fields2->[9] = reverse $fields2->[9]) =~ tr/ACGTacgt/TGCAtgca/;
                $fields2->[10] = reverse $fields2->[10];
                if ($flags2->{M_UNMAPPED}) {
                    ($fields->[9] = reverse $fields->[9]) =~ tr/ACGTacgt/TGCAtgca/;
                    $fields->[10] = reverse $fields->[10];
                }
            }

            #trim sequences that contain bases with quality < qthreshold
            if (TRIMQUALITYTHRESHOLD() > 0) {
                trim_bwa($fields, TRIMQUALITYTHRESHOLD());
                trim_bwa($fields2, TRIMQUALITYTHRESHOLD());
            }


            #check if length > lthreshold
            my $seqlength = length($fields->[9]);
            my $seqlength2 = length($fields2->[9]);
            if (MINREADLENGTH() > 0 && $seqlength < MINREADLENGTH() && $seqlength2 >= MINREADLENGTH()) {
                if (FASTQ) {
                    printf {$fq{u}} "\@%s/%s\n%s\n+\n%s\n", $fields2->[0], $matenr2, $fields2->[9], $fields2->[10];
                }
                if (SCARF) {
                    printf {$sc{u}} "%s/%s\:%s\:%s\n", $fields2->[0] =~ s/[^\d:]//rg, $matenr2, $fields2->[9], $fields2->[10] =~ tr/[\x{21}-\x{5F}]/[\x{40}-\x{7E}]/r;
                }
                COUNT && $count{single_read_deleted}++;
                delete($readHash->{$fields->[0]});
                $hs--;
                next;
            }
            elsif (MINREADLENGTH() > 0 && $seqlength2 < MINREADLENGTH() && $seqlength >= MINREADLENGTH()) {
                if (FASTQ) {
                    printf {$fq{u}} "\@%s/%s\n%s\n+\n%s\n", $fields->[0], $matenr, $fields->[9], $fields->[10];
                }
                if (SCARF) {
                    printf {$sc{u}} "%s/%s\:%s\:%s\n", $fields->[0] =~ s/[^\d:]//rg, $matenr, $fields->[9], $fields->[10] =~ tr/[\x{21}-\x{5F}]/[\x{40}-\x{7E}]/r;
                }
                delete($readHash->{$fields->[0]});
                COUNT && $count{single_read_deleted}++;
                $hs--;
                next;
            }
            elsif (MINREADLENGTH() > 0 && $seqlength < MINREADLENGTH() && $seqlength2 < MINREADLENGTH()) {
                #both sequences too short, only delete read ID in readHash, no output
                delete($readHash->{$fields->[0]});
                COUNT && $count{read_pair_deleted}++;
                $hs--;
                next;
            }
            else {
                #both sequences still long enough


                # write out pair
                if (FASTQ) {
                    printf {$fq{$matenr}} "\@%s/%s\n%s\n+\n%s\n", $fields->[0], $matenr, $fields->[9], $fields->[10];
                    printf {$fq{$matenr2}} "\@%s/%s\n%s\n+\n%s\n", $fields2->[0], $matenr2, $fields2->[9], $fields2->[10];
                }
                if (SCARF) {
                    printf {$sc{$matenr}} "%s/%s\:%s\:%s\n", $fields->[0] =~ s/[^\d:]//rg, $matenr, $fields->[9], $fields->[10] =~ tr/[\x{21}-\x{5F}]/[\x{40}-\x{7E}]/r;
                    printf {$sc{$matenr2}} "%s/%s\:%s\:%s\n", $fields2->[0] =~ s/[^\d:]//rg, $matenr2, $fields2->[9], $fields2->[10] =~ tr/[\x{21}-\x{5F}]/[\x{40}-\x{7E}]/r;
                }

                delete($readHash->{$fields->[0]});
                $hs--;
                next;
            }
        }
    }
    if (!$flags->{PAIRED}) {
        # unpaired read
        COUNT && $count{unpaired_reads}++;
        $selected = check_selection($fields, $flags, $noiseHash);
        if (!$selected) {
            next;
        }
        else {
            $matenr = 1;
            # write out unpaired
            if (FASTQ) {
                printf {$fq{u}} "\@%s/%s\n%s\n+\n%s\n", $fields->[0], $matenr, $fields->[9], $fields->[10];
            }
            if (SCARF) {
                printf {$sc{u}} "%s/%s\:%s\:%s\n", $fields->[0] =~ s/[^\d:]//rg, $matenr, $fields->[9], $fields->[10] =~ tr/[\x{21}-\x{5F}]/[\x{40}-\x{7E}]/r;
            }
            next;
        }
    }
    # add read to readhash
    $readHash->{$fields->[0]} = [];
    push(@{$readHash->{$fields->[0]}}, $fields, $flags);
    $hs++;
    $maxhs = $hs if ($hs > $maxhs);
    die "Read cache exceeded limit in input line $." if ($hs > MAXREADCACHE()); # TODO: implement cache in temp file
}


### write remaining reads from read hash to unpaired (are declared as being paired after seq but do actually not have a matching read)
foreach my $key (keys %{$readHash}) {
    $selected = 0;
    COUNT && $count{remaining_reads}++; # total number, no selection

    if (FASTQ) {
        printf {$fq{r}} "\@%s/%s\n%s\n+\n%s\n", $fields->[0], $matenr, $fields->[9], $fields->[10];
    }
    if (SCARF) {
        printf {$sc{r}} "%s/%s\:%s\:%s\n", $fields->[0] =~ s/[^\d:]//rg, $matenr, $fields->[9], $fields->[10] =~ tr/[\x{21}-\x{5F}]/[\x{40}-\x{7E}]/r;
    }

    $fields = $readHash->{$key}[0];
    $flags = $readHash->{$key}[1];

    $selected = check_selection($fields, $flags, $noiseHash);
    if ($selected) {
        $matenr = (!$flags->{PAIRED}) ? 1 : ($flags->{FIRST_MATE}) ? 1 : 2;

        if ((!$flags->{UNMAPPED}) && $flags->{REVERSED}) {
            ($fields->[9] = reverse $fields->[9]) =~ tr/ACGTacgt/TGCAtgca/;
            $fields->[10] = reverse $fields->[10];
        }
        if (TRIMQUALITYTHRESHOLD() > 0) {
            trim_bwa($fields, TRIMQUALITYTHRESHOLD());
        }
        my $seqlength = length($fields->[9]);
        if (MINREADLENGTH() > 0 && $seqlength >= MINREADLENGTH()) {
            COUNT && $count{remaining_reads_passedq}++;
            if (FASTQ) {
                printf {$fq{u}} "\@%s/%s\n%s\n+\n%s\n", $fields->[0], $matenr, $fields->[9], $fields->[10];
            }
            if (SCARF) {
                printf {$sc{u}} "%s/%s\:%s\:%s\n", $fields->[0] =~ s/[^\d:]//rg, $matenr, $fields->[9], $fields->[10] =~ tr/[\x{21}-\x{5F}]/[\x{40}-\x{7E}]/r;
            }
        }
    }

}

say "Max_hash_size: $maxhs";
if (COUNT()) {
    foreach (sort keys %count) {
        say "$_: $count{$_}";
    }
}




sub check_selection {
    #takes ref to array containing the fields from one SAM file line and ref to flag hash;
    #test various selection criteria
    #returns either 0 (not select) or 1 (selected)
    my ($fields, $flags, $nh) = @_;
    #TODO: check validity of input
    #    my $selected = 0;
    if (UNMAPPED && $flags->{UNMAPPED}) {
        COUNT && $count{unmapped}++;
        return 1;
    } #read unmapped
    if (UNMAPPED && $flags->{PAIRED} && $flags->{M_UNMAPPED}) {
        COUNT && $count{m_unmapped}++;
        return 1;
    } #read paired and mate unmapped
    if (NOPROPERPAIR && $flags->{PAIRED} && !$flags->{MAP_PAIR}) {
        COUNT && $count{no_proper_pair}++;
        return 1;
    } #read paired and both mapped and no proper pair
    if (SOFTCLIPPED && $fields->[5] =~ m/S/) {
        COUNT && $count{softclipped}++;
        return 1;
    } # read soft-clipped
    if (MQT > 0 && $fields->[4] < MQT) {
        COUNT && $count{mqt}++;
        return 1;
    } # mapping quality below threshold


    if (NOISEOVERLAP() == 0) { # no overlap with noise intervals requested? Then we are done.
        return 0;
    }

    #check if we have overlap to noise intervals
    # first I need the coordinates of the alignment
    my $padded_length = 0;
    my ($op, $el);

    while ($fields->[5] =~ /(\d+)([MIDNSHP=X])/g) {
        ($el, $op) = ($1, $2);
        $padded_length += $el if ($op =~ /[MDN=XP]/);
    }
    my ($lc, $rc); #left coordinate and right coordinate; the left coordinate is directly from the 4th field of the BAM file; when accessed with SAMtools view it is 1-based;
    $lc = $fields->[3];
    $rc = $lc + $padded_length - 1;

    #now check if these alignment coordinates  overlap an interval in the noise hash
    my $chr = $fields->[2];
    my @a; #(1st to 3rd-last digit of lc, last 2 digits of lc, 1st to 3rd-last digit of rc, last 2 digits of rc)
    @a = (substr($lc, 0, -2), substr($lc, -2), substr($rc, 0, -2), substr($rc, -2));
    map {$_ = 0 if (!$_)} @a;
    my $i;
    DEBUG && say "@a";
    # first the easy case: alignment falls into only one bin
    # test if interval [t1;t2] overlaps with interval [n1;n2]
    # this is true if (t1 <= n1 && t2 >= n1) || (t1 >= n1 && t1 <= n2)
    if ($a[0] == $a[2]) {
        DEBUG && say '$a[0] == $a[2]';
        if (DEBUG && defined($nh->{$chr}{$a[0]})) {
            say '(((', $a[1], ' <= ', $nh->{$chr}{$a[0]}{'start'}, ' && ', $a[3], ' >= ', $nh->{$chr}{$a[0]}{'start'}, ') || (', $a[1], ' >= ', $nh->{$chr}{$a[0]}{'start'}, ' && ', $a[1], ' <= ', $nh->{$chr}{$a[0]}{'end'}, ')))';
        }
        if (defined($nh->{$chr}{$a[0]}) && (($a[1] <= $nh->{$chr}{$a[0]}{'start'} && $a[3] >= $nh->{$chr}{$a[0]}{'start'}) || ($a[1] >= $nh->{$chr}{$a[0]}{'start'} && $a[1] <= $nh->{$chr}{$a[0]}{'end'}))) {
            COUNT && $count{noise_overlap}++;
            return 1;
        }
    }
    else {
        # the alignment falls into at least two bins; test each bin separately
        DEBUG && say '$a[0] != $a[2]';
        #the leftmost bin: we have an overlap if t1 <= n2
        if (defined($nh->{$chr}{$a[0]}) && ($a[1] <= $nh->{$chr}{$a[0]}{'end'})) {
            COUNT && $count{noise_overlap}++;
            return 1;
        }
        #middle bins if the alignemnt spans more than two: here we have always an overlap if a noise interval exist
        for ($i = 1; $i < $a[2] - $a[0]; $i++) {
            if (defined($nh->{$chr}{$a[0] + $i})) {
                COUNT && $count{noise_overlap}++;
                return 1;
            }
        }
        #right bin: we have an overlap if t2 >= n1
        if (defined($nh->{$chr}{$a[2]}) && ($a[3] >= $nh->{$chr}{$a[2]}{'start'})) {
            COUNT && $count{noise_overlap}++;
            return 1;
        }
    }
    return 0;
}




sub get_flags {
                         # takes integer value from SAM file FLAG field and returns ref to hash with all tags as keys and either 0 or 1 as value
    my $flagint = $_[0]; #first element in query, here $fields->[1] = FLAG tag
    my $flaghash = {};
    $flaghash->{PAIRED} = $flagint & 1;        #the read is paired in sequencing, no matter whether it is mapped in a pair
    $flaghash->{MAP_PAIR} = $flagint & 2;      #the read is mapped in a proper pair
    $flaghash->{UNMAPPED} = $flagint & 4;      #the query sequence itself is unmapped
    $flaghash->{M_UNMAPPED} = $flagint & 8;    #the mate is unmapped
    $flaghash->{REVERSED} = $flagint & 16;     # query is reversed
    $flaghash->{M_REVERSED} = $flagint & 32;   # mate is reversed
    $flaghash->{FIRST_MATE} = $flagint & 64;   # first read in pair
    $flaghash->{SECOND_MATE} = $flagint & 128; # second read in pair
    $flaghash->{NOT_PRIMARY} = $flagint & 256; #the alignment is not primary (a read having split hits may have multiple primary alignment records)
    $flaghash->{QC_FAILED} = $flagint & 512;   #not passing quality controls
    $flaghash->{DUPLICATE} = $flagint & 1024;  #PCR or optical duplicate
    return $flaghash;
}


sub initialize_noiseHash {
    my $noiseFile = $_[0];
    my ($chr, $istart, $iend);
    my %noisehash;
    my @a;
    my $i;

    open(my $nfh, $noiseFile) || die "Could not open noise interval file $noiseFile";
    while (<$nfh>) {
        chomp;
        ($chr, $istart, $iend) = split(/\t/);
        @a = (substr($istart, 0, -2), substr($istart, -2), substr($iend, 0, -2), substr($iend, -2));
        $noisehash{$chr}{$a[0]}{'start'} = $a[1];
        if ($a[0] == $a[2]) { #start and end position fall into same bin
            $noisehash{$chr}{$a[0]}{'end'} = $a[3];
        }
        else {
            $noisehash{$chr}{$a[0]}{'end'} = 99;
            for ($i = 1; $i < ($a[2] - $a[0]); $i++) {
                $noisehash{$chr}{$a[0] + $i}{'start'} = 0;
                $noisehash{$chr}{$a[0] + $i}{'end'} = 99;
            }
            $noisehash{$chr}{$a[2]}{'start'} = 0;
            $noisehash{$chr}{$a[2]}{'end'} = $a[3];
        }
    }
    return \%noisehash;
}

sub trim_bwa {
    my ($fields, $qthreshold) = @_;
    my $pos = length($fields->[10]) - 1;
    my $maxPos = $pos;


    ## skip empty read.
    if ($pos <= 0) {
        return;
    }
    my $area = 0;
    my $maxArea = 0;
    my @qarray = unpack("C*", $fields->[10]);
    DEBUG && say '@qarray';

    while ($pos > 0 && $area >= 0) {
        $area += $qthreshold - ($qarray[($pos)] - 33);
        ## not <=  for more aggressive trimming : if multiple equal maxima => use shortest read.
        if ($area < $maxArea) {
            $pos--;
            next;
        }
        else {
            $maxArea = $area;
            $maxPos = $pos;
            $pos--;
            next;
        }
    }
    ## The online perl approach is wrong if the high-quality part is not long enough to reach 0.
    if ($maxPos == 0) {
        # maximal badness on position 0 => no point of no return reached.
        $fields->[9] = '';
        $fields->[10] = '';
        return;
    }
    # otherwise trim before position where area reached a maximum 
    # $maxPos as length of substr gives positions zero to just before the maxpos.
    $fields->[9] = substr($fields->[9], 0, $maxPos);
    $fields->[10] = substr($fields->[10], 0, $maxPos);
    return;
}
