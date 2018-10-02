/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/COWorkflowsBasePlugin/LICENSE).
 */
package de.dkfz.b080.co.common

/**
 * A static class for the storage of computational oncology based constants.
 */
@groovy.transform.CompileStatic
final class COConstants {

    /**
     * Flags and values for execution control
     */
    public static final String FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES = "extractSamplesFromOutputFiles"
    public static final String FLAG_ENFORCE_ATOMIC_SAMPLE_NAME = "enforceAtomicSampleName"

    /**
     * Parameters for job execution
     */
    public static final String PRM_RAW_SEQ = "RAW_SEQ"
    public static final String PRM_FILENAME_ALIGNMENT = "FILENAME_ALIGNMENT"
    public static final String PRM_RAW_SEQ_1 = "RAW_SEQ_1"
    public static final String PRM_RAW_SEQ_2 = "RAW_SEQ_2"
    public static final String PRM_FILENAME_BAM_INDEX = "FILENAME_BAM_INDEX"
    public static final String PRM_FILENAME_SORTED_BAM = "FILENAME_SORTED_BAM"
    public static final String PRM_FILENAME_SEQ_1 = "FILENAME_SEQ_1"
    public static final String PRM_FILENAME_SEQ_2 = "FILENAME_SEQ_2"
    public static final String PRM_RAW_SEQ_FILE_1_INDEX = "RAW_SEQ_FILE_1_INDEX"
    public static final String PRM_RAW_SEQ_FILE_2_INDEX = "RAW_SEQ_FILE_2_INDEX"
    public static final String PRM_FILENAME_FLAGSTAT = "FILENAME_FLAGSTAT"
    public static final String PRM_RAW_SEQ_JOBJ = "jobj_rawSeq"
    public static final String PRM_CVAL_LIBRARY = "LIB_ADD"
    public static final String PRM_ID = "ID"
    public static final String PRM_PID = "pid"
    public static final String PRM_PID_CAP = "PID"
    public static final String PRM_ANALYSIS_DIR = "ANALYSIS_DIR"
    public static final String PRM_SM = "SM"
    public static final String PRM_LB = "LB"
    /**
     * Configuration values
     */
    public static final String CVALUE_CHROMOSOME_INDICES = "CHROMOSOME_INDICES"
    public static final String CVALUE_AUTOSOME_INDICES = "AUTOSOME_INDICES"
    public static final String CVALUE_SAMPLE_DIRECTORY = "sampleDirectory"
    public static final String CVALUE_ALIGNMENT_INPUT_DIRECTORY_NAME = "alignmentInputDirectory"
    public static final String CVALUE_SEQUENCE_DIRECTORY = "sequenceDirectory"
    public static final String CVALUE_ALIGNMENT_DIRECTORY_NAME = "alignmentOutputDirectory"
    public static final String CVALUE_WINDOW_SIZE = "WINDOW_SIZE"
    public static final String CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES = "possibleControlSampleNamePrefixes"
    public static final String CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES = "possibleTumorSampleNamePrefixes"
    public static final String CVALUE_SEARCH_MERGEDBAM_WITH_SEPARATOR = "searchMergedBamWithSeparator"
    public static final String CVALUE_SELECT_SAMPLE_EXTRACTION_METHOD = "selectSampleExtractionMethod"
    public static final String CVALUE_USE_ATOMIC_SAMPLE_NAMES = "useAtomicSampleNames"
    public static final String CVALUE_ALLOW_SAMPLE_TERMINATION_WITH_INDEX = "allowSampleTerminationWithIndex"
    public static final String CVALUE_MATCH_EXACT_SAMPLE_NAMES = "matchExactSampleNames"
    public static final String CVALUE_FASTQ_LIST = "fastq_list"
    public static final String CVALUE_BAMFILE_LIST = "bamfile_list"
    public static final String CVALUE_SAMPLE_LIST = "sample_list"

    /**
     * Input table column names
     */
    public static final String INPUT_TABLE_SAMPLECOL_NAME = "sampleCol"
    public static final String INPUT_TABLE_MARKCOL_NAME = "markCol"
    public static final String INPUT_TABLE_RUNCOL_NAME = "runCol"
    public static final String INPUT_TABLE_DATASETCOL_NAME = "datasetCol"
    public static final String INPUT_TABLE_READLAYOUTCOL_NAME = "readLayoutCol"
    public static final String INPUT_TABLE_MATECOL_NAME = "mateCol"
    public static final String INPUT_TABLE_FASTQCOL_NAME = "fileCol"

    private COConstants() {
    }
}
