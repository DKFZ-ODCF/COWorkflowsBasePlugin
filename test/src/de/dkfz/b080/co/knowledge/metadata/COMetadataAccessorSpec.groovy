/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */
package de.dkfz.b080.co.knowledge.metadata

import de.dkfz.b080.co.common.BasicCOProjectsRuntimeService
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.RoddyTestSpec
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext
import spock.lang.Shared

import static de.dkfz.b080.co.common.COConstants.CVALUE_ALLOW_SAMPLE_TERMINATION_WITH_INDEX
import static de.dkfz.b080.co.common.COConstants.CVALUE_MATCH_EXACT_SAMPLE_NAMES
import static de.dkfz.b080.co.common.COConstants.CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES
import static de.dkfz.b080.co.common.COConstants.CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES
import static de.dkfz.b080.co.common.COConstants.CVALUE_SELECT_SAMPLE_EXTRACTION_METHOD
import static de.dkfz.b080.co.common.COConstants.CVALUE_USE_LOWER_CASE_FILENAMES_FOR_SAMPLE_EXTRACTION

class COMetadataAccessorSpec extends RoddyTestSpec {

    @Shared
    static final ExecutionContext context = contextResource.createSimpleContext(COMetadataAccessorSpec)

    def "GrepPathElementFromFilenames"() {

        when:
        def files = [new File("/a/b//c1/sampleName1/bla/bla/bla"),
                     new File("/a/b/c2/sampleName1/bla/bla/blub"),
                     new File("/a/b/c2/sampleName2/bla/blub/")]
        def pattern = "/a/b/c/\${sample}"

        then:
        assert COMetadataAccessor.grepPathElementFromFilenames(
                pattern,
                '${sample}',
                files).collect { it.x }.equals(["sampleName1", "sampleName1", "sampleName2"])
        assert COMetadataAccessor.grepPathElementFromFilenames(
                pattern,
                '${sample}',
                files).collect { it.y }.equals(["/a/b/c1/sampleName1", "/a/b/c2/sampleName1", "/a/b/c2/sampleName2"].collect { new File(it) })
    }

    def "GrepPathElementFromFilenames_tooShortPath"() {

        when:
        def file = new File("/a/b/c/")
        def pattern = "/a/b/c/\${sample}"

        then:
        try {
            COMetadataAccessor.grepPathElementFromFilenames(pattern, '${sample}', [file] as List<File>)
        } catch (RuntimeException e) {
            assert e.message.startsWith("Path to file")
        }
    }

    def "Version_1: Extract sample name from BAM basename"(String filename, String possibleControlSampleNamePrefixes, String possibleTumorSampleNamePrefixes, boolean useAtomicSampleNames, boolean allowSampleTerminationWithIndex, String resultSample) {

    }

    def "Version_2: Extract sample name from BAM basename"(String filename, String possibleControlSampleNamePrefixes, String possibleTumorSampleNamePrefixes, boolean matchExactSampleNames, boolean allowSampleTerminationWithIndex, boolean useAllLowerCaseSampleNames, String expectedSample) {

        when:
        context.configurationValues << new ConfigurationValue(CVALUE_SELECT_SAMPLE_EXTRACTION_METHOD, "version_2")
        context.configurationValues << new ConfigurationValue(CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES, possibleControlSampleNamePrefixes, ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY)
        context.configurationValues << new ConfigurationValue(CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES, possibleTumorSampleNamePrefixes, ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY)
        context.configurationValues << new ConfigurationValue(CVALUE_MATCH_EXACT_SAMPLE_NAMES, matchExactSampleNames)
        context.configurationValues << new ConfigurationValue(CVALUE_ALLOW_SAMPLE_TERMINATION_WITH_INDEX, allowSampleTerminationWithIndex)
        context.configurationValues << new ConfigurationValue(CVALUE_USE_LOWER_CASE_FILENAMES_FOR_SAMPLE_EXTRACTION, useAllLowerCaseSampleNames)

        def accessor = new COMetadataAccessor(new BasicCOProjectsRuntimeService())
        def file = new File("/tmp/", filename)
        def extractedSample = accessor.extractSampleNameFromBamBasename(file, context)

        then:
        // The filename has no actual meaning. It is solely for debugging
        "$filename:$extractedSample" == "$filename:$expectedSample"

        where:
        filename                          | possibleControlSampleNamePrefixes | possibleTumorSampleNamePrefixes | matchExactSampleNames | allowSampleTerminationWithIndex | useAllLowerCaseSampleNames | expectedSample
        "control_TEST000_mdup.bam"        | "( cont )"                        | "( tumor )"                     | false                 | true                            | true                       | "control"
        "control_TEST001_mdup.bam"        | "( cont )"                        | "( tumor )"                     | true                  | true                            | true                       | null
        "control_TEST002_mdup.bam"        | "( control_ )"                    | "( tumor )"                     | false                 | true                            | true                       | "control"
        "control_abc_TEST003_mdup.bam"    | "( control_abc )"                 | "( tumor )"                     | false                 | true                            | true                       | "control_abc"
        "control_abc_TEST004_mdup.bam"    | "( control control_abc )"         | "( tumor )"                     | false                 | true                            | true                       | "control_abc"
        "control_abc_0_TEST005_mdup.bam"  | "( control control_abc_0 )"       | "( tumor )"                     | false                 | true                            | true                       | "control_abc_0"
        "control_abc_01_TEST006_mdup.bam" | "( control control_abc_0 )"       | "( tumor )"                     | false                 | true                            | true                       | "control_abc_01"
        "control_abc_01_TEST007_mdup.bam" | "( control control_abc_0 )"       | "( tumor )"                     | true                  | true                            | true                       | "control"
        "control_02_TEST008_mdup.bam"     | "( control_02 )"                  | "( tumor )"                     | false                 | true                            | true                       | "control_02"
        "Control_02_TEST009_mdup.bam"     | "( control_02 )"                  | "( )"                           | false                 | true                            | true                       | "control_02"
        "CONTROL_02_TEST010_mdup.bam"     | "( control_02 )"                  | "( )"                           | false                 | true                            | true                       | "control_02"
        "control_02_TEST011b_mdup.bam"    | "( control )"                     | "( tumor )"                     | true                  | true                            | true                       | "control_02"
        "Control_02_TEST012b_mdup.bam"    | "( control )"                     | "( )"                           | true                  | true                            | true                       | "control_02"
        "CONTROL_02_TEST013b_mdup.bam"    | "( control )"                     | "( )"                           | true                  | true                            | true                       | "control_02"
        "control_02_TEST011c_mdup.bam"    | "( control_02 )"                  | "( tumor )"                     | true                  | true                            | false                      | "control_02"
        "Control_02_TEST012c_mdup.bam"    | "( Control_02 )"                  | "( )"                           | true                  | true                            | false                      | "Control_02"
        "CONTROL_02_TEST013c_mdup.bam"    | "( CONTROL_02 )"                  | "( )"                           | true                  | true                            | false                      | "CONTROL_02"

        // In the following cases, the sample extraction will "Fail". But this is not a fault of the code, we just do not have enough |information to
        // extract more. Maybe later, this could be extended with regex etc. But for now it is not possible.
        "control_abc_02_TEST014_mdup.bam" | "( control )"                     | "( )"                           | false                 | true                            | true                       | "control"
        "control_abc_02_TEST015_mdup.bam" | "( control )"                     | "( )"                           | true                  | true                            | true                       | "control"

        // Fallback. If no sample was found ( could be matched against the list ), the first part of the filename will be used. This
        // way, we will keep the old process.
        "control_abc_01_TEST016_mdup.bam" | "( control_abc_0 )"               | "( )"                           | true                  | true                            | true                       | null
        "control_abc_01_TEST016_mdup.bam" | "( control_abc_0 )"               | "( )"                           | false                 | true                            | true                       | "control_abc_01"

        "tumor_02_TEST019a_mdup.bam"      | "( control )"                     | "( tumor )"                     | false                 | false                           | true                       | "tumor"
        "tumor_02_TEST019b_mdup.bam"      | "( control )"                     | "( tumor )"                     | true                  | true                            | true                       | "tumor_02"
        "tumor02_TEST019c_mdup.bam"       | "( control )"                     | "( tumor )"                     | true                  | true                            | true                       | null
        "tumor02_TEST019c_mdup.bam"       | "( control )"                     | "( tumor )"                     | false                 | true                            | true                       | "tumor02"
    }

    def "test extract samples from filenames"(List<String> filenames, List<String> expected) {
        when:
        def accessor = new COMetadataAccessor(new BasicCOProjectsRuntimeService())
        def context = contextResource.createSimpleContext(COMetadataAccessorSpec)
        context.configurationValues << new ConfigurationValue(CVALUE_SELECT_SAMPLE_EXTRACTION_METHOD, "version_2")
        context.configurationValues << new ConfigurationValue(CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES, "( control )", ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY)
        context.configurationValues << new ConfigurationValue(CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES, "( tumor )", ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY)
        context.configurationValues << new ConfigurationValue(CVALUE_MATCH_EXACT_SAMPLE_NAMES, true)
        context.configurationValues << new ConfigurationValue(CVALUE_ALLOW_SAMPLE_TERMINATION_WITH_INDEX, true)
        context.configurationValues << new ConfigurationValue(CVALUE_USE_LOWER_CASE_FILENAMES_FOR_SAMPLE_EXTRACTION, true)
        def files = filenames.collect { new File(it) } as List<File>
        def samples = accessor.extractSamplesFromFilenames(files, context)
        def expectedSamples = expected.collect { new Sample(context, it) }

        then:
        samples == expectedSamples

        where:
        filenames                                                                        | expected
        ["/tmp/control_pid_merged.bam.rmdup.bam"]                                        | ["control"]
        ["/tmp/control_02_pid_merged.bam.rmdup.bam"]                                     | ["control_02"]
        ["/tmp/tumor_pid_merged.bam.rmdup.bam"]                                          | ["tumor"]
        ["/tmp/tumor_pid_merged.bam.rmdup.bam", "/tmp/control_pid_merged.bam.rmdup.bam"] | ["tumor", "control"]

    }
}
