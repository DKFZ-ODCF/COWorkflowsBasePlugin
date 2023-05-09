/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
 */

package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.Sample
import de.dkfz.b080.co.knowledge.metadata.COMetadataAccessor
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic

import static COConstants.CVALUE_SAMPLE_DIRECTORY
import static COConstants.CVALUE_SEQUENCE_DIRECTORY
import static de.dkfz.b080.co.common.COConstants.CVALUE_ALIGNMENT_INPUT_DIRECTORY_NAME

@CompileStatic
class BasicCOProjectsRuntimeService extends RuntimeService {

    public COMetadataAccessor metadataAccessor

    BasicCOProjectsRuntimeService() {
        super()
        metadataAccessor = new COMetadataAccessor(this)
    }

    COMetadataAccessor getMetadataAccessor() {
        return this.metadataAccessor
    }

    /**
     * Use RuntimeService._createJobName or the instance method createJobName
     */
    @Deprecated
    @Override
    String createJobName(ExecutionContext executionContext, BaseFile file, String toolID, boolean reduceLevel) {
        return _createJobName(executionContext, file, toolID, reduceLevel)
    }

    File getAlignmentDirectory(ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        return getDirectory(cfg.alignmentFolderName, context)
    }

    File getAlignmentInputDirectory(ExecutionContext context, Sample sample) {
        return fillTemplatesInPathnameString(CVALUE_ALIGNMENT_INPUT_DIRECTORY_NAME, context, sample)
    }

    File getSampleDirectory(ExecutionContext process, Sample sample, String library = null) {
        File sampleDir = fillTemplatesInPathnameString(CVALUE_SAMPLE_DIRECTORY, process, sample, library)
        return sampleDir
    }

    File getSequenceDirectory(ExecutionContext process, Sample sample, String run, String library = null) {
        return new File(fillTemplatesInPathnameString(CVALUE_SEQUENCE_DIRECTORY, process, sample, library).getAbsolutePath().replace('${run}', run))
    }

    /**
     * Given a path and some background information (context/dataset, sample, library), fill in the dataSet, sample, library and other placeholders
     * in the path, make it an absolute path and return it as File object.
     *
     * TODO: Issue #257
     *
     * @param dir
     * @param context
     * @param sample
     * @param library
     * @return
     */
    @Deprecated
    protected File fillTemplatesInPathnameString(String dir, ExecutionContext context, Sample sample, String library = null) {
        Configuration cfg = context.configuration
        File path = cfg.configurationValues.get(dir).toFile(context)
        String temp = path.absolutePath.
                replace('${dataSet}', context.dataSet.toString()).
                replace('${sample}', sample.name)
        if (library)
            temp = temp.replace('${library}', library)
        else
            temp = temp.replace('${library}/', "")

        return new File(temp)
    }

    @Deprecated
    List<Sample> getSamplesForContext(ExecutionContext context) {
        return metadataAccessor.getSamples(context)
    }

    @Deprecated
    MetadataTable getMetadataTable(ExecutionContext context) {
        return metadataAccessor.getMetadataTable(context)
    }

}

