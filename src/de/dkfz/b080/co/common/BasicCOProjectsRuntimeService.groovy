/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/COWorkflowsBasePlugin/LICENSE).
 */

package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.Sample
import de.dkfz.b080.co.knowledge.metadata.COMetadataAccessor
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.execution.jobs.BatchEuphoriaJobManager
import de.dkfz.roddy.execution.jobs.JobManager
import de.dkfz.roddy.knowledge.files.BaseFile

import static COConstants.CVALUE_SAMPLE_DIRECTORY
import static COConstants.CVALUE_SEQUENCE_DIRECTORY

@groovy.transform.CompileStatic
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
        Configuration cfg = context.getConfiguration()
        File path = cfg.getConfigurationValues().get(dir).toFile(context)
        String temp = path.getAbsolutePath()
        temp = temp.replace('${dataSet}', context.getDataSet().toString())
        temp = temp.replace('${sample}', sample.getName())
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

