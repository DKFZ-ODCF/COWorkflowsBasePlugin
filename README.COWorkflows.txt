== Description

The COWorkflowsBasePlugin (Computational Oncology Workflows Base Plugin for Roddy) provides some general classes and framework for some of the other
Roddy plugins. This includes both, JVM-based code (Java, Groovy) as well as command line tools used in cluster jobs.

== Run flags / switches

Switch                                    Default Description
isNoControlWorkflow                       false   Set to true to allow this workflow to work without a control bam file.
workflowSupportsMultiTumorSamples         false   Allow the workflow to run with several tumor bam files. This is done with a for loop:
                                                    BasicBamFile bamControlMerged = initialBamFiles[0];
                                                    for (int i = 1; i < initialBamFiles.length; i++) {
                                                        result &= execute(context, bamControlMerged, initialBamFiles[i]);
                                                    }

== Changelist

* Version update to 1.0.4

- Roddy 3.1 has changed unit test framework classes. COWFBP unit tests require Roddy 3.1.
- contributors
- changed internal column names for metadata table
- refactoring: moved a lot of code to new MetadataAccessor class (will be facade for more elaborate metadata backend), no effect on client code (that we know of) is expected)

* Version update to 1.0.3

- bugfixes
- improved error detection and reporting
- more default merged-BAM suffixes (to ease configuration-free start)

* Version update to 1.0.2

- Minor release.

* Version update to 1.0.1

- Roddy 3.0 is required
- dead code removal
- added bamIsComplete.sh that check for BAM termination sequences
- re-added some scripts that were incorrectly removed when publishing the workflow to Github
- refactorings and smaller bugfixes

* Version update to 1.0.0

- Introduce flag isNoControlWorkflow

== Changelist of COWorkflowsPlugin

* Plugin rename to COWorkflowsBasePlugin

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
