/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */
package de.dkfz.b080.co.knowledge.metadata

import de.dkfz.b080.co.common.BasicCOProjectsRuntimeService
import de.dkfz.b080.co.common.COConstants
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ContextResource
import de.dkfz.roddy.core.ExecutionContext
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

class COMetadataAccessorSpec extends Specification {


    @ClassRule
    static final ContextResource contextResource = new ContextResource() {
        {
            before()
        }
    }

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

    def "Version_2: Extract sample name from BAM basename"(String filename, String possibleControlSampleNamePrefixes, String possibleTumorSampleNamePrefixes, boolean matchExactSampleNames, boolean allowSampleTerminationWithIndex, String resultSample) {

        when:
        context.configurationValues.add(new ConfigurationValue(COConstants.CVALUE_SELECT_SAMPLE_EXTRACTION_METHOD, "version_2", ConfigurationConstants.CVALUE_TYPE_STRING))
        context.configurationValues.add(new ConfigurationValue(COConstants.CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES, possibleControlSampleNamePrefixes, ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY))
        context.configurationValues.add(new ConfigurationValue(COConstants.CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES, possibleTumorSampleNamePrefixes, ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY))
        context.configurationValues.add(new ConfigurationValue(COConstants.CVALUE_MATCH_EXACT_SAMPLE_NAMES, matchExactSampleNames, ConfigurationConstants.CVALUE_TYPE_BOOLEAN))
        context.configurationValues.add(new ConfigurationValue(COConstants.CVALUE_ALLOW_SAMPLE_TERMINATION_WITH_INDEX, allowSampleTerminationWithIndex.toString(), ConfigurationConstants.CVALUE_TYPE_BOOLEAN))

        then:
        (new COMetadataAccessor(new BasicCOProjectsRuntimeService()).extractSampleNameFromBamBasename(new File("/tmp/", filename), context)) == resultSample

        where:
        filename                          | possibleControlSampleNamePrefixes | possibleTumorSampleNamePrefixes | matchExactSampleNames | allowSampleTerminationWithIndex | resultSample
        "control_TEST000_mdup.bam"        | "( cont )"                        | "( tumor )"                     | false                 | true                            | "control"
        "control_TEST001_mdup.bam"        | "( cont )"                        | "( tumor )"                     | true                  | true                            | null
        "control_TEST002_mdup.bam"        | "( control_ )"                    | "( tumor )"                     | false                 | true                            | "control"
        "control_abc_TEST003_mdup.bam"    | "( control_abc )"                 | "( tumor )"                     | false                 | true                            | "control_abc"
        "control_abc_TEST004_mdup.bam"    | "( control control_abc )"         | "( tumor )"                     | false                 | true                            | "control_abc"
        "control_abc_0_TEST005_mdup.bam"  | "( control control_abc_0 )"       | "( tumor )"                     | false                 | true                            | "control_abc_0"
        "control_abc_01_TEST006_mdup.bam" | "( control control_abc_0 )"       | "( tumor )"                     | false                 | true                            | "control_abc_01"
        "control_abc_01_TEST007_mdup.bam" | "( control control_abc_0 )"       | "( tumor )"                     | true                  | true                            | "control"
        "control_02_TEST008_mdup.bam"     | "( control_02 )"                  | "( tumor )"                     | false                 | true                            | "control_02"
        "Control_02_TEST009_mdup.bam"     | "( control_02 )"                  | "( )"                           | false                 | true                            | "Control_02"
        "CONTROL_02_TEST010_mdup.bam"     | "( control_02 )"                  | "( )"                           | false                 | true                            | "CONTROL_02"
        "control_02_TEST008_mdup.bam"     | "( control_02 )"                  | "( tumor )"                     | true                  | true                            | null
        "Control_02_TEST009_mdup.bam"     | "( control_02 )"                  | "( )"                           | true                  | true                            | null
        "CONTROL_02_TEST010_mdup.bam"     | "( control_02 )"                  | "( )"                           | true                  | true                            | null

        // In the following cases, the sample extraction will "Fail". But this is not a fault of the code, we just do not have enough |information to
        // extract more. Maybe later, this could be extended with regex etc. But for now it is not possible.
        "control_abc_02_TEST011_mdup.bam" | "( control )"                     | "( )"                           | false                 | true                            | "control"
        "control_abc_02_TEST012_mdup.bam" | "( control )"                     | "( )"                           | true                  | true                            | "control"

        // Fallback. If no sample was found ( could be matched against the list ), the first part of the filename will be used. This
        // way, we will keep the old process.
        "control_abc_01_TEST013_mdup.bam" | "( control_abc_0 )"               | "( )"                           | true                  | true                            | "control"
        "xeno_TEST014_mdup.bam"           | "( control )"                     | "( )"                           | false                 | true                            | "xeno"
        "xeno_TEST015_mdup.bam"           | "( control )"                     | "( )"                           | true                  | true                            | "xeno"

        "tumor_02_TEST015_mdup.bam"       | "( control )"                     | "( tumor )"                     | false                 | false                           | "tumor"
        "tumor_02_TEST015_mdup.bam"       | "( control )"                     | "( tumor )"                     | true                  | true                            | "tumor_02"
        "tumor02_TEST015_mdup.bam"        | "( control )"                     | "( tumor )"                     | true                  | true                            | "tumor02"
    }

}
