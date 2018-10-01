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

    def "Extract sample name from BAM basename"(String filename, String possibleControlSampleNamePrefixes, String possibleTumorSampleNamePrefixes, boolean searchWithSeparator, String resultSample) {

        when:
        context.configurationValues.add(new ConfigurationValue(COConstants.CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES, possibleControlSampleNamePrefixes, ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY))
        context.configurationValues.add(new ConfigurationValue(COConstants.CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES, possibleTumorSampleNamePrefixes, ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY))
        context.configurationValues.add(new ConfigurationValue(COConstants.CVALUE_SEARCH_MERGEDBAM_WITH_SEPARATOR, searchWithSeparator.toString(), ConfigurationConstants.CVALUE_TYPE_BOOLEAN))

        then:
        (new COMetadataAccessor(new BasicCOProjectsRuntimeService()).extractSampleNameFromBamBasename(filename, context)) == resultSample

        where:
        filename                          | possibleControlSampleNamePrefixes | possibleTumorSampleNamePrefixes | searchWithSeparator | resultSample
        "control_TEST002_mdup.bam"        | "( control )"                     | "( )"                           | false               | "control"
        "control_abc_TEST003_mdup.bam"    | "( control_abc )"                 | "( )"                           | false               | "control_abc"
        "control_abc_TEST004_mdup.bam"    | "( control control_abc )"         | "( )"                           | false               | "control_abc"
        "control_abc_0_TEST005_mdup.bam"  | "( control control_abc_0 )"       | "( )"                           | false               | "control_abc_0"
        "control_abc_01_TEST006_mdup.bam" | "( control control_abc_0 )"       | "( )"                           | false               | "control_abc_01"
        "control_abc_01_TEST007_mdup.bam" | "( control control_abc_0 )"       | "( )"                           | true                | "control"
        "control_02_TEST008_mdup.bam"     | "( control_02 )"                  | "( )"                           | false               | "control_02"
        "Control_02_TEST009_mdup.bam"     | "( control_02 )"                  | "( )"                           | false               | "Control_02"
        "CONTROL_02_TEST010_mdup.bam"     | "( control_02 )"                  | "( )"                           | false               | "CONTROL_02"

        // In the following cases, the sample extraction will "Fail". But this is not a fault of the code, we just do not have enough information to
        // extract more. Maybe later, this could be extended with regex etc. But for now it is not possible.
        "control_abc_02_TEST011_mdup.bam" | "( control )"                     | "( )"                           | false               | "control"
        "control_abc_02_TEST012_mdup.bam" | "( control )"                     | "( )"                           | true                | "control"

        // Fallback. If no sample was found ( could be matched against the list ), the first part of the filename will be used. This
        // way, we will keep the old process.
        "control_abc_01_TEST013_mdup.bam" | "( control_abc_0 )"               | "( )"                           | true                | "control"
        "xeno_TEST014_mdup.bam"           | "( control )"                     | "( )"                           | false                | "xeno"
        "xeno_TEST014_mdup.bam"           | "( control )"                     | "( )"                           | true                | "xeno"
    }

}
