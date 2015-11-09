#!/bin/bash

cd $DIR

$SAMTOOLS_BIN index $BAM
if [[ "$?" != 0 ]]
then
	echo "Non-zero exit code from samtools index; exiting..."
	exit 2
fi

$SAMTOOLS_BIN flagstat $BAM > ${BAM}_flagstat.tmp

if [[ "$?" != 0 ]]
then
	echo "Non-zero exit code from samtools flagstat; exiting..."
	exit 21
fi

mv ${BAM}_flagstat.tmp ${BAM}_flagstat.txt
