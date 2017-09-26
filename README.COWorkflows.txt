== Description

Initial import for the COWorkflows (Computational Oncology Workflows Base Plugin for Roddy) repo into Phabricator.

== Run flags / switches

Switch                                    Default Description
isNoControlWorkflow                       false   Set to true to allow this workflow to work without a control bam file.
workflowSupportsMultiTumorSamples         false   Allow the workflow to run with several tumor bam files. This is done with a for loop:
                                                    BasicBamFile bamControlMerged = initialBamFiles[0];
                                                    for (int i = 1; i < initialBamFiles.length; i++) {
                                                        result &= execute(context, bamControlMerged, initialBamFiles[i]);
                                                    }

== Changelist

- Introduce flag isNoControlWorkflow

* Version update to 1.1.59

- Library support for WGBS (FileStage)

* Version update to 1.1.39

- JDK Version 1.8
- Groovy Version 2.4
- Roddy API Version 2.3

* Version update to 1.1.23

- Sambamba support in QCWorkflows and changes required here.

* Version update to 1.1.20

- Reverted references to FileSystemAccessProvider back to FileSystemInfoProvider to ensure compatibility of current COWorkflows to older version of Roddy (e.g. 2.2.66 currently used in OTP).
- Changed COProjectsRuntimeService so, that a library name can be set when getting the sample or sequence directory.
- Function added to get the read groups of a bam file from the bam header. Small fix in the SQ count
- Add readme file.
