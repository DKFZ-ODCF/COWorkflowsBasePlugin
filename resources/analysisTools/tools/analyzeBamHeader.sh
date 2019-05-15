#!/bin/bash
#
# Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
#
# This script offers several functions to get data out of a bam file header.
# The script is supposed to run in roddy environments and needs a proper configuration set.

function getRefGenomeAndChrPrefixFromHeader {
    local bamFile="${1:?No BAM file given}"
    if [[ "${disableAutoBAMHeaderAnalysis:-false}" == false ]]
    then
        CHROMOSOME_LENGTH_FILE="$chromosomeLengthFile_hg19"
    #    <cvalue name="CHROM_SIZES_FILE" value="${chromosomeSizesFile_hs37}" type="path" />

    #        <cvalue name='chromosomeSizesFile_mm10_GRC' value='${chromosomeSizesBaseDirectory_mm10}/GRCm38mm10.fa.chrLenOnlyACGT_realChromosomes.tab' type="path"/>
    #        <cvalue name='chromosomeSizesFile_mm10' value='${chromosomeSizesBaseDirectory_mm10}/mm10_1-19_X_Y_M.fa.chrLenOnlyACGT_realChromosomes.tab' type="path"/>
        countCHRPrefixes=$("$SAMTOOLS_BINARY" view -H "$bamFile" | grep "^@SQ" | grep "SN:chr" | wc -l)
        if [[ $countCHRPrefixes -gt 0 ]]; then
            CHR_PREFIX="chr"
            REFERENCE_GENOME="$referenceGenome_hg19_chr"
            CHROM_SIZES_FILE="$chromosomeSizesFile_hg19"
        else
            CHR_PREFIX=""
            REFERENCE_GENOME="$referenceGenome_1KGRef"
            CHROM_SIZES_FILE="$chromosomeSizesFile_hs37"
        fi
    else
        if [[ "${REFERENCE_GENOME:-}" == "" || ! -r "$REFERENCE_GENOME" ]]; then
            echo "The reference genome file is not readable ('${REFERENCE_GENOME:-}')! Aborting!"
            exit 250
        fi
        if [[ "${CHROM_SIZES_FILE:-}" == "" || ! -r "$CHROM_SIZES_FILE" ]]; then
            echo "The chromosome sizes file is not readable ('${CHROM_SIZES_FILE:-}')! Aborting!"
            exit 251
        fi
    fi
}

function getReadGroupsFromHeader {
    local bamFile="${1:?No BAM file given}"
    if [[ ! -r "$bamFile" ]]; then
        echo "Cannot read BAM file: '$bamFile'" >> /dev/stderr
        exit 249
    fi
	BAM_READ_GROUPS=( $("$SAMTOOLS_BINARY" view -H "$bamFile" | grep "^@RG" | $PERL_BINARY -e 'use strict; use warnings; open(BAM, "<$ARGV[0]") or die "Could not open the bam header\n"; my @RGnames; while(<BAM>){chomp; my @line = split("\t", $_); foreach(@line){next if($_ !~ /^ID:/); if($_ =~ /^ID:/){my $RGname = $_; $RGname =~ s/^ID://; push(@RGnames, $RGname);}}} print join(" ", @RGnames);' - ) )
}
