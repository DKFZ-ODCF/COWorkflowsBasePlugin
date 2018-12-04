/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.common

import de.dkfz.b080.co.knowledge.metadata.MethodForSampleFromFilenameExtraction
import de.dkfz.roddy.RoddyTestSpec
import de.dkfz.roddy.config.ConfigurationError
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext

class COConfigSpec extends RoddyTestSpec {

    void getSelectedSampleExtractionMethod(String selected, MethodForSampleFromFilenameExtraction result) {

        when:
        ExecutionContext context = contextResource.createSimpleContext(COConfigSpec)
        context.configurationValues.add(new ConfigurationValue("selectSampleExtractionMethod", selected))

        then:
        new COConfig(context).selectedSampleExtractionMethod == result

        where:
        selected    | result
        "version_1" | MethodForSampleFromFilenameExtraction.version_1
        "version_2" | MethodForSampleFromFilenameExtraction.version_2
    }

    void getSelectedSampleExtractionMethodWithExceptions(String selected, Class<Throwable> exception) {

        when:
        ExecutionContext context = contextResource.createSimpleContext(COConfigSpec)
        context.configurationValues.add(new ConfigurationValue("selectSampleExtractionMethod", selected))
        new COConfig(context).selectedSampleExtractionMethod

        then:
        def exc = thrown(exception)
        exc.message == "Value for selectSampleExtractionMethod is wrong, needs to be one of:\n\t- version_1\n\t- version_2"

        where:
        selected     | exception
        null         | ConfigurationError
        ""           | ConfigurationError
        "variant222" | ConfigurationError
    }
}