/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.COFileStageSettings
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.StringConstants
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

    private Map<DataSet, BasicBamFile[]> foundInputFiles = new LinkedHashMap<>()

    BasicBamFile[] getInitialBamFiles(ExecutionContext context) {
        //Enable extract samples by default.
        RecursiveOverridableMapContainerForConfigurationValues configurationValues = context.getConfiguration().getConfigurationValues()
        boolean extractSamplesFromOutputFiles = configurationValues.getBoolean(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, true)
        configurationValues.put(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, "" + extractSamplesFromOutputFiles, "boolean")
        boolean isNoControlWorkflow = getflag(context, IS_NO_CONTROL_WORKFLOW, false)
        boolean bamfileListIsSet = configurationValues.hasValue(BAMFILE_LIST)
        // There is a method missing in COProjectsRuntimeService. This fix will ONLY work, when sample_list is set!
        List<String> samplesPassedInConfig = Arrays.asList(configurationValues.getString("sample_list", "").split("[;]")) as List<String>
        boolean sampleListIsSet = samplesPassedInConfig != null && samplesPassedInConfig.size() > 0

        BasicCOProjectsRuntimeService runtimeService = (BasicCOProjectsRuntimeService) context.getRuntimeService()
        List<Sample> samples = runtimeService.getSamplesForContext(context)
        List<BasicBamFile> bamsTumorMerged = new LinkedList<>()
        BasicBamFile bamControlMerged = null
        DataSet dataSet = context.getDataSet()

        BasicBamFile[] found

        synchronized (foundInputFiles) {
            if (!foundInputFiles.containsKey(dataSet as Object)) {
                List<BasicBamFile> allFound = new LinkedList<>()
                if (bamfileListIsSet) {
                    List<String> bamFiles = new COConfig(context).getBamList()
                    for (int i = 0; i < bamFiles.size(); i++) {
                        File path = new File(bamFiles.get(i))
                        // The bam loading code at this position should maybe be moved to the runtimeservice.
                        // The get samples method relies on the array index of each bam file! This should work, but might be problematic. let's check it.
                        // Ok, this code is only called when bamfile_list is set, so in this case it should always work. Order matters!!
                        Sample sample = ((BasicCOProjectsRuntimeService) context.getRuntimeService()).getSamplesForContext(context).get(i)

                        if (sample.getType() == Sample.SampleType.CONTROL)
                            bamControlMerged = new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(path, context, new COFileStageSettings(sample, dataSet), null))
                        else if (sample.getType() == Sample.SampleType.TUMOR)
                            bamsTumorMerged << new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(path, context, new COFileStageSettings(sample, dataSet), null))
                    }
                } else {

                    for (Sample sample : samples) {
                        BasicBamFile tempBam = ((BasicCOProjectsRuntimeService) context.getRuntimeService()).getMergedBamFileForDataSetAndSample(context, sample)
                        if (sample.getType() == Sample.SampleType.CONTROL)
                            bamControlMerged = tempBam
                        else if (sample.getType() == Sample.SampleType.TUMOR)
                            bamsTumorMerged.add(tempBam)
                    }
                }
                if (!isNoControlWorkflow && bamControlMerged) allFound.add(bamControlMerged)
                allFound.addAll(bamsTumorMerged)
                foundInputFiles.put(dataSet, allFound as BasicBamFile[])
            }
            found = foundInputFiles[dataSet]
            if (found != null && found[0] != null && found[0].getExecutionContext() != context) {
                BasicBamFile[] copy = new BasicBamFile[found.length]
                for (int i = 0; i < found.length; i++) {
                    if (found[i] == null) continue
                    copy[i] = new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(found[i].getPath(), context, found[i].getFileStage().copy(), null))
                    copy[i].setAsSourceFile()
                }
                found = copy
            }
        }
        return found
    }

    boolean checkInitialFiles(ExecutionContext context, BasicBamFile[] initialBamFiles) {
        boolean isNoControlWorkflow = getflag(context, IS_NO_CONTROL_WORKFLOW, false)
        if (!initialBamFiles) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Did not find any bam files."))
            return false
        }
        if (isNoControlWorkflow) {
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
        return true;
    }


    @Override
    boolean execute(ExecutionContext context) {
        BasicBamFile[] initialBamFiles = getInitialBamFiles(context)
        if (!checkInitialFiles(context, initialBamFiles))
            return false

        //TODO Low priority. There were thoughts to have workflows which support multi-tumor samples, this it not supported by any workflow now.
        if (context.getConfiguration().getConfigurationValues().getBoolean(WORKFLOW_SUPPORTS_MULTI_TUMOR_SAMPLES, false)) {
            return executeMulti(context, initialBamFiles)
        }
        boolean isNoControlWorkflow = getflag(context, IS_NO_CONTROL_WORKFLOW, false)
        if (isNoControlWorkflow)
            return execute(context, null, initialBamFiles[0])
        else
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
