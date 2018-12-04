# Description

The COWorkflowsBasePlugin (Computational Oncology Workflows Base Plugin for Roddy) provides some general classes and framework for some of the other
Roddy plugins. This includes both, JVM-based code (Java, Groovy) as well as command line tools used in cluster jobs.

## Run flags / switches

### Hints

#### Alignment folder

The alignment folder is referenced several times. For the plugin to work, it is currently necessary to have a folder
for your dataset like e.g.:
```bash
/tmp/[dataset_id]
```
Inside this, you will need to create the alignment subfolder:
```bash
/tmp/[dataset_id]/alignment
```
And inside this, you will need to place or link your merged bams, e.g.:
```bash
/tmp/[dataset_id]/alignment/[sample_id]_[dataset_id]_merged.rmdup.bam
/tmp/[dataset_id]/alignment/[sample_id]_[dataset_id]_merged.rmdup.bam.bai
```
So whenever we speak of the alignment folder, it is basically the described structure.
You can change the alignment folder by overriding in your xml:
```xml
<cvalue name='alignmentOutputDirectory' value='alignment' type="path"/>
```

### For classes extending WorkflowUsingMergedBams: 

|Switch                                   | Default | Description |
|-----------------------------------------|---------|-------------|
|isNoControlWorkflow                      |false    |Set to true to allow this workflow to work without a control bam file. |
|workflowSupportsMultiTumorSamples        |false    |Allow the workflow to run with several tumor bam files. This is done with a for loop (see code documentation in WorkflowUsingMergedBam) |

### For sample extraction from filenames:


To extract samples from filenames, multiple methods exist or are planned. You can control
the workflows behaviour with the variable "selectSampleExtractionMethod".

Valid values with their control variables are:
version
|selectSampleExtractionMethod             |version_2| Alternative values are "version_1" |

#### "version_1"
This one is very simple and just splits the filename. Afterwards, it takes the first splitted value
and uses it as the sample name. Further control is possible with:

|Switch                                   | Default | Description |
|-----------------------------------------|---------|-------------|
|enforceAtomicSampleName||

Please take a close look at the file SampleFromFilenameExtractorVersionOneTest (to be found [here](https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/blob/master/test/src/de/dkfz/b080/co/knowledge/metadata/sampleextractorstrategies/SampleFromFilenameExtractorVersionOneTest.groovy)) to 
see a table of filenames and expected samples. 


#### "version_2"
|Switch                                   | Default | Description |
|---|---|---|
|matchExactSampleName                     |false    | If set, the sample will be extracted like they are set in the config. This is compatible with allowSampleTerminationWithIndex. |
|allowSampleTerminationWithIndex          |true     | Allow recognition of trailing integer numbers for sample names, e.g. tumor_02, if tumor is set. |
|useLowerCaseFilenamesForSampleExtraction |true     | The switch will tell the method to work on lowercase filenames.|

Please take a close look at the file SampleFromFilenameExtractorVersionTwoTest (to be found [here](https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/blob/master/test/src/de/dkfz/b080/co/knowledge/metadata/sampleextractorstrategies/SampleFromFilenameExtractorVersionTwoTest.groovy)). There is 
a large test case *"Version_2: Extract sample name from BAM basename"*, which features a table with input, switches and
expected output.

#### "regex" (planned)
Not implemented, but planned.

### For sample extraction from the alignment directory

|Switch                            | Default | Description |
|----------------------------------|---------|-------------|
|extractSamplesFromOutputFiles     | false   | If this is true and samples are neither passed by metadata table, configuration or sample list, samples are extracted from files in the alignment folder.            |
|extractSampleNameOnlyFromBamFiles | false   | By default, the method will search for samples in all files in the alignment directory. With this switch, you can restrict it to BAM files.            |

## Changelist

  * Version update to 1.3.0
    - Change tests to extend the RoddyTestSpec class in Roddy 3.4.0
    - Changed tests in WorkflowUsingMergedBamsSpec to use flags instead of getContext() method.
      * The ContextResource @Rule did not work anymore for the tests.
      * Makes it easier to read, flags are boolean values now.
      * Context and file objects are now created in the test methods.
    - Updated copyright notes.
    - Added compareTo(), equals() and hashCode() to Sample class.
    - In COMetadataAccessor class:
      * Added the 'extractSampleNameOnlyFromBamFiles' flag which tells extractSamplesFromOutputFiles() to ignore non BAM files.
        * Added variable and description to the xml.
      * Minimized the code in extractSamplesFromFilenames() by reusing existing code. 
    - Add another sample from filename extraction method and move code from the COMetadataAccessor
      to custom strategy classes (SampleFromFilenameExtractionVersionOne/Two)  
        * Add variable and descriptions for the new (and old) method to the xml.
        * Add a lot of tests for the new method.
    - Add the COConfigSpec test class.
    - Rename the SampleTest class to SampleSpec and make it extend RoddyTestSpec.
  * Version updates until 1.2.0
    
    ***Description Missing...***
    
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

### Changelist of COWorkflowsPlugin

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
    - Reverted references to FileSystemAccessProvider back to FileSystemInfoProvider to ensure compatibility of current COWorkflows to older version of Roddy (e.g.  2.2.66 currently used in OTP).
    - Changed COProjectsRuntimeService so, that a library name can be set when getting the sample or sequence directory.
    - Function added to get the read groups of a bam file from the bam header. Small fix in the SQ count
    - Add readme file.
