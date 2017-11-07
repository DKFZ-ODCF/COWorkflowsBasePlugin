/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
 */

package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.COFileStageSettings
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.config.RecursiveOverridableMapContainerForConfigurationValues
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.Workflow
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic

import java.util.*

import static de.dkfz.b080.co.files.COConstants.FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES

/**
 * A basic workflow which uses merged bam files as an input and offers some check routines for those files.
 * Created by michael on 05.05.14.
 */
@CompileStatic
abstract class WorkflowUsingMergedBams extends Workflow {

    public static final String BAMFILE_LIST = "bamfile_list"
    public static final String IS_NO_CONTROL_WORKFLOW = "isNoControlWorkflow"
    public static final String WORKFLOW_SUPPORTS_MULTI_TUMOR_SAMPLES = "workflowSupportsMultiTumorSamples"

    private Map<DataSet, BasicBamFile[]> _cachedFoundBamFiles = new LinkedHashMap<>()

    boolean isNoControlWorkflow(ExecutionContext context) {
        return getflag(context, IS_NO_CONTROL_WORKFLOW, false)
    }

    boolean supportsMultiTumor(ExecutionContext context) {
        context.getConfigurationValues().getBoolean(WORKFLOW_SUPPORTS_MULTI_TUMOR_SAMPLES, false)
    }

    BasicBamFile[] getInitialBamFiles(ExecutionContext context) {
        //Enable extract samples by default.
        RecursiveOverridableMapContainerForConfigurationValues configurationValues = context.getConfiguration().getConfigurationValues()
        boolean extractSamplesFromOutputFiles = configurationValues.getBoolean(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, true)
        configurationValues.put(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, "" + extractSamplesFromOutputFiles, "boolean")

        synchronized (_cachedFoundBamFiles) {
            checkAndFileBamFileCacheForDataSet(context)
            return copyBamFilesFromCacheWithNewContext(context)
        }
    }

    /**
     * Check, if a list of bamfiles was already loaded for this dataset.
     * Possibly needs to be synchronized with the copyBamFilesFromCacheWithNewContext
     * @param context
     */
    void checkAndFileBamFileCacheForDataSet(ExecutionContext context) {

        DataSet dataSet = context.getDataSet()
        if (!_cachedFoundBamFiles.containsKey(dataSet as Object)) {
            BasicCOProjectsRuntimeService runtimeService = (BasicCOProjectsRuntimeService) context.getRuntimeService()
            List<Sample> samples = runtimeService.getSamplesForContext(context)

            BasicBamFile bamControlMerged
            List<BasicBamFile> bamsTumorMerged = new LinkedList<>()

            List<BasicBamFile> foundBamFilesTemp = new LinkedList<>()
            boolean bamfileListIsSet = context.getConfigurationValues().hasValue(BAMFILE_LIST)

            List<BasicBamFile> listOfBams = []

            if (bamfileListIsSet) {
                // The bam loading code at this position should maybe be moved to the runtimeservice.
                // The get samples method relies on the array index of each bam file! This should work, but might be problematic. let's check it.
                // This code is only called when bamfile_list is set, so in this case it should always work. Order matters!!

                List<String> bamFiles = new COConfig(context).getBamList()
                if (bamFiles.size() != samples.size()) {
                    context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Bam files were passed as a list but the count of bam files does not match the sample count, will not run for this dataset."))
                } else {
                    for (int i = 0; i < bamFiles.size(); i++)
                        listOfBams << new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(new File(bamFiles[i]), context, new COFileStageSettings(samples[i], dataSet), null))
                }
            } else {
                listOfBams += samples.collect { Sample sample -> runtimeService.getMergedBamFileForDataSetAndSample(context, sample) }
            }

            bamsTumorMerged += listOfBams.findAll { BasicBamFile bf -> bf.getSample().getType() == Sample.SampleType.TUMOR }

            if (!isNoControlWorkflow(context) && bamControlMerged) {
                def controlBams = listOfBams.findAll { BasicBamFile bf -> bf.getSample().getType() == Sample.SampleType.CONTROL }
                if (controlBams.size() > 1)
                    context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("There was more than one control bam file found. Will not run for this dataset."))

                foundBamFilesTemp << bamControlMerged
            }

            foundBamFilesTemp += bamsTumorMerged
            _cachedFoundBamFiles.put(dataSet, foundBamFilesTemp as BasicBamFile[])
        }
    }

    /**
     * Create a copy of a map of bamfiles in the _cachedFoundBamFiles map
     * Be careful, needs to be synchronized with the cache filling method.
     */
    BasicBamFile[] copyBamFilesFromCacheWithNewContext(ExecutionContext context) {
        DataSet dataSet = context.getDataSet()
        BasicBamFile[] foundBamFiles = _cachedFoundBamFiles[dataSet]
        if (foundBamFiles != null && foundBamFiles[0] != null && foundBamFiles[0].getExecutionContext() != context) {
            BasicBamFile[] copy = new BasicBamFile[foundBamFiles.length]
            for (int i = 0; i < foundBamFiles.length; i++) {
                if (foundBamFiles[i] == null) continue
                copy[i] = new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(foundBamFiles[i].getPath(), context, foundBamFiles[i].getFileStage().copy(), null))
                copy[i].setAsSourceFile()
            }
        }
        return foundBamFiles
    }

    /**
     * Check the list of files for consistency
     * @param context
     * @param initialBamFiles
     * @return
     */
    boolean checkInitialFiles(ExecutionContext context, BasicBamFile[] initialBamFiles) {
        List<ExecutionContextError> errors = []

        initialBamFiles = initialBamFiles.findAll() // Clean up list, only accept non null entries.

        if (!initialBamFiles)
            errors << ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Did not find any bam files.")
        else {
            BasicBamFile possibleControlBamFile = initialBamFiles[0]
            List<BasicBamFile> possibleTumorBamFiles

            if (isNoControlWorkflow(context)) {
                if (possibleControlBamFile || possibleControlBamFile.sample.sampleType == Sample.SampleType.TUMOR)
                    errors << ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Control bam is missing and workflow is not set to accept tumor only.")
                possibleTumorBamFiles = initialBamFiles[1..-1] // There is a control bam, change the list so, that it should only contain tumor files.
            } else {
                possibleTumorBamFiles = initialBamFiles as List<BasicBamFile>
            }

            if (possibleTumorBamFiles.any { BasicBamFile bf -> bf.sample.sampleType != Sample.SampleType.TUMOR })
                errors << ExecutionContextError.EXECUTION_NOINPUTDATA.expand("There are no tumor bam files.")
        }
        errors.each { context.addErrorEntry(it) }

        return errors
    }


    @Override
    boolean execute(ExecutionContext context) {
        BasicBamFile[] initialBamFiles = getInitialBamFiles(context)
        if (!checkInitialFiles(context, initialBamFiles))
            return false

        if (supportsMultiTumor(context))
            return executeMulti(context, initialBamFiles)

        if (isNoControlWorkflow(context))
            return execute(context, null, initialBamFiles[0])

        // Normal execution, one control, one tumor file.
        return execute(context, initialBamFiles[0], initialBamFiles[1])
    }

    protected abstract boolean execute(ExecutionContext context, BasicBamFile bamControlMerged, BasicBamFile bamTumorMerged)

    private boolean executeMulti(ExecutionContext context, BasicBamFile[] initialBamFiles) {

        boolean result = true
        BasicBamFile bamControlMerged = initialBamFiles[0]
        for (int i = 1; i < initialBamFiles.length; i++) {
            result &= execute(context, bamControlMerged, initialBamFiles[i])
        }
        return result
    }

    @Override
    boolean checkExecutability(ExecutionContext context) {
        BasicBamFile[] initialBamFiles = getInitialBamFiles(context)
        if (initialBamFiles == null) return false
        return checkInitialFiles(context, initialBamFiles)
    }
}
