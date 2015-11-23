/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.*
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.core.*
import de.dkfz.roddy.execution.io.ExecutionResult
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider
import de.dkfz.roddy.execution.jobs.CommandFactory
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.tools.LoggerWrapper

import java.util.function.Consumer

import static de.dkfz.b080.co.files.COConstants.*

//import net.xeoh.plugins.base.annotations.PluginImplementation

/**
 * This service is mainly for qcpipeline. It might be of use for other projects
 * as well. TODO Think about renaming and extending this as needed.
 *
 * @author michael
 */
//@PluginImplementation
@groovy.transform.CompileStatic
public class COProjectsRuntimeService extends RuntimeService {

    private static LoggerWrapper logger = LoggerWrapper.getLogger(COProjectsRuntimeService.class.getName());

    private static List<File> alreadySearchedMergedBamFolders = [];

    /**
     * Releases the cache in this provider
     */
    @Override
    public void releaseCache() {

    }

    @Override
    public boolean initialize() {
    }

    @Override
    public void destroy() {
    }
//
//    @Override
//    public Map<String, Object> getDefaultJobParameters(ExecutionContext context, String TOOLID) {
//    }

    public Map<String, Object> getDefaultJobParameters(ExecutionContext context, String toolID) {
        def fs = context.getRuntimeService();
        //File cf = fs..createTemporaryConfigurationFile(executionContext);
        Configuration cfg = context.getConfiguration();
        String pid = context.getDataSet().toString()
        Map<String, Object> parameters = [
                pid         : (Object) pid,
                PID         : pid,
                CONFIG_FILE : fs.getNameOfConfigurationFile(context).getAbsolutePath(),
                ANALYSIS_DIR: context.getOutputDirectory().getParentFile().getParent()
        ]
        return parameters;
    }

    @Override
    public String createJobName(ExecutionContext executionContext, BaseFile file, String toolID, boolean reduceLevel) {
        return CommandFactory.getInstance().createJobName(file, toolID, reduceLevel);
    }

    /**
     * Checks if a folder is valid
     *
     * A folder is valid if:
     * <ul>
     *   <li>its parents are valid</li>
     *   <li>it was not created recently (within this context)</li>
     *   <li>it exists</li>
     *   <li>it can be validated (i.e. by its size or files, but not with a lengthy operation!)</li>
     * </ul>
     */
    @Override
    public boolean isFileValid(BaseFile baseFile) {
        //Parents valid?
        boolean parentsValid = true;
        for (BaseFile bf in baseFile.parentFiles) {
            if (bf.isTemporaryFile()) continue; //We do not check the existence of parent files which are temporary.
            if (bf.isSourceFile()) continue;
            if (!bf.isFileValid()) {
                return false;
            }
        }

        boolean result = true;

        //Source files should be marked as such and checked in a different way. They are assumed to be valid.
        if (baseFile.isSourceFile())
            return true;

        //Temporary files are also considered as valid.
        if (baseFile.isTemporaryFile())
            return true;

        try {
            //Was freshly created?
            if (baseFile.creatingJobsResult != null && baseFile.creatingJobsResult.wasExecuted) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }

        try {
            //Does it exist and is it readable?
            if (result && !baseFile.isFileReadable()) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }

        try {
            //Can it be validated?
            //TODO basefiles are always validated!
            if (result && !baseFile.checkFileValidity()) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }

// If the file is not valid then also temporary parent files should be invalidated! Or at least checked.
        if (!result) {

        }

        return result;
    }


    private static void getFileCompression(ExecutionContext run, List<LaneFile> allLaneFiles) {
        ExecutionService es = ExecutionService.getInstance();
        StringBuilder command = new StringBuilder();
        for (LaneFile lf : allLaneFiles) {
            if (lf.getDecompressionString() == null) {
                command << "&& file -bL ${lf.path.absolutePath}";
//            } else if (lf.getDecompressionString().equals("setgid")) {
//                command << "&& file -bL ${lf.path.absolutePath} | cut -d' ' -f2 ";
            } else {
                command << "&& echo 0 ";
            }
        }

        //TODO Throw or post error that no lane files are available

        ExecutionResult er = es.execute(command[2..-1].toString());
        for (int i = 0; i < er.resultLines.size(); i++) {
            //Look at Matthias ExomePipeline extension. The code is mainly taken from there.
            String dCString = null;
            String dRString = "gzip -c"; //POSSIBLE-ERROR: Not zipper set for 'cat'/ASCII
            String type = er.resultLines[i];
            if (type.startsWith("setgid "))
                type = type[7..-1].trim().split(" ")[0];
            if (type == "0") continue;
            if (type == "gzip") {
                dCString = "gunzip -c";
                dRString = "gzip -c"
            } else if (type == "bzip2") {
                dCString = "bunzip2 -c -k";
                dRString = "bzip2 -c -k";
            } else if (type == "ASCII" || type == "ASCII text") {
                dCString = "cat";
                dRString = "head -n E";
            }
            allLaneFiles[i].setDecompressionString(dCString);
            allLaneFiles[i].setRecompressionString(dRString);
        }
    }

    public static void determineFileDecoder(ExecutionContext context, List<LaneFileGroup> laneFileGroupList) {
        List<LaneFile> allLaneFiles = [];
        for (LaneFileGroup lfg : laneFileGroupList) {
            List<LaneFile> lflist = lfg.getFilesInGroup();
            for (LaneFile lf : lflist) {
                allLaneFiles << lf;
            }
        }
        getFileCompression(context, allLaneFiles);
    }

    public List<Sample> getSamplesForContext(ExecutionContext context) {
        List<Sample> samples = new LinkedList<Sample>();

        def configurationValues = context.getConfiguration().getConfigurationValues()
        boolean extractSamplesFromFastqList = configurationValues.getString("fastq_list", ""); //Evaluates to false automatically.
        boolean extractSamplesFromOutputFiles = configurationValues.getBoolean(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, false);
        boolean enforceAtomicSampleName = configurationValues.getBoolean(FLAG_ENFORCE_ATOMIC_SAMPLE_NAME, false);


        FileSystemInfoProvider fileSystemAccessProvider = FileSystemInfoProvider.getInstance()
        if (extractSamplesFromFastqList) {
            List<String> fastqFiles = configurationValues.getString("fastq_list", "").split(StringConstants.SPLIT_SEMICOLON) as List<String>;
            def sequenceDirectory = configurationValues.get("sequenceDirectory").toFile(context).getAbsolutePath();
            int indexOfSampleID = sequenceDirectory.split(StringConstants.SPLIT_SLASH).findIndexOf { it -> it == '${sample}' }
            samples += fastqFiles.collect {
                it.split(StringConstants.SPLIT_SLASH)[indexOfSampleID]
            }.unique().collect {
                if (Sample.getSampleType(context, it) != Sample.SampleType.UNKNOWN) {
                    return new Sample(context, it)
                } else {
                    logger.warning("Unknown sample type '${it}'")
                    return (List<Sample>) null
                }
            }.findAll {
                it != null
            } as List<Sample>;
        } else if (extractSamplesFromOutputFiles) {
            //TODO etractSamplesFromOutputFiles fails, when no alignment directory is available. Should one fall back to the default method?

            File alignmentDirectory = getAlignmentDirectory(context)
            if (!fileSystemAccessProvider.checkDirectory(alignmentDirectory, context, false)) {
                logger.severe("Cannot retrieve samples from missing directory: " + alignmentDirectory.absolutePath);
                return (List<Sample>) null;
            }
            List<File> filesInDirectory = fileSystemAccessProvider.listFilesInDirectory(alignmentDirectory).sort();

            List<Sample.SampleType> availableTypes = [];
            for (File f : filesInDirectory) {
                String name = f.getName();
                String sampleName = null;
                try {
                    String[] split = name.split(StringConstants.SPLIT_UNDERSCORE);
                    sampleName = split[0];
                    if (!enforceAtomicSampleName && split[1].isInteger() && split[1].length() <= 2)
                        sampleName = split[0..1].join(StringConstants.UNDERSCORE);

                    Sample.SampleType type = Sample.getSampleType(context, sampleName)
                    if (type == Sample.SampleType.UNKNOWN)
                        throw new Exception();
                    if (!availableTypes.contains(type)) {
                        availableTypes << type;
                    }
                } catch (Exception ex) {
                    logger.warning("The sample for file ${f.getAbsolutePath()} could not be determined.");
                }

                if (!samples.find { sample -> sample.name == sampleName })
                    samples << new Sample(context, sampleName);
            }
            if (samples.size() == 0) {
                logger.warning("There were no samples available for dataset ${context.getDataSet().getId()}, extractSamplesFromOutputFiles is set to true, should this value be false?")
            }
        } else {
            List<File> sampleDirs = fileSystemAccessProvider.listDirectoriesInDirectory(context.getInputDirectory());
            for (File sd : sampleDirs) {
                if (Sample.getSampleType(context, sd.getName()) == Sample.SampleType.UNKNOWN) {
                    logger.warning("Skipping directory ${sd.absolutePath}, name is not a known sample type.")
                    continue;
                } else {
                    samples.add(new Sample(context, sd));
                }
            }
        }
        return samples;
    }

    public List<String> getLibrariesForSample(Sample sample) {
        return FileSystemInfoProvider.getInstance().listDirectoriesInDirectory(sample.path).collect { File f -> f.name } as List<String>;
    }

    private File getAlignmentDirectory(ExecutionContext run) {
        String alignmentFolderName = run.getConfiguration().getConfigurationValues().getString(CVALUE_ALIGNMENT_DIRECTORY_NAME, "alignment");
        File alignmentDirectory = getDirectory(alignmentFolderName, run);
        alignmentDirectory
    }

    private File getInpDirectory(String dir, ExecutionContext process, Sample sample, String library = null) {
        Configuration cfg = process.getConfiguration();
        File path = cfg.getConfigurationValues().get(dir).toFile(process);
        String temp = path.getAbsolutePath();
        temp = temp.replace('${dataSet}', process.getDataSet().toString());
        temp = temp.replace('${sample}', sample.getName());
        if(library)
            temp = temp.replace('${library}', library);
        else
            temp = temp.replace('${library}/', "");

        return new File(temp);
    }

    private File getSampleDirectory(ExecutionContext process, Sample sample, String library = null) {
        File sampleDir = getInpDirectory(COConstants.CVALUE_SAMPLE_DIRECTORY, process, sample, library);
        return sampleDir
    }

    private File getSequenceDirectory(ExecutionContext process, Sample sample, String run, String library = null) {
        return new File(getInpDirectory(COConstants.CVALUE_SEQUENCE_DIRECTORY, process, sample, library).getAbsolutePath().replace('${run}', run));
    }

    public List<LaneFileGroup> getLanesForSample(ExecutionContext context, Sample sample, String library = null) {
        ProcessingFlag flag = context.setProcessingFlag(ProcessingFlag.STORE_FILES);
        List<LaneFileGroup> laneFiles = new LinkedList<LaneFileGroup>();

        def configurationValues = context.getConfiguration().getConfigurationValues()
        boolean getLanesFromFastqList = configurationValues.getString("fastq_list", "");
        if (getLanesFromFastqList) {
            // If fastq_list was set via command line or via config file.
            List<File> fastqFiles = configurationValues.getString("fastq_list").split(StringConstants.SPLIT_SEMICOLON).collect { String it -> new File(it); };
            def sequenceDirectory = configurationValues.get(COConstants.CVALUE_SEQUENCE_DIRECTORY).toFile(context).getAbsolutePath();
            int indexOfSampleID = sequenceDirectory.split(StringConstants.SPLIT_SLASH).findIndexOf { it -> it == '${sample}' }
            int indexOfRunID = sequenceDirectory.split(StringConstants.SPLIT_SLASH).findIndexOf { it -> it == '${run}' }

            List<File> listOfFastqFilesForSample = fastqFiles.findAll { it.absolutePath.split(StringConstants.SPLIT_SLASH)[indexOfSampleID] == sample.getName() } as List<File>
            List<String> listOfRunIDs = listOfFastqFilesForSample.collect { it.absolutePath.split(StringConstants.SPLIT_SLASH)[indexOfRunID] }.unique() as List<String>
            listOfRunIDs.each { String runID ->
                List<File> fastqFilesForRun = listOfFastqFilesForSample.findAll { it.absolutePath.split(StringConstants.SPLIT_SLASH)[indexOfRunID] == runID } as List<File>
                List<LaneFileGroup> bundleFiles = QCPipelineScriptFileServiceHelper.bundleFiles(context, sample, runID, fastqFilesForRun);
                laneFiles.addAll(bundleFiles);
            }

        } else {
            // Default case

            File sampleDirectory = getSampleDirectory(context, sample, library);

            loadFastqFilesFromDirectory(sampleDirectory, context, sample, library, laneFiles)
        }
        if (laneFiles.size() == 0) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_NOINPUTDATA.expand("There were no lane files available for sample ${sample.getName()}"));
        } else {
            determineFileDecoder(context, laneFiles);
        }
        context.setProcessingFlag(flag);
        return laneFiles;
    }

    public List<LaneFileGroup> getLanesForSampleAndLibrary(ExecutionContext context, Sample sample, String library) {
        return getLanesForSample(context, sample, library);
    }

    private void loadFastqFilesFromDirectory(File sampleDirectory, ExecutionContext context, Sample sample, String library = null, LinkedList<LaneFileGroup> laneFiles) {
        logger.postAlwaysInfo("Searching for lane files in directory ${sampleDirectory}")
        List<File> runsForSample = FileSystemInfoProvider.getInstance().listDirectoriesInDirectory(sampleDirectory);
        for (File run : runsForSample) {
            File runFilePath = getSequenceDirectory(context, sample, run.getName(), library);
            if(!FileSystemInfoProvider.getInstance().checkDirectory(sequenceDirectory, context, false)) // Skip directories which do not exist
                continue;
            List<File> files = FileSystemInfoProvider.getInstance().listFilesInDirectory(runFilePath);
            if (files.size() == 0)
                logger.postAlwaysInfo("\t There were no lane files in directory ${runFilePath}")
            //Find file bundles
            List<LaneFileGroup> bundleFiles = QCPipelineScriptFileServiceHelper.bundleFiles(context, sample, run.getName(), files);
            laneFiles.addAll(bundleFiles);
        }
    }

    public BamFileGroup getPairedBamFilesForDataSet(ExecutionContext context, Sample sample) {
        File alignmentDirectory = getAlignmentDirectory(context);
        final String pairedBamSuffix = context.getConfiguration().getConfigurationValues().get("pairedBamSuffix", "paired.bam.sorted.bam")
        //TODO Create constants
        List<String> filters = ["${sample.getName()}*${pairedBamSuffix}".toString()]
        List<File> pairedBamPaths = FileSystemInfoProvider.getInstance().listFilesInDirectory(alignmentDirectory, filters);

        int laneID = 0;
        List<BamFile> bamFiles = pairedBamPaths.collect({
            File f ->
                laneID++;
                String name = f.getName();
                String[] split = name.split(StringConstants.SPLIT_UNDERSCORE);
                String sampleName = split[0];
                int runIndex = 1;
                if (split[1].isInteger()) {
                    runIndex = 2;
                    sampleName = split[0..1].join(StringConstants.UNDERSCORE);
                }
                String run = split[runIndex..-2].join(StringConstants.UNDERSCORE);
                String lane = String.format("L%03d", laneID);


                BamFile bamFile = new BamFile(f, context, new COFileStageSettings(lane, run, sample, context.getDataSet()))
                bamFile.setAsSourceFile();
                return bamFile;
        })
        BamFileGroup bamFileGroup = new BamFileGroup(bamFiles);
        logger.info("Found ${bamFileGroup.getFilesInGroup().size()} paired bam files for sample ${sample.getName()}");
        return bamFileGroup;
    }

    public BamFile getMergedBamFileForDataSetAndSample(ExecutionContext context, Sample sample) {
        //TODO Create constants

        def configurationValues = context.getConfiguration().getConfigurationValues()
        final String[] mergedBamSuffixList = configurationValues.get("mergedBamSuffixList", "merged.bam.dupmarked.bam").toString().split(StringConstants.COMMA);
        final boolean useMergedBamsFromInputDirectory = configurationValues.getBoolean("useMergedBamsFromInputDirectory", false);
        final boolean searchMergedBamFilesWithPID = configurationValues.getBoolean("searchMergedBamFilesWithPID", false);


        List<String> filters = [];
        for (String suffix in mergedBamSuffixList) {
            if (!searchMergedBamFilesWithPID) {
                filters += ["${sample.getName()}*${suffix}".toString()
                            , "${sample.getName().toLowerCase()}*${suffix}".toString()
                            , "${sample.getName().toUpperCase()}*${suffix}".toString()]
            } else {
                def dataSetID = context.getDataSet().getId()
                filters += ["${sample.getName()}*${dataSetID}*${suffix}".toString()
                            , "${sample.getName().toLowerCase()}*${dataSetID}*${suffix}".toString()
                            , "${sample.getName().toUpperCase()}*${dataSetID}*${suffix}".toString()]
            }
        }


        List<File> mergedBamPaths;

        File searchDirectory = getAlignmentDirectory(context);
        if (useMergedBamsFromInputDirectory)
            searchDirectory = getInpDirectory(COConstants.CVALUE_ALIGNMENT_INPUT_DIRECTORY_NAME, context, sample);

        synchronized (alreadySearchedMergedBamFolders) {
            if (!alreadySearchedMergedBamFolders.contains(searchDirectory)) {
                logger.postAlwaysInfo("Looking for merged bam files in directory ${searchDirectory.getAbsolutePath()}");
                alreadySearchedMergedBamFolders << searchDirectory;
            }
        }

        mergedBamPaths = FileSystemInfoProvider.getInstance().listFilesInDirectory(searchDirectory, filters);

        List<BamFile> bamFiles = mergedBamPaths.collect({
            File f ->
                String name = f.getName();
                String[] split = name.split(StringConstants.SPLIT_UNDERSCORE);
                int runIndex = 1;
                if (split[1].isInteger()) {
                    runIndex = 2;
                }
                String run = split[runIndex..-2].join(StringConstants.UNDERSCORE);


                BamFile bamFile = new BamFile(f, context, new COFileStageSettings(run, sample, context.getDataSet()))
                bamFile.setAsSourceFile();
                return bamFile;
        })

        if (bamFiles.size() == 1)
            logger.info("\tFound merged bam file ${bamFiles[0].getAbsolutePath()} for sample ${sample.getName()}");
        if (bamFiles.size() > 1) {
            StringBuilder info = new StringBuilder();
            info << "Found more ${bamFiles.size()} merged bam files for sample ${sample.getName()}.\nConsider using option searchMergedBamFilesWithPID=true in your configuration.";
            bamFiles.each { BamFile bamFile -> info << "\t" << bamFile.getAbsolutePath() << "\n"; }

            logger.postAlwaysInfo(info.toString());
            return null;
        }
        if (bamFiles.size() == 0) {
            logger.severe("Found no merged bam file for sample ${sample.getName()}. Please make sure that merged bam files exist or are linked to the alignment folder within the result folder.");
            return null;
        }

        return bamFiles[0];
    }

    public BamFileGroup getMergedBamFilesForDataSet(ExecutionContext context) {

        ProcessingFlag flag = context.setProcessingFlag(ProcessingFlag.STORE_NOTHING);

//        File outputFolderForDataSetAndAnalysis = getOutputFolderForDataSetAndAnalysis(context.getDataSet(), context.getAnalysis());
        BamFileGroup mergedBamFiles = new BamFileGroup();
        Map<Sample.SampleType, CoverageTextFileGroup> coverageTextFilesBySample = new LinkedHashMap<>();

        //Set level to test, set back later.
        ExecutionContextLevel executionContextLevel = context.getExecutionContextLevel();
        ExecutionContextSubLevel detailedExecutionContextLevel = context.getDetailedExecutionContextLevel();
        context.setExecutionContextLevel(ExecutionContextLevel.QUERY_STATUS);
//        context.setExecutionContextLevel(ExecutionContextLevel.QUERY_STATUS);
        getSamplesForContext(context).parallelStream().forEach(new Consumer<Sample>() {
            @Override
            void accept(Sample sample) {
                List<LaneFileGroup> rawSequenceGroups = sample.getLanes();
                BamFileGroup sortedBamFiles = new BamFileGroup();

                for (LaneFileGroup rawSequenceGroup : rawSequenceGroups) {
                    rawSequenceGroup.alignAll();
                    BamFile bamFile = rawSequenceGroup.getAllAlignedFiles().pairAndSort();
                    sortedBamFiles.addFile(bamFile);
                }
                context.setProcessingFlag(ProcessingFlag.STORE_FILES);
                mergedBamFiles.addFile(sortedBamFiles.mergeAndRemoveDuplicates());
                context.setProcessingFlag(ProcessingFlag.STORE_NOTHING);
            }
        });
        context.setExecutionContextLevel(executionContextLevel);
        context.setProcessingFlag(flag);
        return mergedBamFiles;
    }

}
