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
        BasicCOProjectsRuntimeService runtimeService = (BasicCOProjectsRuntimeService) context.getRuntimeService()

        List<BasicBamFile> bamsTumorMerged = []
        List<BasicBamFile> bamsControlMerged = []

        // Array of bam files which hopefully can be found.
        BasicBamFile[] bamFilesForDataset

        synchronized (foundBamFilesForDatasets) {
            // Check the cache for the dataset. If not, try to load the files.
            if (!foundBamFilesForDatasets.containsKey(dataSet)) {

                runtimeService.getAllBamFiles(context).collect { BasicBamFile bam ->
                    Sample sample = bam.sample
                    if (sample.getType() == Sample.SampleType.CONTROL)
                        bamsControlMerged.add(bam)
                    else if (sample.getType() == Sample.SampleType.TUMOR)
                        bamsTumorMerged.add(bam)
                    else // Other types of BAMs are ignored!
                        null
                }.findAll { it }

                if (bamsControlMerged.size() > 1) {
                    context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("More than one control sample in bamfile_list:\n" +
                            bamsControlMerged.collect { "${it.sample.name}: ${it.getAbsolutePath()}" }.join("\n")))
                }
                if (!isNoControlWorkflow(context) && bamsControlMerged.size() == 1)
                    foundBamFilesForDatasets.put(dataSet, (bamsControlMerged + bamsTumorMerged) as BasicBamFile[])
                else
                    foundBamFilesForDatasets.put(dataSet, bamsTumorMerged as BasicBamFile[])
            }

            // Now return copies of found files.
            // Why do we return the copies? Because we don't want to run the above code several times and to prevent duplicate loader messages.
            // The copies are also linked to the new context now.
            bamFilesForDataset = foundBamFilesForDatasets[dataSet]
            if (bamFilesForDataset != null &&
                    bamFilesForDataset.size() > 0 &&
                    bamFilesForDataset[0] != null &&
                    bamFilesForDataset[0].getExecutionContext() != context) {
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
            context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Did not find any BAM files."))
            return false
        }

        if (isNoControlWorkflow(context)) {
            boolean foundAll = true
            initialBamFiles.each { BasicBamFile it -> foundAll &= it && ((COFileStageSettings) it.getFileStage()).sample.sampleType == Sample.SampleType.TUMOR }

            if (!foundAll) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Not all found files are tumor BAM files."))
                return false
            }

        } else {
            boolean foundAll = true
            BasicBamFile bamControlMerged = initialBamFiles[0]

            for (int i = 1; i < initialBamFiles.size(); i++) {
                if (initialBamFiles[i] == null || !((COFileStageSettings) initialBamFiles[i].getFileStage()).sample.sampleType == Sample.SampleType.TUMOR) {
                    context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Tumor BAM is missing."))
                    foundAll = false
                }
            }

            if (bamControlMerged == null || ((COFileStageSettings) bamControlMerged.getFileStage()).sample.sampleType == Sample.SampleType.TUMOR) {
                context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Control BAM is missing and workflow is not set to accept tumor only."))
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
        return checkInitialFiles(context, loadInitialBamFilesForDataset(context))
    }
}
