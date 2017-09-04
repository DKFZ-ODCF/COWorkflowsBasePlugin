#!/usr/bin/env bash
# Philip R. Kensche
# Loosely based on a Groovy script written by Manuel Prinz.

# Report FAIL or OK to standard output and details to standard error.

set -ue

fileToCheck="${1:?No BAM file to check is given}"

# End of file marker as defined by the SAM Format Specification.
#
# @see <a href="http://samtools.github.io/hts-specs/SAMv1.pdf">Sequence Alignment/Map Format Specification</a>,
# section 4.1.2.

declare -a expectedBytes=( "0x1f" "0x8b" "0x08" "0x04" "0x00" "0x00" "0x00" "0x00" "0x00" "0xff" "0x06" "0x00" "0x42" "0x43" \
                           "0x02" "0x00" "0x1b" "0x00" "0x03" "0x00" "0x00" "0x00" "0x00" "0x00" "0x00" "0x00" "0x00" "0x00")

declare -a observedBytes=( $(tail --bytes 28 "$fileToCheck" | od -A none -t x1) )

if [[ ${#observedBytes[@]} -ne ${#expectedBytes[@]} ]]; then
    echo "FAIL"
    echo "Could not read ${#expectedBytes[@]} bytes from input file '$fileToCheck'." > /dev/stderr
    exit 2
fi

for i in $( seq 0 $( expr ${#expectedBytes[@]} - 1 ) ); do
    if [[ "0x${observedBytes[$i]}" != "${expectedBytes[$i]}" ]]; then
        echo "FAIL"
        echo "Byte mismatch in ${#expectedBytes[@]} byte trailer of '$fileToCheck' with expected byte sequence for BAMs." > /dev/stderr
        exit 1
    fi
done

echo "OK"
exit 0