/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
 */
package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.Workflow
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic

import java.util.logging.Level

import static de.dkfz.b080.co.files.COConstants.FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES
import static de.dkfz.b080.co.files.Sample.SampleType.CONTROL
import static de.dkfz.b080.co.files.Sample.SampleType.TUMOR

/**
 * A basic workflow which uses merged bam files as an input and offers some check routines for those files.
 *
 * @author Michael Heinold
 *
 */
@CompileStatic
abstract class WorkflowUsingMergedBams extends Workflow {

    /**
     * Flag, which can be enable in the configuration. true == you can't / don't use a control bam file.
     */
    public static final String IS_NO_CONTROL_WORKFLOW = "isNoControlWorkflow"

    /**
     * The flag will automatically be created and is the negative of isNoControlWorkflow. If you set it, it will be
     * overriden
     */
    public static final String IS_CONTROL_WORKFLOW = "isControlWorkflow"

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
    @Deprecated
    boolean isNoControlWorkflow(ExecutionContext context) {
        getflag(IS_NO_CONTROL_WORKFLOW, false)
    }

    boolean isNoControlWorkflow() {
        getFlag(IS_NO_CONTROL_WORKFLOW, false)
    }

    boolean isControlWorkflow() {
        !isNoControlWorkflow()
    }

    /**
     * The first element is the control BAM and the remaining the tumor BAMs.
     * @param context
     * @return
     */
    @Deprecated
    BasicBamFile[] getInitialBamFiles(ExecutionContext context) {
        return loadInitialBamFilesForDataset(context)
    }

    /**
     * Load and cache bam files for a dataset.
     * The first element is the control BAM and the remaining the tumor BAMs.
     * If the bam files were already loaded for a dataset, they are returned from the cache.
     *
     * @param context
     * @return
     */
    BasicBamFile[] loadInitialBamFilesForDataset(ExecutionContext context) {
        DataSet dataSet = context.getDataSet()

        // Enable extract samples by default.
        COConfig coConfig = new COConfig(context)
        context.configuration.configurationValues.put(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, "" +
                coConfig.getExtractSamplesFromOutputFiles(true), "boolean")

        BasicCOProjectsRuntimeService runtimeService = (BasicCOProjectsRuntimeService) context.getRuntimeService()

        List<BasicBamFile> bamsTumorMerged = []
        List<BasicBamFile> bamsControlMerged = []

        // Array of bam files which hopefully can be found.
        BasicBamFile[] bamFilesForDataset

        synchronized (foundBamFilesForDatasets) {

            // Check the cache for the dataset. If not, try to load the files.
            if (!foundBamFilesForDatasets.containsKey(dataSet)) {

                runtimeService.metadataAccessor.getAllBamFiles(context).findAll().collect { BasicBamFile bam ->
                    Sample sample = bam.sample
                    if (sample.getType() == Sample.SampleType.CONTROL)
                        bamsControlMerged << bam
                    else if (sample.getType() == TUMOR)
                        bamsTumorMerged << bam
                    else {
                        context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.
                                expand("Skipping BAM that is not classified as tumor or control: ${bam.getAbsolutePath()}", Level.WARNING))
                        null
                    }
                }.findAll { it }

                if (bamsControlMerged.size() > 1) {
                    context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("More than one control sample in bamfile_list:\n" +
                            bamsControlMerged.collect { "${it.sample.name}: ${it.getAbsolutePath()}" }.join("\n")))
                }
                if (!isNoControlWorkflow() && bamsControlMerged.size() == 1)
                    foundBamFilesForDatasets[dataSet] = (bamsControlMerged + bamsTumorMerged) as BasicBamFile[]
                else
                    foundBamFilesForDatasets[dataSet] = bamsTumorMerged as BasicBamFile[]
            }

            // Now return copies of found files.

            // Why do we return the copies? Because we don't want to run the above code several times and to prevent duplicate loader messages.
            // The copies are also linked to the new context now.
            bamFilesForDataset = foundBamFilesForDatasets[dataSet]
            if (bamFilesForDataset != null
                    && bamFilesForDataset.size() > 0
                    && bamFilesForDataset[0] != null
                    && bamFilesForDataset[0].getExecutionContext() != context) {
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
            context.addError(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Did not find any BAM files."))
            return false
        }

        assertBamfileArrayValidity(initialBamFiles)

        if (isControlWorkflow())
            return checkBamfileArrayContentForControlWorkflows(context, initialBamFiles)

        return checkBamfileArrayContentForTumorOnlyWorkflows(context, initialBamFiles)
    }

    void assertBamfileArrayValidity(BasicBamFile[] initialBamFiles) {
        if (initialBamFiles.any { it == null }) {
            throw new RuntimeException(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("WorkflowUsingMergedBams.checkInitialFiles failed: The list of BAM files contains an empty entry.").getDescription())
        }

        if (initialBamFiles.any { BasicBamFile bam -> bam.fileStage == null }) {
            throw new RuntimeException(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("WorkflowUsingMergedBams.checkInitialFiles failed: The list of BAM files contains an entry with unset sample.").getDescription())
        }
    }

    boolean checkBamfileArrayContentForTumorOnlyWorkflows(ExecutionContext context, BasicBamFile[] initialBamFiles) {
        List<ExecutionContextError> errors = []
        if (initialBamFiles.size() == 0)
            errors << ExecutionContextError.EXECUTION_NOINPUTDATA.expand("No tumor BAM files found.")
        else if (initialBamFiles.size() > 0 && !initialBamFiles.every { BasicBamFile bam -> bam.sample.sampleType == TUMOR }) {
            errors << ExecutionContextError.EXECUTION_NOINPUTDATA.expand("The list of BAM files must consist one or more tumor BAM files. Not all files are tumor BAM files.")
        }
        errors.each { context.addError(it) }
        return !errors
    }

    boolean checkBamfileArrayContentForControlWorkflows(ExecutionContext context, BasicBamFile[] initialBamFiles) {
        List<ExecutionContextError> errors = []
        boolean controlWasFound = true
        if (initialBamFiles[0].sample.type != CONTROL) {
            errors << ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Control BAM file is missing and workflow is not set to accept tumor only." +
                    "\n\t- Set the cvalue isNoControlWorkflow=true in your configuration to allow this." +
                    "\n\t- Please note, that the workflow needs to support this option."
            )
            controlWasFound = false
        }

        if (controlWasFound && initialBamFiles.size() == 1)
            errors << ExecutionContextError.EXECUTION_NOINPUTDATA.expand("No tumor BAM file found.")
        else if (!initialBamFiles[(controlWasFound ? 1 : 0)..-1].every { it.sample.sampleType == TUMOR }) {
            errors << ExecutionContextError.EXECUTION_NOINPUTDATA.expand("The list of BAM files must contain one control BAM file and one or more tumor BAM files. Some of these files are neither of sample type control nor tumor.")
        }
        errors.each { context.addError(it) }
        return !errors
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

        // Just put them to the context config, so they are available in every case.
        context.configurationValues.put(IS_CONTROL_WORKFLOW, isControlWorkflow().toString())
        context.configurationValues.put(IS_NO_CONTROL_WORKFLOW, isNoControlWorkflow().toString())

        //TODO Low priority. There were thoughts to have workflows which support multi-tumor samples, this it not supported by any workflow now.
        if (context.getConfiguration().getConfigurationValues().getBoolean(WORKFLOW_SUPPORTS_MULTI_TUMOR_SAMPLES, false)) {
            return executeMulti(context, initialBamFiles)
        }

        if (isNoControlWorkflow())
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
