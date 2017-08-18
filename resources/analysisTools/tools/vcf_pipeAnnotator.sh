#!/bin/bash

set -x
set -o pipefail

# Check if input file is gzipped or uncompressed.
filenameVCF=`dirname ${FILENAME_VCF}`/`basename ${FILENAME_VCF} .gz`
[[ -f ${FILENAME_CHECKPOINT} ]] && rm ${FILENAME_CHECKPOINT}

# create a bunch of pipes using Matthias Schlesners overlapper for multiple annotation files and do a system call with this
pipe=`perl ${TOOL_CREATEPIPES} ${FILENAME_VCF} ${CONFIG_FILE} ${TOOL_ANNOTATE_VCF_FILE} ${PIPENAME} ${TABIX_BINARY}`
pipeTemp=${FILENAME_VCF}.${PIPENAME}.tmp
if [[ "$?" != 0 ]] || [[ -z "$pipe" ]]; then echo "problem when generating pipe: $PIPENAME. Exiting..."; exit 2; fi

eval ${pipe} > ${pipeTemp}

[[ "$?" != 0 ]] && echo "There was a non-zero exit code in the $PIPENAME pipe; temp file ${pipeTemp} not moved back" && exit 2

[[ -f "${filenameVCF}" ]] && rm ${filenameVCF} ${filenameVCF}.tbi
[[ -f "${FILENAME_VCF}" ]] && rm ${FILENAME_VCF} ${FILENAME_VCF}.tbi

mv ${pipeTemp} ${filenameVCF}
bgzip -f ${filenameVCF} && tabix -f -p vcf ${FILENAME_VCF}

[[ ! $? -eq 0 ]] && echo "Error in bgzip" && exit 5

touch ${FILENAME_CHECKPOINT}