/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/COWorkflowsBasePlugin/LICENSE).
 */
package de.dkfz.b080.co.knowledge.metadata

import de.dkfz.b080.co.common.BasicCOProjectsRuntimeService
import de.dkfz.b080.co.common.COConfig
import de.dkfz.b080.co.common.MetadataTable
import de.dkfz.b080.co.common.RunID
import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.common.COConstants
import de.dkfz.b080.co.files.COFileStageSettings
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.execution.io.MetadataTableFactory
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import de.dkfz.roddy.tools.Tuple2
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import static de.dkfz.b080.co.common.COConstants.*

@CompileStatic
class COMetadataAccessor {

    private static LoggerWrapper logger = LoggerWrapper.getLogger(COMetadataAccessor.class.getName())

    private final static List<File> _alreadySearchedMergedBamFolders = []

    BasicCOProjectsRuntimeService runtimeService

    COMetadataAccessor(BasicCOProjectsRuntimeService runtimeService) {
        this.runtimeService = runtimeService
    }

    MetadataTable getMetadataTable(ExecutionContext context) {
        return new MetadataTable(MetadataTableFactory.getTable(context.getAnalysis()).subsetByDataset(context.getDataSet().id))
    }

    protected List<Sample> extractSamplesFromMetadataTable(ExecutionContext context) {
        return getMetadataTable(context).listColumn(INPUT_TABLE_SAMPLECOL_NAME).unique().collect {
            new Sample(context, it)
        }
    }

    protected List<Sample> extractSamplesFromFastqList(List<File> fastqFiles, ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        return grepPathElementFromFilenames(cfg.getSequenceDirectory(), '${sample}', fastqFiles).unique().collect { Tuple2<String, File> pair ->
            // TODO: Do we want a test here, that two files with the same sample (i.e. both matching the pattern!) come from the same sample directory?
            new Sample(context, pair.x, pair.y)
        }.findAll {
            it != null
        } as List<Sample>

    }

    /**
     * Untested and deprecated, replaced by extractSampleNameFromBamBasenameVersion2
     */
    private String extractSampleNameFromBamBasenameVersion1(String filename, ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        String[] split = filename.split(StringConstants.SPLIT_UNDERSCORE)
        if (split.size() <= 2) {
            return null
        }
        String sampleName = split[0]
        if (!cfg.enforceAtomicSampleName && split[1].isInteger() && split[1].length() <= 2)
            sampleName = split[0..1].join(StringConstants.UNDERSCORE)
        return sampleName
    }

    private String extractSampleNameFromBamBasenameVersion2(File file, ExecutionContext context) {
        COConfig cfg = new COConfig(context)

        // Get list of all known samples. Sort and revert them, so we can use them properly.
        // Get rid of empty samples! Sort and reverse for first match searches. (e.g. control_abc comes before control!)
        def listOfAll = (cfg.possibleControlSampleNamePrefixes + cfg.possibleTumorSampleNamePrefixes)
                .findAll().sort().reverse()

        // TODO Rename this value! Does not fit in the future.
        String searchMergedBamWithSeparator = cfg.searchMergedBamWithSeparator ? "_" : ""

        // FIRST match!
        String sampleName = listOfAll.find { file.name.toLowerCase().startsWith(it + searchMergedBamWithSeparator) }
        if (sampleName == null)
            return file.name.split(StringConstants.SPLIT_UNDERSCORE)[0]

        // As we work with sample prefixes, the sample in the filename can actually be a bit longer than the found value.
        // Count delimiters in the sample name, extract the proper part of the filename and join that again.
        return file.name.split(StringConstants.SPLIT_UNDERSCORE)[0..(sampleName.count("_"))].join("_")
    }

    // TODO: Regex
    String extractSampleNameFromBamBasename(File file, ExecutionContext context) {
        COConfig cfg = new COConfig(context)

        switch (cfg.selectedSampleExtractionMethod) {
            case MethodForSampleFromFilenameExtraction.version_1:
                return extractSampleNameFromBamBasenameVersion1(file.name, context)
            case MethodForSampleFromFilenameExtraction.version_2:
                return extractSampleNameFromBamBasenameVersion2(file, context)
        }
    }

    protected List<Sample> extractSamplesFromOutputFiles(ExecutionContext context) {
        //TODO extractSamplesFromOutputFiles fails, when no alignment directory is available. Should one fall back to the default method?
        COConfig cfg = new COConfig(context)
        FileSystemAccessProvider fileSystemAccessProvider = FileSystemAccessProvider.getInstance()

        File alignmentDirectory = runtimeService.getAlignmentDirectory(context)
        if (!fileSystemAccessProvider.checkDirectory(alignmentDirectory, context, false)) {
            logger.severe("Cannot retrieve samples from missing directory (${COConstants.FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES}=${cfg.getExtractSamplesFromOutputFiles()}): " + alignmentDirectory.absolutePath)
            return (List<Sample>) []
        }
        List<File> filesInDirectory = fileSystemAccessProvider.listFilesInDirectory(alignmentDirectory).sort()

        return filesInDirectory.collect { File file ->
            extractSampleNameFromBamBasename(file, context)
        }.unique().findAll { it != null }.collect {
            new Sample(context, it)
        }
    }

    protected List<Sample> extractSamplesFromSampleDirs(ExecutionContext context) {
        FileSystemAccessProvider fileSystemAccessProvider = FileSystemAccessProvider.getInstance()

        if (!fileSystemAccessProvider.checkDirectory(context.getInputDirectory(), context, false)) {
            logger.severe("Cannot retrieve samples from missing directory (fallback-strategy): " + context.getInputDirectory().getAbsolutePath())
            return (List<Sample>) []
        }
        List<File> sampleDirs = fileSystemAccessProvider.listFilesInDirectory(context.getInputDirectory()).sort()

        return sampleDirs.collect {
            new Sample(context, it)
        }
    }

    protected List<Sample> extractSamplesFromFilenames(List<File> filesInDirectory, ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        LinkedList<Sample> samples = [] as LinkedList
        for (File f : filesInDirectory) {
            String name = f.getName()
            String sampleName = null
            String[] split = name.split(StringConstants.SPLIT_UNDERSCORE)
            sampleName = split[0]
            if (!cfg.getEnforceAtomicSampleName() && split[1].isInteger() && split[1].length() <= 2)
                sampleName = split[0..1].join(StringConstants.UNDERSCORE)
            if (!samples.find { sample -> sample.name == sampleName })
                samples << new Sample(context, sampleName)
        }
        if (samples.size() == 0) {
            logger.warning("There were no samples available for dataset ${context.getDataSet().getId()}, extractSamplesFromOutputFiles is set to true, should this value be false?")
        }
        return samples
    }

    protected List<Sample> extractSamplesFromBamfileListAndSampleList(ExecutionContext context) {
        COConfig cfg = new COConfig(context)
        List<File> bamFiles = cfg.getBamList().collect { String f -> new File(f) }
        List<String> sampleList = cfg.getSampleList()
        if (bamFiles.size() != sampleList.size()) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.
                    expand("Different number of BAM files and samples in ${CVALUE_BAMFILE_LIST} and ${CVALUE_SAMPLE_LIST}"))
            return []
        } else {
            return new IntRange(0, bamFiles.size()).collect { i ->
                new Sample(context, sampleList[i], bamFiles[i])
            }
        }
    }

    List<Sample> getSamples(ExecutionContext context) {
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
            extractedFrom = "${CVALUE_SAMPLE_LIST} configuration value"
        } else if (cfg.fastqFileListIsSet) {
            List<File> fastqFiles = cfg.getFastqList().collect { String f -> new File(f) }
            samples = extractSamplesFromFastqList(fastqFiles, context)
            extractedFrom = "fastq_list configuration value"
        } else if (cfg.getExtractSamplesFromOutputFiles()) {
            samples = extractSamplesFromOutputFiles(context)
            extractedFrom = "output files"
        } else if (cfg.extractSamplesFromBamList) {
            List<File> bamFiles = cfg.getBamList().collect { String f -> new File(f) }
            samples = extractSamplesFromFilenames(bamFiles, context)
            extractedFrom = "${CVALUE_BAMFILE_LIST} configuration value "
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

    List<BasicBamFile> extractBamFilesFromFilesystem(ExecutionContext context) {
        return extractSamplesFromOutputFiles(context).collect { Sample sample ->
            getMergedBamFileFromFilesystem(context, context.dataSet, sample)
        }
    }

    List<BasicBamFile> extractBamFilesFromBamList(ExecutionContext context) {
        // Note, the order extractSamplesFromOutputFiles and extractSamplesFromBamList is the same as in
        // getSamples(). We don't reuse the code there, as there UNKNOWN sample type BAMs
        // are removed and we matching sample and BAM in the bamfile_list branch using array indices and
        // want to keep unclassified samples.
        COConfig cfg = new COConfig(context)
        return cfg.bamList.collect { filename ->
            File file = new File(filename)
            Sample sample = new Sample(context, extractSampleNameFromBamBasename(file, context), file)
            BasicBamFile bamFile = BaseFile.getSourceFile(context, filename, "BasicBamFile") as BasicBamFile
            bamFile.fileStage = new COFileStageSettings(null, null, sample, context.getDataSet())
            bamFile
        }
    }

    List<BasicBamFile> extractBamFilesFromMetadataTable(ExecutionContext context) {
        throw new NotImplementedException()
    }

    /** Find BAM files from whatever source according to the context and configuration. BAMs for all types samples (tumor, control, unknown) are
     *  returned.
     *
     * @param context
     * @return
     */
    List<BasicBamFile> getAllBamFiles(ExecutionContext context) {
        COConfig coConfig = new COConfig(context)
        if (Roddy.isMetadataCLOptionSet())
            return extractBamFilesFromMetadataTable(context)
        else if (coConfig.getExtractSamplesFromOutputFiles())
            return extractBamFilesFromFilesystem(context)
        else if (coConfig.extractSamplesFromBamList)
            return extractBamFilesFromBamList(context)
        else {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("Please set bamfile_list or extractSamplesFromOutputFiles."))
            return null
        }
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
                new Tuple2<>(pathComponents[indexOfElement], RoddyIOHelperMethods.assembleLocalPath("", *pathComponents[0..indexOfElement]))
            }
        }
    }

    BasicBamFile getMergedBamFileFromFilesystem(ExecutionContext context, DataSet dataSet, Sample sample) {
        //TODO Create constants
        COConfig cfg = new COConfig(context)
        // If no dataset is set, the one from the context object is taken.
        if (!dataSet) dataSet = context.getDataSet()

        String searchForDId = cfg.searchMergedBamFilesWithPID ? dataSet.getId() : ""
        String searchWithSeparator = cfg.searchMergedBamWithSeparator ? "_" : ""

        List<String> filters = cfg.mergedBamSuffixList.collect { String suffix ->
            return [
                    sample.getName(),
                    sample.getName().toLowerCase(),
                    sample.getName().toUpperCase()
            ].collect { String sampleName ->
                "${sampleName}${searchWithSeparator}*${searchForDId}*${suffix}".toString()
            }
        }.flatten() as List<String>

        String mergedBamSearchMessage = (([
                "Searching merged bams with:",
                "searchMergedBamFilesWithPID='${searchForDId}'",
                "searchWithSeparator='${searchWithSeparator}'",
                "Searching with the following patterns:'"
        ] as List<String>) + filters).join("\n\t")

        File searchDirectory = runtimeService.getAlignmentDirectory(context);
        if (cfg.useMergedBamsFromInputDirectory)
            searchDirectory = runtimeService.fillTemplatesInPathnameString(CVALUE_ALIGNMENT_INPUT_DIRECTORY_NAME, context, sample)

        synchronized (_alreadySearchedMergedBamFolders) {
            if (!_alreadySearchedMergedBamFolders.contains(searchDirectory)) {
                logger.postAlwaysInfo("Looking for merged bam files in directory ${searchDirectory.getAbsolutePath()}")
                logger.sometimes(mergedBamSearchMessage)
                _alreadySearchedMergedBamFolders << searchDirectory
            }
        }

        List<File> mergedBamPaths = FileSystemAccessProvider.getInstance().listFilesInDirectory(searchDirectory, filters)

        List<BasicBamFile> bamFiles = mergedBamPaths.collect({
            File f ->
                String name = f.getName()
                String[] split = name.split(StringConstants.SPLIT_UNDERSCORE)
                int runIndex = 1
                if (split[1].isInteger()) {
                    runIndex = 2
                }
                RunID run = new RunID(split[runIndex..-2].join(StringConstants.UNDERSCORE))
                BasicBamFile bamFile = new BasicBamFile(new BaseFile.ConstructionHelperForSourceFiles(f, context,
                        new COFileStageSettings(run, null, sample, dataSet), null))
                return bamFile
        })

        if (bamFiles.size() == 1)
            logger.info("\tFound merged bam file ${bamFiles[0].getAbsolutePath()} for sample ${sample.getName()}")
        if (bamFiles.size() > 1) {
            StringBuilder info = new StringBuilder()
            info << "Found ${bamFiles.size()} merged bam files for sample ${sample.getName()}.\nConsider using option searchMergedBamFilesWithPID=true in your configuration."
            bamFiles.each { BasicBamFile bamFile -> info << "\t" << bamFile.getAbsolutePath() << "\n" }

            logger.postAlwaysInfo(info.toString())
            return null
        }
        if (bamFiles.size() == 0) {
            logger.severe(
                    "Found no merged bam file for sample ${sample.getName()}. " +
                            "\n\t" + mergedBamSearchMessage.replace("Searching", "Searched") +
                            "\n\tPlease make sure that merged bam files exist or are linked to the alignment folder within the result folder." +
                            "\n\tPlease also check the bam suffix list:\n\t\t " +
                            cfg.mergedBamSuffixList.join("\n\t\t") +
                            "\n\tIf wrong suffixes are in the list or values are missing, you can change the configuration value 'mergedBamSuffixList'."
            )
            return null

        }

        return bamFiles[0]
    }

    protected List<String> extractLibrariesFromMetadataTable(ExecutionContext context, String sampleName) {
        MetadataTable resultTable = new MetadataTable(getMetadataTable(context).subsetBySample(sampleName))
        assert resultTable.size() > 0
        return resultTable.listLibraries()
    }

    protected List<String> extractLibrariesFromSampleDirectory(File sampleDirectory) {
        return FileSystemAccessProvider.getInstance().listDirectoriesInDirectory(sampleDirectory).collect { File f -> f.name } as List<String>
    }

    List<String> getLibraries(ExecutionContext executionContext, Sample sample) {
        if (Roddy.isMetadataCLOptionSet()) {
            return extractLibrariesFromMetadataTable(executionContext, sample.name)
        } else {
            return extractLibrariesFromSampleDirectory(sample.path)
        }
    }
}
