/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.knowledge.metadata.sampleextractorstrategies

import groovy.transform.CompileStatic

/**
 * Base class for different sample extractors
 */
@CompileStatic
abstract class SampleFromFilenameExtractor {

    final File file

    SampleFromFilenameExtractor(File file) {
        this.file = file
    }

    abstract String extract()
}
