/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
 */

package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.COFileStageSettings
import de.dkfz.b080.co.files.Sample
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

    /**
     * Flag, which can be enable in the configuration. true == you can't / don't use a control bam file.
     */
    public static final String IS_NO_CONTROL_WORKFLOW = "isNoControlWorkflow"

    public static final String WORKFLOW_SUPPORTS_MULTI_TUMOR_SAMPLES = "workflowSupportsMultiTumorSamples"

    /**
     * A cache map of bam files found for datasets.
     * First file in a list is either control or tumor, all other files
     */
    private Map<DataSet, BasicBamFile[]> foundBamFilesForDatasets = new LinkedHashMap<>()

    /**
     * Query, if this workflow runs without a control bam
     * @param context
     * @return
     */
    boolean isNoControlWorkflow(ExecutionContext context) {
        getflag(context, IS_NO_CONTROL_WORKFLOW, false)
    }

    @Deprecated
    BasicBamFile[] getInitialBamFiles(ExecutionContext context) {
        return loadInitialBamFilesForDataset(context)
    }

    /**
     * Load and cache bam files for a dataset.
     * If the bam files were already loaded for a dataset, they are returned from the cache.
     *
     * @param context
     * @return
     */
    BasicBamFile[] loadInitialBamFilesForDataset(ExecutionContext context) {
        DataSet dataSet = context.getDataSet()

        // Enable extract samples by default.
        COConfig coConfig = new COConfig(context)
        context.configuration.configurationValues.put(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, "" + coConfig.getExtractSamplesFromOutputFiles(true), "boolean")

        // Get all the samples for the datasets
        BasicCOProjectsRuntimeService runtimeService = (BasicCOProjectsRuntimeService) context.getRuntimeService()
        List<Sample> samples = runtimeService.getSamplesForContext(context)

        List<BasicBamFile> bamsTumorMerged = []
        BasicBamFile bamControlMerged = null

        // Array of bam files which hopefully can be found.
        BasicBamFile[] bamFilesForDataset

        def assignBam = { BasicBamFile bam ->
            Sample sample = bam.sample
            if (sample.getType() == Sample.SampleType.CONTROL)
                bamControlMerged = bam
            else if (sample.getType() == Sample.SampleType.TUMOR)
                bamsTumorMerged.add(bam)
        }

        synchronized (foundBamFilesForDatasets) {

            // Check the cache for the dataset. If not, try to load the files
            if (!foundBamFilesForDatasets.containsKey(dataSet as Object)) {

                List<BasicBamFile> allFound = []

                // If the bam_list is set, use it.
                if (coConfig.getBamList()) {
                    List<String> bamFiles = coConfig.getBamList()
                    for (int i = 0; i < bamFiles.size(); i++) {
                        File path = new File(bamFiles.get(i))
                        // The bam loading code at this position should maybe be moved to the runtimeservice.
                        // The get samples method relies on the array index of each bam file! This should work, but might be problematic. let's check it.
                        // Ok, this code is only called when bamfile_list is set, so in this case it should always work. Order matters!!
                        Sample sample = runtimeService.getSamplesForContext(context).get(i)

                        assignBam(new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(path, context, new COFileStageSettings(sample, dataSet), null)))
                    }
                } else { // load it with the runtime service

                    for (Sample sample : samples)
                        assignBam(runtimeService.getMergedBamFileForDataSetAndSample(context, sample))

                }
                if (!isNoControlWorkflow(context) && bamControlMerged) allFound.add(bamControlMerged)
                allFound.addAll(bamsTumorMerged)
                foundBamFilesForDatasets.put(dataSet, allFound as BasicBamFile[])
            }

            // Now return copies of found files.
            // Why do we return the copies? Because we don't want to run the above code serveral times and to prevent
            // duplicate loader messages.
            // The copies are also linked to the new context now.
            bamFilesForDataset = foundBamFilesForDatasets[dataSet]
            if (bamFilesForDataset != null && bamFilesForDataset[0] != null && bamFilesForDataset[0].getExecutionContext() != context) {
                BasicBamFile[] copy = new BasicBamFile[bamFilesForDataset.length]
                for (int i = 0; i < bamFilesForDataset.length; i++) {
                    if (bamFilesForDataset[i] == null) continue
                    copy[i] = new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(bamFilesForDataset[i].getPath(), context, bamFilesForDataset[i].getFileStage().copy(), null))
                    copy[i].setAsSourceFile()
                }
                bamFilesForDataset = copy
            }
        }
        return bamFilesForDataset
    }

    /**
     * Check if the premises for the bam workflow are alright.
     *
     * Check if all files are valid and if the samples are
     * [ control, tumor, tumor, .. , tumor ]
     *
     * OR
     *
     * [ tumor, tumor, .. , tumor ]
     * for no control workflows
     *
     * @param context
     * @param initialBamFiles
     * @return
     */
    boolean checkInitialFiles(ExecutionContext context, BasicBamFile[] initialBamFiles) {
        if (!initialBamFiles) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Did not find any bam files."))
            return false
        }

        if (isNoControlWorkflow(context)) {
            boolean foundAll = true
            initialBamFiles.each { BasicBamFile it -> foundAll &= it && ((COFileStageSettings) it.getFileStage()).sample.sampleType == Sample.SampleType.TUMOR }

            if (!foundAll) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Not all found files are tumor bam files."))
                return false
            }

        } else {
            boolean foundAll = true
            BasicBamFile bamControlMerged = initialBamFiles[0]

            for (int i = 1; i < initialBamFiles.size(); i++) {
                if (initialBamFiles[i] == null || !((COFileStageSettings) initialBamFiles[i].getFileStage()).sample.sampleType == Sample.SampleType.TUMOR) {
                    context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Tumor bam is missing."))
                    foundAll = false
                }
            }

            if (bamControlMerged == null || ((COFileStageSettings) bamControlMerged.getFileStage()).sample.sampleType == Sample.SampleType.TUMOR) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Control bam is missing and workflow is not set to accept tumor only."))
                foundAll = false
            }

            if (!foundAll) return false
        }
        return true
    }

    /**
     * Execute the workflow.
     * Either execute it for control based or no control workflows
     * Also allow multi
     * @param context
     * @return
     */
    @Override
    boolean execute(ExecutionContext context) {
        BasicBamFile[] initialBamFiles = loadInitialBamFilesForDataset(context)
        if (!checkInitialFiles(context, initialBamFiles))
            return false

        //TODO Low priority. There were thoughts to have workflows which support multi-tumor samples, this it not supported by any workflow now.
        if (context.getConfiguration().getConfigurationValues().getBoolean(WORKFLOW_SUPPORTS_MULTI_TUMOR_SAMPLES, false)) {
            return executeMulti(context, initialBamFiles)
        }

        if (isNoControlWorkflow(context))
            return execute(context, null, initialBamFiles[0])
        else
            return execute(context, initialBamFiles[0], initialBamFiles[1])
    }

    protected abstract boolean execute(ExecutionContext context, BasicBamFile bamControlMerged, BasicBamFile bamTumorMerged)

    /**
     * Execute this method
     * @param context
     * @param initialBamFiles
     * @return
     */
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
        BasicBamFile[] initialBamFiles = loadInitialBamFilesForDataset(context)
        if (initialBamFiles == null) return false
        return checkInitialFiles(context, initialBamFiles)
    }
}
