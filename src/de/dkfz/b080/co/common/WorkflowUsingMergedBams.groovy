package de.dkfz.b080.co.common;

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.COFileStageSettings;
import de.dkfz.b080.co.files.Sample;
import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.config.RecursiveOverridableMapContainerForConfigurationValues;
import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextError;
import de.dkfz.roddy.core.Workflow;
import de.dkfz.roddy.knowledge.files.BaseFile

import java.util.*;

import static de.dkfz.b080.co.files.COConstants.FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES;

/**
 * A basic workflow which uses merged bam files as an input and offers some check routines for those files.
 * Created by michael on 05.05.14.
 */

public abstract class WorkflowUsingMergedBams extends Workflow {

    private Map<DataSet, BasicBamFile[]> foundInputFiles = new LinkedHashMap<>();

    public BasicBamFile[] getInitialBamFiles(ExecutionContext context) {
        //Enable extract samples by default.
        RecursiveOverridableMapContainerForConfigurationValues configurationValues = context.getConfiguration().getConfigurationValues();
        boolean extractSamplesFromOutputFiles = configurationValues.getBoolean(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, true);
        configurationValues.put(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, "" + extractSamplesFromOutputFiles, "boolean");

        boolean bamfileListIsSet = configurationValues.hasValue("bamfile_list");

        BasicCOProjectsRuntimeService runtimeService = (BasicCOProjectsRuntimeService) context.getRuntimeService();
        List<Sample> samples = runtimeService.getSamplesForContext(context);
        List<BasicBamFile> bamsTumorMerged = new LinkedList<>();
        BasicBamFile bamControlMerged = null;
        DataSet dataSet = context.getDataSet();

        BasicBamFile[] found = null;

        synchronized (foundInputFiles) {
            if (!foundInputFiles.containsKey(dataSet)) {
                List<BasicBamFile> allFound = new LinkedList<>();
                if (bamfileListIsSet) {

                    List<String> bamFiles = configurationValues.getString("bamfile_list", "").split(StringConstants.SPLIT_SEMICOLON) as List<String>;
                    for (String bf : bamFiles) {
                        def path = new File(bf)
                        Sample sample = BasicCOProjectsRuntimeService.extractSamplesFromFilenames([path], context)[0];
                        if (sample.getType() == Sample.SampleType.CONTROL)
                            bamControlMerged = new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(path, context, new COFileStageSettings(sample, dataSet), null));
                        else if (sample.getType() == Sample.SampleType.TUMOR)
                            bamsTumorMerged.add(new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(path, context, new COFileStageSettings(sample, dataSet), null)));
                    }
                } else {

                    for (Sample sample : samples) {
                        BasicBamFile tempBam = ((BasicCOProjectsRuntimeService) context.getRuntimeService()).getMergedBamFileForDataSetAndSample(context, sample);
                        if (sample.getType() == Sample.SampleType.CONTROL)
                            bamControlMerged = tempBam;
                        else if (sample.getType() == Sample.SampleType.TUMOR)
                            bamsTumorMerged.add(tempBam);
                    }
                }
                allFound.add(bamControlMerged);
                allFound.addAll(bamsTumorMerged);
                foundInputFiles.put(dataSet, allFound.toArray(new BasicBamFile[0]));
            }
            found = foundInputFiles.get(dataSet);
            if (found != null && found[0] != null && found[0].getExecutionContext() != context) {
                BasicBamFile[] copy = new BasicBamFile[found.length];
                for (int i = 0; i < found.length; i++) {
                    if (found[i] == null) continue;
                    copy[i] = new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(found[i].getPath(), context, found[i].getFileStage().copy(), null));
                    copy[i].setAsSourceFile();
                }
                found = copy;
            }
        }
        return found;
    }

    public boolean checkInitialFiles(ExecutionContext context, BasicBamFile[] initialBamFiles) {
        if(!initialBamFiles)  initialBamFiles = new BasicBamFile[2];
        if(initialBamFiles.size() == 1) initialBamFiles = [ initialBamFiles[0], null] as BasicBamFile[];
        BasicBamFile bamControlMerged = initialBamFiles[0];
        BasicBamFile bamTumorMerged = initialBamFiles[1];
        if (bamControlMerged == null || bamTumorMerged == null) {
            if (bamControlMerged == null)
                context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Control bam is missing"));
            if (bamTumorMerged == null)
                context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("Tumor bam is missing"));
            return false;
        }
        return true;
    }


    @Override
    public boolean execute(ExecutionContext context) {
        BasicBamFile[] initialBamFiles = getInitialBamFiles(context);
        if (!checkInitialFiles(context, initialBamFiles))
            return false;

        //TODO Low priority. There were thoughts to have workflows which support multi-tumor samples, this it not supported by any workflow now.
        if (context.getConfiguration().getConfigurationValues().getBoolean("workflowSupportsMultiTumorSamples", false)) {
            return executeMulti(context, initialBamFiles);
        }
        return execute(context, initialBamFiles[0], initialBamFiles[1]);
    }

    protected abstract boolean execute(ExecutionContext context, BasicBamFile bamControlMerged, BasicBamFile bamTumorMerged);

    private boolean executeMulti(ExecutionContext context, BasicBamFile[] initialBamFiles) {

        boolean result = true;
        BasicBamFile bamControlMerged = initialBamFiles[0];
        for (int i = 1; i < initialBamFiles.length; i++) {
            result &= execute(context, bamControlMerged, initialBamFiles[i]);
        }
        return result;
    }

    @Override
    public boolean checkExecutability(ExecutionContext context) {
        BasicBamFile[] initialBamFiles = getInitialBamFiles(context);
        if (initialBamFiles == null) return false;
        return checkInitialFiles(context, initialBamFiles);
    }
}
