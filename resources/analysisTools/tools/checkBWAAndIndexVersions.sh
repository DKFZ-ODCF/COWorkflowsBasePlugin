#!/bin/sh
#
# Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
#

# This script tries to lookup the bwa version and to determine the used index version.
# If both fits everything is fine. In any other case it throws an error.


# Lookup the parameters

set -xvu

[ $# -ne 2 ] && echo "Parameter count is wrong! You have to provide the parameters for the bwa binary and the bwa index." && exit -5

# Currently we only use bwa 0.5 or 0.6, the test is reduced to the second part of the version number!
bwaBinaryVersion=`$1 2>&1 | grep Version | cut -d " " -f 2 | cut -d "." -f 2`
# bwa 7 uses the index of bwa 6
[[ "$bwaBinaryVersion" == 7 ]] && bwaBinaryVersion=6 && echo "bwa 7 uses the index of 6, so bwa 6 is taken to match the index version"

# As we only have two bwa versions the index can be either for 5 or for 6!
bwaIndexDirectory=`dirname $2`
foundIndexVersion=6 # Default to 6
directoryContent=`ls $bwaIndexDirectory/*.rbwt`
[[ 0 -eq $? ]] && foundIndexVersion=5  # Check if files for version 5 exist

# Compare bwa version to index version
[[ "$bwaBinaryVersion" == "$foundIndexVersion" ]] && exit 0 # Both are equal

echo "The bwa version and it's index version do not fit!"
exit -5
