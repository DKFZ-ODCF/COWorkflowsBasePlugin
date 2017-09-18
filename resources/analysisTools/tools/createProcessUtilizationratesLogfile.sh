#!/bin/bash

mkdir -p ${DIR_TEMP}

topLog=$DIR_EXECUTION/cpu_timings_$RODDY_JOBID.log
session=`ps | grep bash | cut -d " " -f 1`
[[ -z "$session" ]] && session=`ps | grep bash | cut -d " " -f 2`
[[ -z "$session" ]] && session=`ps | grep bash | cut -d " " -f 3`
[[ -z "$session" ]] && session=`ps | grep bash | cut -d " " -f 4`
[[ -z "$session" ]] && ps && exit -20

topLog=$DIR_EXECUTION/cpu_timings_$RODDY_JOBID.log
FILE_BENCHMARK_TMP=$DIR_TEMP/$RODDY_JOBID.benchmark.tmp
FILE_BENCHMARK_STAYALIVE=$DIR_TEMP/$RODDY_JOBID.benchmark.stayalive
touch $FILE_BENCHMARK_STAYALIVE

setX=false
setV=false
[[ "$-" == *x* ]] && setX=true && set +x
[[ "$-" == *v* ]] && setV=true && set +v

while [[ -f $FILE_BENCHMARK_STAYALIVE ]]
do

    # Select jobs within this session
    sessionJobs=`ps -s $session | tail -n +2 - | grep -v -E "tail|grep|cut|tr" | cut -b 1-5 | tr -d " "`

    pidPattern=""
    count=0
    for sj in $sessionJobs #Filter out non numbers using an arithmetic expression
    do
        if [[ "$sj" -eq "$sj" ]]
        then
            if [[ $count -ge 20 && `expr $count % 20` -eq 0 ]]
            then
                echo -e "" >> $FILE_BENCHMARK_TMP
            fi
            echo -n -e ",$sj" >> $FILE_BENCHMARK_TMP
            count=$((count+1))
        fi
    done

    pidPatterns=`cat $FILE_BENCHMARK_TMP`
    unlink $FILE_BENCHMARK_TMP

    echo "top - "`date +"%H:%M:%S"` >> $topLog

    for pidPattern in $pidPatterns
    do
    #    [[ $count -gt 20 ]] && echo "problem, too many jobs!"
    # Select jobs with top, print that to the top log file.

        top -b -n 1 -p ${pidPattern:1} | tail -n +8 - >> $topLog
    done

done &

[[ "$setX" == true ]] && set -x
[[ "$setV" == true ]] && set -v

utilizationLogProcess=$!
