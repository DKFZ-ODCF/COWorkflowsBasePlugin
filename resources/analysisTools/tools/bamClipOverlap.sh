#!/bin/bash

set -x
cd $DIR

if [ -e $OUTBAM.tmp.bam ]
then
	rm $OUTBAM.tmp.bam
fi

$BAM_UTILS_BIN clipOverlap --in $INBAM --out $OUTBAM.tmp.bam --stats --poolSize $POOLSIZE
if [[ "$?" != 0 ]]
then
	echo "Non-zero exit code from bam clipOverlap; exiting..."
	exit 2
else
	mv $OUTBAM.tmp.bam $OUTBAM
fi

$SAMTOOLS index $OUTBAM
if [[ "$?" != 0 ]]
then
	echo "Non-zero exit code from samtools index; exiting..."
	exit 21
fi
