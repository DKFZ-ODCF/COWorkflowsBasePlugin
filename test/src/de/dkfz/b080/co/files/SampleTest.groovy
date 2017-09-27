/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.files

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import groovy.transform.CompileStatic
import org.junit.Test;


@CompileStatic
public class SampleTest {

    Configuration getConfiguration(String control, String tumor) {
        Configuration config = new Configuration(null)
        config.getConfigurationValues().addAll([
                new ConfigurationValue(config, COConstants.CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES, control),
                new ConfigurationValue(config, COConstants.CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES, tumor)
        ] as LinkedList<ConfigurationValue>)
        return config
    }

    ExecutionContext getExecutionContext(String control, String tumor) {
        return MockupExecutionContextBuilder.createSimpleContext(Sample.class, getConfiguration(control, tumor))
    }


    @Test
    public void testGetSampleType_MatchInBothPrefixSets() throws Exception {
        ExecutionContext context = getExecutionContext("tumor1", "tumor1a")
        Sample.SampleType sampleType = Sample.determineSampleType(context, "tumor1a")
        assert sampleType == Sample.SampleType.UNKNOWN
        assert context.getErrors().size() == 1
        assert context.getErrors().getAt(0).toString().contains("Sample name")
        assert context.getErrors().getAt(0).toString().contains("matches both")
    }

    @Test
    public void testGetSampleType_MatchControlPrefix() throws Exception {
        ExecutionContext context = getExecutionContext("control", "tumor")
        assert Sample.determineSampleType(context, "control") == Sample.SampleType.CONTROL
    }

    @Test
    public void testGetSampleType_MatchTumorPrefix() throws Exception {
        ExecutionContext context = getExecutionContext("blood", "metastasis")
        assert Sample.determineSampleType(context, "metastasis") == Sample.SampleType.TUMOR
    }

    @Test
    public void testGetSampleType_MatchNeitherPrefix() throws Exception {
        ExecutionContext context = getExecutionContext("blood", "tumor")
        assert Sample.determineSampleType(context, "metastasis") == Sample.SampleType.UNKNOWN
    }


}