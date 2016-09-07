/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.COConstants
import de.dkfz.b080.co.files.COFileStageSettings
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.execution.io.MetadataTableFactory
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.JobManager
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.tools.LoggerWrapper

/**
 * This service is mainly for qcpipeline. It might be of use for other projects
 * as well. TODO Think about renaming and extending this as needed.
 *
 * @author michael
 */
//@PluginImplementation
@groovy.transform.CompileStatic
public class BasicCOProjectsRuntimeService extends RuntimeService {

    private static LoggerWrapper logger = LoggerWrapper.getLogger(BasicCOProjectsRuntimeService.class.getName());

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

    public Map<String, Object> getDefaultJobParameters(ExecutionContext context, String toolID) {
        def fs = context.getRuntimeService();
        //File cf = fs..createTemporaryConfigurationFile(executionContext);
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
        return JobManager.getInstance().createJobName(file, toolID, reduceLevel);
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

        // TODO? If the file is not valid then also temporary parent files should be invalidated! Or at least checked.
        if (!result) {
            // Something is missing here! Michael?
        }

        return result;
    }

    protected MetadataTable getMetadataTable(ExecutionContext context) {
        return new MetadataTable(MetadataTableFactory.getTable(context.getAnalysis()).subsetByDataset(context.getDataSet().id));
    }

    public List<Sample> extractSamplesFromMetadataTable(ExecutionContext context) {
        return getMetadataTable(context).listSampleNames().collect {
            new Sample(context, it)
        }
    }

    public List<String> extractLibrariesFromMetadataTable(ExecutionContext context, String sampleName) {
        MetadataTable resultTable = new MetadataTable(getMetadataTable(context).subsetBySample(sampleName))
        assert resultTable.size() > 0
        return resultTable.listLibraries()
    }

    public static int indexOfPathElement(String pathnamePattern, String element) {
        int index = pathnamePattern.split(StringConstants.SPLIT_SLASH).findIndexOf { it -> it == element }
        if (index < 0) {
            throw new RuntimeException("Couldn't match '${element}' in '${pathnamePattern}")
        }
        return index
    }

    public static List<String> grepPathElementFromFilenames(String pathnamePattern, String element, List<File> files) {
        int indexOfElement = indexOfPathElement(pathnamePattern, element)
        return files.collect {
            String[] pathComponents = it.getPath().split(StringConstants.SPLIT_SLASH)
            if (pathComponents.size() <= indexOfElement) {
                throw new RuntimeException("Path to file '${it.getPath()}' too short to match requested path element '${element}' expected at index ${indexOfElement} (${pathnamePattern})")
            } else {
                return pathComponents[indexOfElement]
            }
        }.unique()
    }

    public static List<Sample> extractSamplesFromFastqList(List<File> fastqFiles, ExecutionContext context) {
        COConfig cfg = new COConfig(context);
        return grepPathElementFromFilenames(cfg.getSequenceDirectory(), '${sample}', fastqFiles).collect {
            new Sample(context, it)
        }
    }

    public static String extractSampleNameFromOutputFile(String filename, boolean enforceAtomicSampleName) {
        String[] split = filename.split(StringConstants.SPLIT_UNDERSCORE);
        if (split.size() <= 2) {
            return null
        }
        String sampleName = split[0];
        if (!enforceAtomicSampleName && split[1].isInteger() && split[1].length() <= 2)
            sampleName = split[0..1].join(StringConstants.UNDERSCORE);
        return sampleName
    }

    public List<Sample> extractSamplesFromOutputFiles(ExecutionContext context) {
        //TODO extractSamplesFromOutputFiles fails, when no alignment directory is available. Should one fall back to the default method?
        COConfig cfg = new COConfig(context);
        FileSystemAccessProvider fileSystemAccessProvider = FileSystemAccessProvider.getInstance()

        File alignmentDirectory = getAlignmentDirectory(context)
        if (!fileSystemAccessProvider.checkDirectory(alignmentDirectory, context, false)) {
            logger.severe("Cannot retrieve samples from missing directory: " + alignmentDirectory.absolutePath);
            return (List<Sample>) [];
        }
        List<File> filesInDirectory = fileSystemAccessProvider.listFilesInDirectory(alignmentDirectory).sort();

        return filesInDirectory.collect { File file ->
            extractSampleNameFromOutputFile(file.name, cfg.enforceAtomicSampleName)
        }.unique().findAll { it != null }.collect {
            new Sample(context, it)
        }
    }


    public List<Sample> extractSamplesFromSampleDirs(ExecutionContext context) {
        FileSystemAccessProvider fileSystemAccessProvider = FileSystemAccessProvider.getInstance()

        if (!fileSystemAccessProvider.checkDirectory(context.getInputDirectory(), context, false)) {
            logger.severe("Cannot retrieve samples from missing directory: " + context.getInputDirectory().getAbsolutePath());
            return (List<Sample>) [];
        }
        List<File> sampleDirs = fileSystemAccessProvider.listFilesInDirectory(context.getInputDirectory()).sort();

        return sampleDirs.collect {
            new Sample(context, it)
        }
    }


    public List<String> extractLibrariesFromSampleDirectory(File sampleDirectory) {
        return FileSystemAccessProvider.getInstance().listDirectoriesInDirectory(sampleDirectory).collect { File f -> f.name } as List<String>;
    }

    public static List<Sample> extractSamplesFromFilenames(List<File> filesInDirectory, ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        LinkedList<Sample> samples = [];
        List<Sample.SampleType> availableTypes = [];
        for (File f : filesInDirectory) {
            String name = f.getName();
            String sampleName = null;
            try {
                String[] split = name.split(StringConstants.SPLIT_UNDERSCORE);
                sampleName = split[0];
                if (!cfg.getEnforceAtomicSampleName() && split[1].isInteger() && split[1].length() <= 2)
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
        return samples;
    }

    public List<Sample> getSamplesForContext(ExecutionContext context) {
        // @Michael: I think that COConfig accessors actually belong into the Context itself.
        COConfig cfg = new COConfig(context);
        List<Sample> samples
        String extractedFrom
        if (Roddy.isMetadataCLOptionSet()) {
            samples = extractSamplesFromMetadataTable(context)
            extractedFrom = "input table '${getMetadataTable(context)}'"
        } else if (cfg.extractSamplesFromFastqFileList) {
            List<File> fastqFiles = cfg.getFastqList().collect { String f -> new File(f); }
            samples = extractSamplesFromFastqList(fastqFiles, context)
            extractedFrom = "fastq_list configuration value"
        } else if (cfg.extractSamplesFromOutputFiles) {
            samples = extractSamplesFromOutputFiles(context)
            extractedFrom = "output files"
        } else if (cfg.extractSamplesFromBamList) {
            List<File> bamFiles = cfg.getBamList().collect { String f -> new File(f); }
            samples = extractSamplesFromFilenames(bamFiles, context)
            // @Michael: Should that not better be called "bam_list" in analogy to "fastq_list"?
            extractedFrom = "bamfile_list configuration value "
        } else {
            samples = extractSamplesFromSampleDirs(context)
            extractedFrom = "subdirectories of input directory '${context.inputDirectory}'"
        }

        // Remove unknown samples
        samples.removeAll { Sample sample ->
            sample.sampleType == Sample.SampleType.UNKNOWN
        }
        if (samples.size() == 0) {
            logger.warning("No valid samples could be extracted from ${extractedFrom} for dataset ${context.getDataSet().getId()}.")
        }
        return samples
    }

    protected File getAlignmentDirectory(ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        return getDirectory(cfg.alignmentFolderName, context);
    }

    protected File getInpDirectory(String dir, ExecutionContext process, Sample sample, String library = null) {
        Configuration cfg = process.getConfiguration();
        File path = cfg.getConfigurationValues().get(dir).toFile(process);
        String temp = path.getAbsolutePath();
        temp = temp.replace('${dataSet}', process.getDataSet().toString());
        temp = temp.replace('${sample}', sample.getName());
        if (library)
            temp = temp.replace('${library}', library);
        else
            temp = temp.replace('${library}/', "");

        return new File(temp);
    }

    public File getSampleDirectory(ExecutionContext process, Sample sample, String library = null) {
        File sampleDir = getInpDirectory(COConstants.CVALUE_SAMPLE_DIRECTORY, process, sample, library);
        return sampleDir
    }

    public File getSequenceDirectory(ExecutionContext process, Sample sample, String run, String library = null) {
        return new File(getInpDirectory(COConstants.CVALUE_SEQUENCE_DIRECTORY, process, sample, library).getAbsolutePath().replace('${run}', run));
    }

    public BasicBamFile getMergedBamFileForDataSetAndSample(ExecutionContext context, Sample sample) {
        //TODO Create constants
        COConfig cfg = new COConfig(context)

        List<String> filters = [];
        for (String suffix in cfg.mergedBamSuffixList) {
            if (!cfg.searchMergedBamFilesWithPID) {
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
        if (cfg.useMergedBamsFromInputDirectory)
            searchDirectory = getInpDirectory(COConstants.CVALUE_ALIGNMENT_INPUT_DIRECTORY_NAME, context, sample);

        synchronized (alreadySearchedMergedBamFolders) {
            if (!alreadySearchedMergedBamFolders.contains(searchDirectory)) {
                logger.postAlwaysInfo("Looking for merged bam files in directory ${searchDirectory.getAbsolutePath()}");
                alreadySearchedMergedBamFolders << searchDirectory;
            }
        }

        mergedBamPaths = FileSystemAccessProvider.getInstance().listFilesInDirectory(searchDirectory, filters);

        List<BasicBamFile> bamFiles = mergedBamPaths.collect({
            File f ->
                String name = f.getName();
                String[] split = name.split(StringConstants.SPLIT_UNDERSCORE);
                int runIndex = 1;
                if (split[1].isInteger()) {
                    runIndex = 2;
                }
                String run = split[runIndex..-2].join(StringConstants.UNDERSCORE);

//                                    BaseFile.ConstructionHelperForSourceFiles.construct(BasicBamFile, f, context, new COFileStageSettings(run, sample, context.getDataSet()));
                BasicBamFile bamFile = new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(f, context, new COFileStageSettings(run, sample, context.getDataSet()), null));
                return bamFile;
        })

        if (bamFiles.size() == 1)
            logger.info("\tFound merged bam file ${bamFiles[0].getAbsolutePath()} for sample ${sample.getName()}");
        if (bamFiles.size() > 1) {
            StringBuilder info = new StringBuilder();
            info << "Found more ${bamFiles.size()} merged bam files for sample ${sample.getName()}.\nConsider using option searchMergedBamFilesWithPID=true in your configuration.";
            bamFiles.each { BasicBamFile bamFile -> info << "\t" << bamFile.getAbsolutePath() << "\n"; }

            logger.postAlwaysInfo(info.toString());
            return null;
        }
        if (bamFiles.size() == 0) {
            logger.severe("Found no merged bam file for sample ${sample.getName()}. Please make sure that merged bam files exist or are linked to the alignment folder within the result folder.");
            return null;
        }

        return bamFiles[0];
    }


}
