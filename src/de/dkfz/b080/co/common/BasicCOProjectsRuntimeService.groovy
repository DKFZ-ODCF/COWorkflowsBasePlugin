/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
*/

package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.COConstants
import de.dkfz.b080.co.files.COFileStageSettings
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.execution.io.MetadataTableFactory
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.JobManager
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import de.dkfz.roddy.tools.Tuple2
import groovy.transform.CompileDynamic

/**
 * This service is mainly for qcpipeline. It might be of use for other projects
 * as well. TODO Think about renaming and extending this as needed.
 *
 * @author michael
 */
//@PluginImplementation
@groovy.transform.CompileStatic
class BasicCOProjectsRuntimeService extends RuntimeService {

    private static LoggerWrapper logger = LoggerWrapper.getLogger(BasicCOProjectsRuntimeService.class.getName());

    private static List<File> alreadySearchedMergedBamFolders = [];

    Map<String, Object> getDefaultJobParameters(ExecutionContext context, String toolID) {
        def fs = context.getRuntimeService();
        //File cf = fs..createTemporaryConfigurationFile(executionContext);
        String pid = context.getDataSet().toString()
        Map<String, Object> parameters = [
                (COConstants.PRM_PID)         : (Object) pid,
                (COConstants.PRM_PID_CAP)     : pid,
                (COConstants.PRM_ANALYSIS_DIR): context.getOutputDirectory().getParentFile().getParent()
        ]
        return parameters
    }

    @Deprecated
    @Override
    String createJobName(ExecutionContext executionContext, BaseFile file, String toolID, boolean reduceLevel) {
        return JobManager.getInstance().createJobName(file, toolID, reduceLevel);
    }

    protected MetadataTable getMetadataTable(ExecutionContext context) {
        return new MetadataTable(MetadataTableFactory.getTable(context.getAnalysis()).subsetByDataset(context.getDataSet().id));
    }

    List<Sample> extractSamplesFromMetadataTable(ExecutionContext context) {
        return getMetadataTable(context).listSampleNames().collect {
            new Sample(context, it)
        }
    }

    List<String> extractLibrariesFromMetadataTable(ExecutionContext context, String sampleName) {
        MetadataTable resultTable = new MetadataTable(getMetadataTable(context).subsetBySample(sampleName))
        assert resultTable.size() > 0
        return resultTable.listLibraries()
    }

    /** This is similar to RoddyIOHelperMethods.getPatternVariableFromPath() but does not check that the leading path elements before the component
     *  match are identical in the files and the pathnamePattern. Instead a list of match-tuples -- (matched element value, prefix up to match) --
     *  is returned.
     *
     *  TODO: This is actually not a nice approach to get the sample for the FASTQ. Why match only a specific component and allow differences for
     *        the rest (unless we'd really implement a wildcard path element like "*" in filename patterns. Rethink the matching strategy.
     *        It should probably not get applied to fastq_list or bam[file]_list, but really *only* when getting files from the command line. Sample
     *        names (metadata) should be provided by other means (extra parameters, metadata table/xml, ...)
     *
     * @param pathnamePattern
     * @param element
     * @param files
     * @return
     */
    @CompileDynamic
    static List<Tuple2<String, File>> grepPathElementFromFilenames(String pathnamePattern, String element, List<File> files) {
        assert pathnamePattern.startsWith("/")   // The pathname needs to be an absolute path!
        int indexOfElement = RoddyIOHelperMethods.findComponentIndexInPath(pathnamePattern, element).
                orElseThrow { new RuntimeException("Couldn't match '${element}' in '${pathnamePattern}") }

        return files.collect {
            List<String> pathComponents = RoddyIOHelperMethods.splitPathname(it.getAbsolutePath()).toList()
            if (pathComponents.size() <= indexOfElement) {
                throw new RuntimeException("Path to file '${it.getPath()}' too short to match requested path element '${element}' expected at index ${indexOfElement} (${pathnamePattern})")
            } else {
                new Tuple2<>(pathComponents[indexOfElement], RoddyIOHelperMethods.assembleLocalPath("", *pathComponents[0 .. indexOfElement]))
            }
        }
    }

    static List<Sample> extractSamplesFromFastqList(List<File> fastqFiles, ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        return grepPathElementFromFilenames(cfg.getSequenceDirectory(), '${sample}', fastqFiles).unique().collect { Tuple2<String, File> pair ->
            // TODO: Do we want a test here, that two files with the same sample (i.e. both matching the pattern!) come from the same sample directory?
            new Sample(context, pair.x, pair.y)
        }.findAll {
            it != null
        } as List<Sample>

    }

    static String extractSampleNameFromOutputFile(String filename, boolean enforceAtomicSampleName) {
        String[] split = filename.split(StringConstants.SPLIT_UNDERSCORE)
        if (split.size() <= 2) {
            return null
        }
        String sampleName = split[0];
        if (!enforceAtomicSampleName && split[1].isInteger() && split[1].length() <= 2)
            sampleName = split[0..1].join(StringConstants.UNDERSCORE)
        return sampleName
    }

    List<Sample> extractSamplesFromOutputFiles(ExecutionContext context) {
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

    List<Sample> extractSamplesFromSampleDirs(ExecutionContext context) {
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


    List<String> extractLibrariesFromSampleDirectory(File sampleDirectory) {
        return FileSystemAccessProvider.getInstance().listDirectoriesInDirectory(sampleDirectory).collect { File f -> f.name } as List<String>;
    }

    static List<Sample> extractSamplesFromFilenames(List<File> filesInDirectory, ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        LinkedList<Sample> samples = [] as LinkedList;
        for (File f : filesInDirectory) {
            String name = f.getName();
            String sampleName = null;
            String[] split = name.split(StringConstants.SPLIT_UNDERSCORE);
            sampleName = split[0];
            if (!cfg.getEnforceAtomicSampleName() && split[1].isInteger() && split[1].length() <= 2)
                sampleName = split[0..1].join(StringConstants.UNDERSCORE);
            if (!samples.find { sample -> sample.name == sampleName })
                samples << new Sample(context, sampleName);
        }
        if (samples.size() == 0) {
            logger.warning("There were no samples available for dataset ${context.getDataSet().getId()}, extractSamplesFromOutputFiles is set to true, should this value be false?")
        }
        return samples;
    }

    List<Sample> extractSamplesFromBamfileListAndSampleList(ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        List<File> bamFiles = cfg.getBamList().collect { String f -> new File(f) }
        List<String> sampleList = cfg.getSampleList()
        if (bamFiles.size() != sampleList.size()) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.
                    expand("Different number of BAM files and samples in ${COConstants.CVALUE_BAMFILE_LIST} and ${COConstants.CVALUE_SAMPLE_LIST}"))
            return []
        } else {
            return new IntRange(0, bamFiles.size()).collect { i ->
                new Sample(context, sampleList[i], bamFiles[i])
            }
        }
    }

    List<Sample> getSamplesForContext(ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        List<Sample> samples
        String extractedFrom
        List<String> samplesPassedInConfig = cfg.getSampleList()

        if (Roddy.isMetadataCLOptionSet()) {
            samples = extractSamplesFromMetadataTable(context)
            extractedFrom = "input table '${getMetadataTable(context)}'"
        } else if (samplesPassedInConfig) {
            logger.postSometimesInfo("Samples were passed as configuration value: ${samplesPassedInConfig}")
            samples = samplesPassedInConfig.collect { String it -> new Sample(context, it) }
            extractedFrom = "${COConstants.CVALUE_SAMPLE_LIST} configuration value"
        } else if (cfg.fastqFileListIsSet) {
            List<File> fastqFiles = cfg.getFastqList().collect { String f -> new File(f) }
            samples = extractSamplesFromFastqList(fastqFiles, context)
            extractedFrom = "fastq_list configuration value"
        } else if (cfg.getExtractSamplesFromOutputFiles()) {
            samples = extractSamplesFromOutputFiles(context)
            extractedFrom = "output files"
        } else if (cfg.extractSamplesFromBamList) {
            List<File> bamFiles = cfg.getBamList().collect { String f -> new File(f); }
            samples = extractSamplesFromFilenames(bamFiles, context)
            extractedFrom = "${COConstants.CVALUE_BAMFILE_LIST} configuration value "
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

    /** Find BAM files from whatever source according to the context and configuration. BAMs for all types samples (tumor, control, unknown) are
     *  returned.
     *
     * @param context
     * @return
     */
    List<BasicBamFile> getAllBamFiles(ExecutionContext context) {
        DataSet dataSet = context.getDataSet()
        COConfig coConfig = new COConfig(context)

        List<BasicBamFile> allFound = []
        // Note, the order extractSamplesFromOutputFiles and extractSamplesFromBamList is the same as in
        // getSamples(). We don't reuse the code there, as there UNKNOWN sample type BAMs
        // are removed and we matching sample and BAM in the bamfile_list branch using array indices and
        // want to keep unclassified samples.
        if (coConfig.getExtractSamplesFromOutputFiles()) {
            for (Sample sample : extractSamplesFromOutputFiles(context))
                allFound.add(getMergedBamFileFromFilesystem(context, null, sample))

        } else if (coConfig.extractSamplesFromBamList) {
            List<File> bamFiles = coConfig.getBamList().collect { String f -> new File(f) }
            List<Sample> samples = extractSamplesFromFilenames(bamFiles, context)
            for (int i = 0; i < bamFiles.size(); i++)
                allFound.add(new BasicBamFile(new BaseFile.
                        ConstructionHelperForSourceFiles(bamFiles[i], context, new COFileStageSettings(samples[i], dataSet), null)))

        } else {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("Please set bamfile_list or extractSamplesFromOutputFiles."))
        }

        return allFound
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

    File getSampleDirectory(ExecutionContext process, Sample sample, String library = null) {
        File sampleDir = getInpDirectory(COConstants.CVALUE_SAMPLE_DIRECTORY, process, sample, library);
        return sampleDir
    }

    File getSequenceDirectory(ExecutionContext process, Sample sample, String run, String library = null) {
        return new File(getInpDirectory(COConstants.CVALUE_SEQUENCE_DIRECTORY, process, sample, library).getAbsolutePath().replace('${run}', run));
    }

    BasicBamFile getMergedBamFileFromFilesystem(ExecutionContext context, DataSet dataSet, Sample sample) {
        //TODO Create constants
        COConfig cfg = new COConfig(context)
        // If no dataset is set, the one from the context object is taken.
        if (!dataSet) dataSet = context.getDataSet()

        List<String> filters = [];
        for (String suffix in cfg.mergedBamSuffixList) {
            if (!cfg.searchMergedBamFilesWithPID) {
                filters += ["${sample.getName()}*${suffix}".toString()
                            , "${sample.getName().toLowerCase()}*${suffix}".toString()
                            , "${sample.getName().toUpperCase()}*${suffix}".toString()]
            } else {
                def dataSetID = dataSet.getId()
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
                RunID run = new RunID(split[runIndex..-2].join(StringConstants.UNDERSCORE));
                BasicBamFile bamFile = new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(f, context, new COFileStageSettings(run, null, sample, dataSet), null));
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
            logger.severe(
                    "Found no merged bam file for sample ${sample.getName()}. " +
                            "\n\tPlease make sure that merged bam files exist or are linked to the alignment folder within the result folder." +
                            "\n\tPlease also check the bam suffix list:\n\t\t " +
                            cfg.mergedBamSuffixList.join("\n\t\t") +
                            "\n\tIf wrong suffixes are in the list or values are missing, you can change the configuration value 'mergedBamSuffixList'."
            );
            return null;
        }

        return bamFiles[0];
    }

}
