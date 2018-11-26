package de.dkfz.b080.co.files

import de.dkfz.b080.co.common.COConstants
import de.dkfz.roddy.RoddyTestSpec
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ContextResource
import groovy.transform.CompileStatic
import org.junit.Rule
import org.junit.Test

class SampleSpec extends RoddyTestSpec {

    Configuration getConfiguration(String control, String tumor) {
        Configuration config = new Configuration(null)
        config.getConfigurationValues().addAll([
                new ConfigurationValue(config, COConstants.CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES, control, ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY),
                new ConfigurationValue(config, COConstants.CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES, tumor, ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY)
        ] as LinkedList<ConfigurationValue>)
        return config
    }

    ExecutionContext getExecutionContext(String control, String tumor) {
        return contextResource.createSimpleContext(Sample.class, getConfiguration(control, tumor))
    }

    def testGetSampleType_MatchInBothPrefixSets() {
        when:
        ExecutionContext context = getExecutionContext("(tumor1)", "(tumor1a)")
        Sample.SampleType sampleType = Sample.determineSampleType(context, "tumor1a")

        then:
        sampleType == Sample.SampleType.UNKNOWN
        context.getErrors().size() == 1
        context.getErrors()[0].toString().contains("Sample name")
        context.getErrors()[0].toString().contains("matches both")
    }

    def testGetSampleType_MatchControlPrefix() {
        when:
        ExecutionContext context = getExecutionContext("( control )", "(tumor)")

        then:
        Sample.determineSampleType(context, "control") == Sample.SampleType.CONTROL
    }

    def testGetSampleType_MatchTumorPrefix() {
        when:
        ExecutionContext context = getExecutionContext("(blood)", "(metastasis)")

        then:
        Sample.determineSampleType(context, "metastasis") == Sample.SampleType.TUMOR
    }

    def testGetSampleType_MatchNeitherPrefix() {
        when:
        ExecutionContext context = getExecutionContext("(blood)", "(tumor)")

        then:
        Sample.determineSampleType(context, "metastasis") == Sample.SampleType.UNKNOWN
    }

    def testCompareTo() {
        when:
        def context = getExecutionContext("( control blood control_02 )", "( tumor_01 tumor_02 xenograft )")
        def c00 = new Sample(context, "blood")
        def c01 = new Sample(context, "control")
        def c02 = new Sample(context, "control_02")
        def t00 = new Sample(context, "tumor_01")
        def t01 = new Sample(context, "tumor_02")
        def t02 = new Sample(context, "xenograft")

        List<Sample> input = [c02, t02, c00, t00, c01, t01,]

        List<Sample> expected = [c00, c01, c02, t00, t01, t02]

        then:
        input.sort() == expected
    }

    def testEquals() {
        when:
        def context = getExecutionContext("( control blood control_02 )", "( tumor_01 tumor_02 xenograft )")
        def c00 = new Sample(context, "blood")
        def c01 = new Sample(context, "control")
        def c02 = new Sample(context, "control_02")
        def c03 = new Sample(context, "control_02")
        def t00 = new Sample(context, "tumor_01")

        then:
        c00 != c01
        c02 == c03
        c00 != t00
    }
}