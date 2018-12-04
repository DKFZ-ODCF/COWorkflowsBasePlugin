/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.knowledge.metadata.sampleextractorstrategies

import de.dkfz.roddy.RoddyTestSpec

class SampleFromFilenameExtractorVersionTwoTest extends RoddyTestSpec {

    def "Version_2: Extract sample name from BAM basename"(String filename, List<String> possibleControlSampleNamePrefixes, List<String> possibleTumorSampleNamePrefixes, boolean matchExactSampleNames, boolean allowSampleTerminationWithIndex, boolean useLowerCaseFilenamesForSampleExtraction, String expectedSample) {

        when:
        def file = new File("/tmp/", filename)
        SampleFromFilenameExtractorVersionTwo extractor =
                new SampleFromFilenameExtractorVersionTwo(
                        file,
                        possibleControlSampleNamePrefixes + possibleTumorSampleNamePrefixes,
                        matchExactSampleNames,
                        allowSampleTerminationWithIndex,
                        useLowerCaseFilenamesForSampleExtraction
                )
        String extractedSample = extractor.extract()

        then:
        // The filename has no actual meaning. It is solely for debugging
        "$filename:$extractedSample" == "$filename:$expectedSample"

        where:
        filename                          | possibleControlSampleNamePrefixes | possibleTumorSampleNamePrefixes | matchExactSampleNames | allowSampleTerminationWithIndex | useLowerCaseFilenamesForSampleExtraction | expectedSample
        "control_TEST000_mdup.bam"        | ["cont"]                          | ["tumor"]                       | false                 | true                            | true                                     | "control"
        "control_TEST001_mdup.bam"        | ["cont"]                          | ["tumor"]                       | true                  | true                            | true                                     | null
        "control_TEST002_mdup.bam"        | ["control_"]                      | ["tumor"]                       | false                 | true                            | true                                     | "control"
        "control_abc_TEST003_mdup.bam"    | ["control_abc"]                   | ["tumor"]                       | false                 | true                            | true                                     | "control_abc"
        "control_abc_TEST004_mdup.bam"    | ["control", "control_abc"]        | ["tumor"]                       | false                 | true                            | true                                     | "control_abc"
        "control_abc_0_TEST005_mdup.bam"  | ["control", "control_abc_0"]      | ["tumor"]                       | false                 | true                            | true                                     | "control_abc_0"
        "control_abc_01_TEST006_mdup.bam" | ["control", "control_abc_0"]      | ["tumor"]                       | false                 | true                            | true                                     | "control_abc_01"
        "control_abc_01_TEST007_mdup.bam" | ["control", "control_abc_0"]      | ["tumor"]                       | true                  | true                            | true                                     | "control"
        "control_02_TEST008_mdup.bam"     | ["control_02"]                    | ["tumor"]                       | false                 | true                            | true                                     | "control_02"
        "Control_02_TEST009_mdup.bam"     | ["control_02"]                    | []                              | false                 | true                            | true                                     | "control_02"
        "CONTROL_02_TEST010_mdup.bam"     | ["control_02"]                    | []                              | false                 | true                            | true                                     | "control_02"
        "control_02_TEST011b_mdup.bam"    | ["control"]                       | ["tumor"]                       | true                  | true                            | true                                     | "control_02"
        "Control_02_TEST012b_mdup.bam"    | ["control"]                       | []                              | true                  | true                            | true                                     | "control_02"
        "CONTROL_02_TEST013b_mdup.bam"    | ["control"]                       | []                              | true                  | true                            | true                                     | "control_02"
        "control_02_TEST011c_mdup.bam"    | ["control_02"]                    | ["tumor"]                       | true                  | true                            | false                                    | "control_02"
        "Control_02_TEST012c_mdup.bam"    | ["Control_02"]                    | []                              | true                  | true                            | false                                    | "Control_02"
        "CONTROL_02_TEST013c_mdup.bam"    | ["CONTROL_02"]                    | []                              | true                  | true                            | false                                    | "CONTROL_02"

        // In the following cases, the sample extraction will "Fail". But this is not a fault of the code, we just do not have enough |information to
        // extract more. Maybe later, this could be extended with regex etc. But for now it is not possible.
        "control_abc_02_TEST014_mdup.bam" | ["control"]                       | []                              | false                 | true                            | true                                     | "control"
        "control_abc_02_TEST015_mdup.bam" | ["control"]                       | []                              | true                  | true                            | true                                     | "control"

        // Fallback. If no sample was found ( could be matched against the list ), the first part of the filename will be used. This
        // way, we will keep the old process.
        "control_abc_01_TEST016_mdup.bam" | ["control_abc_0"]                 | []                              | true                  | true                            | true                                     | null
        "control_abc_01_TEST016_mdup.bam" | ["control_abc_0"]                 | []                              | false                 | true                            | true                                     | "control_abc_01"

        "tumor_02_TEST019a_mdup.bam"      | ["control"]                       | ["tumor"]                       | false                 | false                           | true                                     | "tumor"
        "tumor_02_TEST019b_mdup.bam"      | ["control"]                       | ["tumor"]                       | true                  | true                            | true                                     | "tumor_02"
        "tumor02_TEST019c_mdup.bam"       | ["control"]                       | ["tumor"]                       | true                  | true                            | true                                     | null
        "tumor02_TEST019c_mdup.bam"       | ["control"]                       | ["tumor"]                       | false                 | true                            | true                                     | "tumor02"
        "tumor02_TEST019d_mdup.bam"       | ["control"]                       | ["tumor"]                       | true                  | false                           | true                                     | null
    }
}
