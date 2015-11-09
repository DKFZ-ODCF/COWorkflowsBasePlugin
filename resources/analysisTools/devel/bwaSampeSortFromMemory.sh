#!/bin/bash

#PBS -q highmem
#PBS -l walltime=50:00:00
#PBS -l nodes=1:ppn=12
#PBS -l mem=120g
#PBS -m a

source ${CONFIG_FILE}

set -o pipefail
# use scratch dir for temp files: samtools sort uses the current working directory for them
cd $PBS_SCRATCH_DIR/$PBS_JOBID

SHM_FILE0=/dev/shm/${PBS_JOBID}_rawSequence0
SHM_FILE1=/dev/shm/${PBS_JOBID}_rawSequence1
SHM_TMP_SORT=/dev/shm/${PBS_JOBID}_tempSort.bam

FNPIPE1=$PBS_SCRATCH_DIR/$PBS_JOBID/NAMED_PIPE1
FNPIPE2=$PBS_SCRATCH_DIR/$PBS_JOBID/NAMED_PIPE2
NP_SEQUENCE_0=$PBS_SCRATCH_DIR/$PBS_JOBID/NP_SEQUENCE_0
NP_SEQUENCE_1=$PBS_SCRATCH_DIR/$PBS_JOBID/NP_SEQUENCE_1
NP_SEQUENCE_2=$PBS_SCRATCH_DIR/$PBS_JOBID/NP_SEQUENCE_2
NP_SEQUENCE_3=$PBS_SCRATCH_DIR/$PBS_JOBID/NP_SEQUENCE_3
#MEM_FILE_0=/dev/shm/${PBS_JOBID}.memorybackedfile.0
#MEM_FILE_1=/dev/shm/${PBS_JOBID}.memorybackedfile.1
#NP_FLAGSTATS=$PBS_SCRATCH_DIR/$PBS_JOBID/NAMED_PIPE_FLAGSTATS
#NP_INDEX=$PBS_SCRATCH_DIR/$PBS_JOBID/NAMED_PIPE_FLAGSTATS
#NP_ISIZES=$PBS_SCRATCH_DIR/$PBS_JOBID/NAMED_PIPE_ISIZES

MBUFFER_2="mbuffer -q -m 16G -l /dev/null"

# the more efficient version pipes via local scratch dir (to avoid network problems)
mkfifo $FNPIPE1
mkfifo $FNPIPE2
mkfifo $NP_SEQUENCE_0
mkfifo $NP_SEQUENCE_1
mkfifo $NP_SEQUENCE_2
mkfifo $NP_SEQUENCE_3
#mkfifo $NP_FLAGSTATS
#mkfifo $NP_INDEX
#mkfifo $NP_ISIZES
echo created pipes

#TMP_FILE_INDEX=${FILENAME_SORTED_BAM}.bai
TMP_FILE_SAMPESORT=${FILENAME_SORTED_BAM}_tmp
TMP_FILE=${TMP_FILE_SAMPESORT}
TMP_FILE_SORT=/dev/shm/${PBS_JOBID}_sampeSort_sorted.temporary
#TMP_FILE_SORT=${PBS_SCRATCH_DIR}/${PBS_JOBID}/sorted.temporary
FLAGSTAT_TMP=${FILENAME_FLAGSTAT}_tmp
# error tracking!
BWA_LOG=${FILENAME_SORTED_BAM}_errlog_sampesort
#LOG_INDEX=${FILENAME_SORTED_BAM}_errlog_index


#TMP_FILE_PORT_SEQ0=${FILENAME_SEQ_1}_mbufferReceivingPort
#TMP_FILE_PORT_SEQ1=${FILENAME_SEQ_2}_mbufferReceivingPort
[[ -z ${useMBufferStreaming+x} ]] && useMBufferStreaming=false # If useMBufferStreaming is unset set it to false on default.

# TODO streaming is active but alignments did not run, what now?

lockfile ${LOCKFILE}

if [[ ${useMBufferStreaming} ]]
then
    lockfile ${DIR_TEMP}/~streamingBufferPortFinder.lock
    startPort=$((0x`echo ${PBS_JOBID} | md5sum | cut -b 1-8` % 16000 + 49152))
    # Get two free ports for listening for raw data
    hostAndPort0=`cat ${STREAM_BUFFER_PORTEXCHANGE_0}`
    hostAndPort1=`cat ${STREAM_BUFFER_PORTEXCHANGE_1}`
    java7 -jar ${TOOL_MEMORY_STREAMER} pull $hostAndPort0 - $DIR_TEMP/${PBS_JOBID}_memStreamer_0_pull | mbuffer -m 10G > $NP_SEQUENCE_0 &
    java7 -jar ${TOOL_MEMORY_STREAMER} pull $hostAndPort1 - $DIR_TEMP/${PBS_JOBID}_memStreamer_1_pull | mbuffer -m 10G > $NP_SEQUENCE_1 &
#    portSAIFile0=`${TOOLSDIR}/../roddy/findOpenPort.sh $startPort`
#    netcat -vv -l -p $portSAIFile0 | mbuffer -m 10G  > $NP_SEQUENCE_0 &
#    cat $NP_SEQUENCE_0 > $NP_SEQUENCE_2
#    netcat -vv -l -p $portSAIFile0 | mbuffer -m 10G  > $MEM_FILE_0 &
#    mbuffer -I $portSAIFile0 -m 10G  > $NP_SEQUENCE_0 &
#    portSAIFile1=`${TOOLSDIR}/../roddy/findOpenPort.sh $portSAIFile0`
#    netcat -vv -l -p $portSAIFile1 | mbuffer -m 10G  > $NP_SEQUENCE_1 &
#    cat $NP_SEQUENCE_1 > $NP_SEQUENCE_3
#    netcat -vv -l -p $portSAIFile1 | mbuffer -m 10G  > $MEM_FILE_1 &
#    mbuffer -I $portSAIFile1 -m 10G  > $NP_SEQUENCE_1 &
#         -P 0.001
    ip=`ip addr | grep "inet " | grep "19" | grep -v "0.0." | cut -d " " -f 6 | cut -d "/" -f 1`

#    echo $ip ${portSAIFile0} > ${STREAM_BUFFER_PORTEXCHANGE_0}
#    echo $ip ${portSAIFile1} > ${STREAM_BUFFER_PORTEXCHANGE_1}
#    echo `hostname` ${portSAIFile0} > ${STREAM_BUFFER_PORTEXCHANGE_0}
#    echo `hostname` ${portSAIFile1} > ${STREAM_BUFFER_PORTEXCHANGE_1}

#    sleep 60

    rm -rf ${DIR_TEMP}/~streamingBufferPortFinder.lock
fi

# Create the files and lock the process with lockfile.
# If you create the files first the lock will be available immediately!
#touch ~${TMP_FILE_PORT_SEQ0} ~${TMP_FILE_PORT_SEQ1}

RAW_SEQ=${RAW_SEQ_1}
source ${TOOL_COMMON_ALIGNMENT_SETTINGS_SCRIPT}

##Reset error and tmp variables. these are modified by bwaCommonAlignment...
#Samtools always attaches .bam, so we use a different output filename for samtools without .bam
#TEST_FILE=${RAW_SEQ_1}
#source ${TOOLSDIR}/../roddy.determineFileCompressor.sh
nice ${UNZIPTOOL} ${UNZIPTOOL_OPTIONS} $RAW_SEQ_1 ${TRIM_STEP} ${REVERSE_STEP} | mbuffer -m 24G -l /dev/null > $FNPIPE1 &
#cpProc0=$!
# Repeat some steps for the second named pipe
#TEST_FILE=${RAW_SEQ_2}
#source ${TOOLSDIR}/../roddy/determineFileCompressor.sh
nice ${UNZIPTOOL} ${UNZIPTOOL_OPTIONS} $RAW_SEQ_2 ${TRIM_STEP} ${REVERSE_STEP} | mbuffer -m 24G -l /dev/null > $FNPIPE2 &
#cpProc1=$!


## -r STR   read group header line such as `@RG\tID:foo\tSM:bar' [null]
#Samtools 0.1.19 uses multithreaded sorting => therefore it is hardcoded here.

#wait cpProc0
#wait cpProc1
SAMTOOLS_SORT_BINARY=samtools-0.1.19

#If it says here that the bamfile is not a valid one: Please check if you are using the convey version or the one with samtools view!

#${BWA_BINARY} sampe -P -T -t 16 ${BWA_SAMPESORT_OPTIONS} -r "@RG\tID:${ID}\tSM:${SM}\tLB:${LB}\tPL:ILLUMINA" ${INDEX_PREFIX} $NP_SEQUENCE_0 $NP_SEQUENCE_1 $SHM_FILE0 $SHM_FILE1 2> ${BWA_LOG} | $MBUFFER_2 | ${SAMTOOLS_BINARY} view -uSbh - | $MBUFFER_2 | ${SAMTOOLS_SORT_BINARY} sort -@ 16 -m ${SAMPESORT_MEMSIZE} -o - ${TMP_FILE_SORT} | tee ${TMP_FILE_SAMPESORT} | ${SAMTOOLS_SORT_BINARY} flagstat - > ${FLAGSTAT_TMP}
#${BWA_BINARY} sampe -X -Y -b -u -P -T -t 0 ${BWA_SAMPESORT_OPTIONS} -r "@RG\tID:${ID}\tSM:${SM}\tLB:${LB}\tPL:ILLUMINA" ${INDEX_PREFIX} $NP_SEQUENCE_0 $NP_SEQUENCE_1 $FNPIPE1 $FNPIPE2 2> ${BWA_LOG} | $MBUFFER_2 | ${SAMTOOLS_SORT_BINARY} sort -@ 8 -m ${SAMPESORT_MEMSIZE} -o - ${TMP_FILE_SORT} | tee $SHM_TMP_SORT | ${SAMTOOLS_SORT_BINARY} flagstat - > ${FLAGSTAT_TMP}
#${BWA_BINARY} sampe -b -u -T -t 16 ${BWA_SAMPESORT_OPTIONS} -r "@RG\tID:${ID}\tSM:${SM}\tLB:${LB}\tPL:ILLUMINA" ${INDEX_PREFIX} $NP_SEQUENCE_0 $NP_SEQUENCE_1 $FNPIPE1 $FNPIPE2 2> ${BWA_LOG} | $MBUFFER_2 | ${SAMTOOLS_SORT_BINARY} sort -@ 16 -m ${SAMPESORT_MEMSIZE} -o - ${TMP_FILE_SORT} | tee $SHM_TMP_SORT | ${SAMTOOLS_SORT_BINARY} flagstat - > ${FLAGSTAT_TMP}
mv $SHM_TMP_SORT ${TMP_FILE_SAMPESORT}
${BWA_BINARY} sampe -P -T -t 16 ${BWA_SAMPESORT_OPTIONS} -r "@RG\tID:${ID}\tSM:${SM}\tLB:${LB}\tPL:ILLUMINA" ${INDEX_PREFIX} $NP_SEQUENCE_0 $NP_SEQUENCE_1 $FNPIPE1 $FNPIPE2 2> ${BWA_LOG} | $MBUFFER_2 | ${SAMTOOLS_BINARY} view -uSbh - | $MBUFFER_2 | ${SAMTOOLS_SORT_BINARY} sort -@ 16 -m ${SAMPESORT_MEMSIZE} -o - ${TMP_FILE_SORT} | tee ${TMP_FILE_SAMPESORT} | ${SAMTOOLS_SORT_BINARY} flagstat - > ${FLAGSTAT_TMP}
#${BWA_BINARY} sampe -P -T -t 8 ${BWA_SAMPESORT_OPTIONS} -r "@RG\tID:${ID}\tSM:${SM}\tLB:${LB}\tPL:ILLUMINA" ${INDEX_PREFIX} $NP_SEQUENCE_0 $NP_SEQUENCE_1 $FNPIPE1 $FNPIPE2 2> ${BWA_LOG} | $MBUFFER_2 | ${SAMTOOLS_BINARY} view -uSbh - | $MBUFFER_2 | ${SAMTOOLS_SORT_BINARY} sort -@ 8 -m ${SAMPESORT_MEMSIZE} -o - ${TMP_FILE_SORT} | tee ${TMP_FILE_SAMPESORT} | ${SAMTOOLS_SORT_BINARY} flagstat - > ${FLAGSTAT_TMP}
#${SAMTOOLS_BINARY} index ${NP_INDEX}
errorString="There was a non-zero exit code in the bwa sampe - samtools sort pipeline; exiting..." 

#rm $MEM_FILE_0
#rm $MEM_FILE_1


useMBufferStreaming=false # Disable mbuffer streaming for the error checking. We produced an outfile and want to process it.
source ${TOOL_BWA_ERROR_CHECKING_SCRIPT}


mv ${TMP_FILE_SAMPESORT} ${FILENAME_SORTED_BAM}

rm $FNPIPE1
rm $FNPIPE2
rm $SHM_TMP_SORT
#rm $SHM_FILE0
#rm $SHM_FILE1