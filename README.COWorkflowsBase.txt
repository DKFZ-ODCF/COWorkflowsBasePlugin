== Description

Basically a package with some standard files and tool classes which are useful for 
working with bam files.

== Run flags / switches

Switch                                    Default Description
isNoControlWorkflow                       false   Set to true to allow this workflow to work without a control bam file.
workflowSupportsMultiTumorSamples         false   Allow the workflow to run with several tumor bam files. This is done with a for loop:
                                                    BasicBamFile bamControlMerged = initialBamFiles[0];
                                                    for (int i = 1; i < initialBamFiles.length; i++) {
                                                        result &= execute(context, bamControlMerged, initialBamFiles[i]);
                                                    }

== Changelist

