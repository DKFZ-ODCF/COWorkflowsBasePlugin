#!/bin/bash

#PBS -l walltime=50:00:00
#PBS -l nodes=1:ppn=8
#PBS -l mem=3600m
#PBS -m a
#PBS -S /bin/bash
set -o pipefail

TMP_FILE=${FILENAME_ALIGNMENT}_temp
# error tracking because bwa never exits anything but 0
FILENAME_BWA_LOG=${DIR_TEMP}/`basename ${FILENAME_ALIGNMENT}`_errlog

source ${CONFIG_FILE}
source ${TOOL_COMMON_ALIGNMENT_SETTINGS_SCRIPT}

cmd=""
bwaBinary="${BWA_BINARY}"
alnThreadOptions="8"
useConvey=false
#useAdaptorTrimming=${USEADAPTORTRIMMING-false}
[[ -z ${useMBufferStreaming+x} ]] && useMBufferStreaming=false # If useMBufferStreaming is unset set it to false on default.

# If things go wrong with this, one could use which with the convey binary!
if [[ ${PBS_QUEUE} == "convey" ]]
then
    bwaBinary=${BWA_ACCELERATED_BINARY}
    useConvey=true
    alnThreadOptions="12"
fi

# Check the bwa version
sh ${TOOL_CHECK_BWA_AND_INDEX_VERSIONS} ${bwaBinary} ${INDEX_PREFIX}
[[ $? -ne 0 ]] && echo "Problems when checking bwa binary against the index prefix." && exit -5
# [[ ${useReverseStepping} ]] &&

targetCall="> ${TMP_FILE}"
removeLockCommand=""

if [[ $useMBufferStreaming ]]
then
    lockfile ${LOCKFILE} # Wait lock

    # We want to use mbuffer as some sort of interprocess communication system.
    # The settings for the receiver are stored in the following file:
    portString=`cat ${STREAM_BUFFER_PORTEXCHANGE}`
    host=`echo $portString | cut -d " " -f 1`
    port=`echo $portString | cut -d " " -f 2`

#    hostIP=`ping $host | head -n 1 | cut -d "(" -f 2 | cut -d ")" -f 1`
#    mbufferCommand="mbuffer -O ${host}:${port}"
    mbufferCommand="java7 -jar ${TOOL_MEMORY_STREAMER} push $host $port $DIR_TEMP/${RODDY_JOBID}_memStreamer_push" # Mbuffer does not really work... Dunno why, so don't use it to have a pre buffer
#    mbufferCommand="mbuffer -m 10G | java7 -jar ${TOOL_MEMORY_STREAMER} push $host $port push"
#    mbufferCommand="mbuffer -m 10G | netcat -vv $host $port"
    targetCall="| ${mbufferCommand}"
fi

#[[ ${useConvey} = true ]] && BWA_ALIGNMENT_OPTIONS="${BWA_ALIGNMENT_OPTIONS} -X"
baseBWACall="${bwaBinary} aln -t ${alnThreadOptions} ${BWA_ALIGNMENT_OPTIONS} ${illuminaString} ${INDEX_PREFIX}"

if [[ ${useConvey} = true && ${useReverseStepping} = false ]]
then
    cmd="${baseBWACall} ${RAW_SEQ} ${targetCall} 2> ${FILENAME_BWA_LOG}"
else
    # In this case either the convey with reverse stepping is used or the default queue with or without reverse stepping.
    cmd="${UNZIPTOOL} ${UNZIPTOOL_OPTIONS} ${RAW_SEQ} ${TRIM_STEP} ${REVERSE_STEP} | mbuffer -q -m 100M -l /dev/null | ${baseBWACall} - ${targetCall} 2> ${FILENAME_BWA_LOG}"
fi
eval "nice ${removeLockCommand} $cmd" # execute the command

errorString="There was a non-zero exit code in bwa aln; exiting..." 
source ${TOOL_BWA_ERROR_CHECKING_SCRIPT}

[[ $useMBufferStreaming == false ]] && mv ${TMP_FILE} ${FILENAME_ALIGNMENT}
