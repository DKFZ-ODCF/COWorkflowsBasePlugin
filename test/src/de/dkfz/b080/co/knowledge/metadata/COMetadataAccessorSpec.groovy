/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.knowledge.metadata

import de.dkfz.b080.co.common.BasicCOProjectsRuntimeService
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.RoddyTestSpec
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import spock.lang.Ignore

import static de.dkfz.b080.co.common.COConstants.*

class COMetadataAccessorSpec extends RoddyTestSpec {

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

    @Ignore
    def "Version_1: Extract sample name from BAM basename"(String filename, String possibleControlSampleNamePrefixes, String possibleTumorSampleNamePrefixes, boolean useAtomicSampleNames, boolean allowSampleTerminationWithIndex, String resultSample) {

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
