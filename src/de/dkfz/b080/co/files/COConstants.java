/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.files;

/**
 * A static class for the storage of computational oncology based constants.
 */
@groovy.transform.CompileStatic
public final class COConstants {
    /**
     * Tool entries
     */
    public static final String TOOL_ALIGNMENT = "alignment";
    public static final String TOOL_ACCELERATED_ALIGNMENT = "accelerated:alignment";
    public static final String TOOL_COLLECT_BAM_METRICS = "collectBamMetrics";
    public static final String TOOL_SAMPESORT = "sampesort";
    public static final String TOOL_ALIGNANDPAIR = "alignAndPair";
    public static final String TOOL_ALIGNANDPAIR_SLIM = "alignAndPairSlim";
    public static final String TOOL_ACCELERATED_ALIGNANDPAIR = "accelerated:alignAndPair";
    public static final String TOOL_ACCELERATED_ALIGNANDPAIR_SLIM = "accelerated:alignAndPairSlim";
    public static final String TOOL_SAMTOOLS_INDEX = "samtoolsIndex";
    public static final String TOOL_SAMTOOLS_FLAGSTAT = "samtoolsFlagstat";
    public static final String TOOL_FILTER_VCF_FILES = "indelVcfFilter";
    public static final String TOOL_SNV_CALLING = "snvCalling";
    public static final String TOOL_JOIN_SNV_VCF_FILES = "snvJoinVcfFiles";
    public static final String TOOL_SNV_ANNOTATION = "snvAnnotation";
    public static final String TOOL_SNV_FILTER = "snvFilter";
    public static final String TOOL_SNV_DEEP_ANNOTATION = "snvDeepAnnotation";
    public static final String TOOL_INDEL_CALLING = "indelCalling";
    public static final String TOOL_INDEL_ANNOTATION = "indelAnnotation";
    public static final String TOOL_INDEL_DEEP_ANNOTATION = "indelDeepAnnotation";
    public static final String TOOL_PURITY_ESTIMATION = "purityEstimation";
    public static final String TOOL_PURITY_ESTIMATION_THETA = "purityEstimationTheta";
    public static final String TARGET_EXTRACTION_AND_COVERAGE_SLIM = "targetExtractCoverageSlim";

    /**
     * Flags and values for execution control
     */
    public static final String FLAG_USE_ACCELERATED_HARDWARE = "useAcceleratedHardware";
    public static final String FLAG_USE_BIOBAMBAM_SORT = "useBioBamBamSort";
    public static final String FLAG_USE_BIOBAMBAM_MARK_DUPLICATES = "useBioBamBamMarkDuplicates";
    public static final String FLAG_USE_ADAPTOR_TRIMMING = "useAdaptorTrimming";
    public static final String FLAG_USE_EXISTING_PAIRED_BAMS = "useExistingPairedBams";
    public static final String FLAG_USE_EXISTING_MERGED_BAMS = "useExistingMergedBams";
    public static final String FLAG_USE_MBUFFER_STREAMING = "useMBufferStreaming";
    public static final String FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES = "extractSamplesFromOutputFiles";
    public static final String FLAG_ENFORCE_ATOMIC_SAMPLE_NAME = "enforceAtomicSampleName";

    public static final String FLAG_RUN_FASTQC = "runFastQC";
    public static final String FLAG_RUN_FASTQC_ONLY = "runFastQCOnly";

    public static final String FLAG_RUN_ALIGNMENT_ONLY = "runAlignmentOnly";

    public static final String FLAG_ALIGNMENT_PROCESSING_OPTIONS = "alignmentProcessingOptions";
    public static final String FLAG_RUN_COVERAGE_PLOTS = "runCoveragePlots";
    public static final String FLAG_RUN_COVERAGE_PLOTS_ONLY = "runCoveragePlotsOnly";
    public static final String FLAG_RUN_EXOME_ANALYSIS = "runExomeAnalysis";
    public static final String FLAG_RUN_SNP_COMPARISON = "runSNPComparison";
    public static final String FLAG_RUN_COLLECT_BAMFILE_METRICS = "runCollectBamFileMetrics";
    public static final String FLAG_IS_DEBUG_CONFIGURATION = "isDebugConfiguration";
    public static final String FLAG_RUN_SLIM_WORKFLOW = "runSlimWorkflow";

    public static final String FLAG_USE_COMBINED_ALIGN_AND_SAMPE = "useCombinedAlignAndSampe";
    public static final String FLAG_USE_SINGLE_END_PROCESSING = "useSingleEndProcessing";
    /**
     * Parameters for job execution
     */
    public static final String PRM_RAW_SEQ = "RAW_SEQ";
    public static final String PRM_RAW_SEQ2 = "RAW_SEQ2";
    public static final String PRM_FILENAME_ALIGNMENT = "FILENAME_ALIGNMENT";
    public static final String PRM_STREAM_BUFFER_PORTEXCHANGE = "STREAM_BUFFER_PORTEXCHANGE";
    public static final String PRM_LOCKFILE = "LOCKFILE";
    public static final String PRM_RAW_SEQ_1 = "RAW_SEQ_1";
    public static final String PRM_RAW_SEQ_2 = "RAW_SEQ_2";
    public static final String PRM_FILENAME_BAM_INDEX = "FILENAME_BAM_INDEX";
    public static final String PRM_FILENAME_SORTED_BAM = "FILENAME_SORTED_BAM";
    public static final String PRM_FILENAME_SEQ_1 = "FILENAME_SEQ_1";
    public static final String PRM_FILENAME_SEQ_2 = "FILENAME_SEQ_2";
    public static final String PRM_RAW_SEQ_FILE_1_INDEX = "RAW_SEQ_FILE_1_INDEX";
    public static final String PRM_RAW_SEQ_FILE_2_INDEX = "RAW_SEQ_FILE_2_INDEX";
    public static final String PRM_FILENAME_FLAGSTAT = "FILENAME_FLAGSTAT";
    public static final String PRM_STREAM_BUFFER_PORTEXCHANGE_0 = "STREAM_BUFFER_PORTEXCHANGE_0";
    public static final String PRM_STREAM_BUFFER_PORTEXCHANGE_1 = "STREAM_BUFFER_PORTEXCHANGE_1";
    public static final String PRM_RAW_SEQ_JOBJ = "jobj_rawSeq";
    public static final String PRM_CVAL_LIBRARY = "LIB_ADD";
    public static final String PRM_ID = "ID";
    public static final String PRM_PID = "pid";
    public static final String PRM_PID_CAP = "PID";
    public static final String PRM_ANALYSIS_DIR = "ANALYSIS_DIR";
    public static final String PRM_CONFIG_FILE= "CONFIG_FILE";

    public static final String PRM_SM = "SM";
    public static final String PRM_LB = "LB";
    /**
     * Configuration values
     */
    public static final String CVALUE_CHROMOSOME_INDICES = "CHROMOSOME_INDICES";
    public static final String CVALUE_AUTOSOME_INDICES = "AUTOSOME_INDICES";
    public static final String CVALUE_SAMPLE_DIRECTORY = "sampleDirectory";
    public static final String CVALUE_ALIGNMENT_INPUT_DIRECTORY_NAME = "alignmentInputDirectory";
    public static final String CVALUE_SEQUENCE_DIRECTORY = "sequenceDirectory";
    public static final String CVALUE_ALIGNMENT_DIRECTORY_NAME = "alignmentOutputDirectory";
    public static final String CVALUE_WINDOW_SIZE = "WINDOW_SIZE";
    public static final String CVALUE_MARK_DUPLICATES_VARIANT = "markDuplicatesVariant";
    public static final String CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES = "possibleControlSampleNamePrefixes";
    public static final String CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES = "possibleTumorSampleNamePrefixes";

    /**
     * Input table column names
     */
    public static final String INPUT_TABLE_MERGECOL_NAME = "mergeCol";
    public static final String INPUT_TABLE_MARKCOL_NAME = "markCol";
    public static final String INPUT_TABLE_RUNCOL_NAME = "runCol";
    public static final String INPUT_TABLE_DATASETCOL_NAME = "datasetCol";
    public static final String INPUT_TABLE_READLAYOUTCOL_NAME = "readLayoutCol";
    public static final String INPUT_TABLE_MATECOL_NAME = "mateCol";
    public static final String INPUT_TABLE_FILECOL_NAME = "fileCol";

    private COConstants() {
    }
}
