/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.knowledge.metadata.sampleextractorstrategies

import de.dkfz.roddy.StringConstants
import groovy.transform.CompileStatic

/**
 * Untested and deprecated, replaced by SampleFromFilenameExtractorVersionTwo
 */
@CompileStatic
class SampleFromFilenameExtractorVersionOne extends SampleFromFilenameExtractor {

    private final boolean enforceAtomicSampleName

    SampleFromFilenameExtractorVersionOne(File file, boolean enforceAtomicSampleName) {
        super(file)
        this.enforceAtomicSampleName = enforceAtomicSampleName
    }

    @Override
    String extract() {
        String[] split = file.name.split(StringConstants.SPLIT_UNDERSCORE)
        if (split.size() <= 2) {
            return null
        }
        String sampleName = split[0]
        if (!enforceAtomicSampleName && split[1].isInteger() && split[1].length() <= 2)
            sampleName = split[0..1].join(StringConstants.UNDERSCORE)
        return sampleName
    }
}
