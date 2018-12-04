/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.knowledge.metadata.sampleextractorstrategies

import de.dkfz.roddy.RoddyTestSpec

class SampleFromFilenameExtractorVersionOneTest extends RoddyTestSpec {

    def "Version_1: Extract sample name from BAM basename"(String filename, boolean enforceAtomicSampleName, expectedSample) {

        when:
        def file = new File("/tmp/", filename)
        SampleFromFilenameExtractorVersionOne extractor = new SampleFromFilenameExtractorVersionOne(file, enforceAtomicSampleName)
        String extractedSample = extractor.extract()

        then:
        // The filename has no actual meaning. It is solely for debugging
        "$filename:$extractedSample" == "$filename:$expectedSample"

        where:
        filename                       | enforceAtomicSampleName | expectedSample
        "control_TEST000_mdup.bam"     | false                   | "control"
        "control_02_TEST008_mdup.bam"  | false                   | "control_02"
        "Control_02_TEST009_mdup.bam"  | false                   | "Control_02"
        "CONTROL_02_TEST010_mdup.bam"  | false                   | "CONTROL_02"

        "control_002_TEST008_mdup.bam" | false                   | "control"

        "control_TEST000_mdup.bam"     | true                    | "control"
        "control_02_TEST011b_mdup.bam" | true                    | "control"
        "Control_02_TEST012b_mdup.bam" | true                    | "Control"
        "CONTROL_02_TEST013b_mdup.bam" | true                    | "CONTROL"

        "tumor_02_TEST019a_mdup.bam"   | false                   | "tumor_02"
        "tumor_02_TEST019b_mdup.bam"   | true                    | "tumor"
        "tumor02_02_TEST019c_mdup.bam" | false                   | "tumor02_02"
        "tumor02_02_TEST019c_mdup.bam" | true                    | "tumor02"
        "tumor02_TEST019c_mdup.bam"    | true                    | "tumor02"
    }
}
