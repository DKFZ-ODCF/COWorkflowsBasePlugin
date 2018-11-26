/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/COWorkflowsBasePlugin/LICENSE).
 */

package de.dkfz.b080.co.common

import de.dkfz.b080.co.knowledge.metadata.MethodForSampleFromFilenameExtraction
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.ConfigurationError
import de.dkfz.roddy.config.RecursiveOverridableMapContainerForConfigurationValues
import de.dkfz.roddy.core.ExecutionContext
import static COConstants.*

@groovy.transform.CompileStatic
class COConfig {

    public ExecutionContext context

    COConfig(ExecutionContext context) {
        this.context = context
    }

    // This is used so often, it should maybe be part of ExecutionContext.
    RecursiveOverridableMapContainerForConfigurationValues getConfigValues() {
        return context.getConfiguration().getConfigurationValues()
    }

    void setConfig(String flagName, String value, String typeName) {
        configValues.put(flagName, value, typeName)
    }

    boolean getFastqFileListIsSet() {
        return !getFastqList().isEmpty()
    }

    boolean getMetadataTableIsSet() {
        return Roddy.isMetadataCLOptionSet()
    }

    boolean getExtractSamplesFromOutputFiles(boolean defaultValue = false) {
        return configValues.getBoolean(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, defaultValue)
    }

    boolean getEnforceAtomicSampleName() {
        return configValues.getBoolean(FLAG_ENFORCE_ATOMIC_SAMPLE_NAME, false)
    }

    boolean getExtractSamplesFromBamList() {
        return !getBamList().isEmpty()
    }

    private List<String> checkAndSplitListFromConfig(String listID) {
        String list = configValues.getString(listID, null)
        if (list)
            return list.split(StringConstants.SPLIT_SEMICOLON) as List<String>
        return []
    }

    List<String> getFastqList() {

        return checkAndSplitListFromConfig(CVALUE_FASTQ_LIST)
    }

    List<String> getBamList() {
        return checkAndSplitListFromConfig(CVALUE_BAMFILE_LIST)
    }

    List<String> getSampleList() {
        return checkAndSplitListFromConfig(CVALUE_SAMPLE_LIST)
    }

    String getSequenceDirectory() {
        return configValues.get(COConstants.CVALUE_SEQUENCE_DIRECTORY).toFile(context).getAbsolutePath()
    }

    String getAlignmentFolderName() {
        return configValues.getString(CVALUE_ALIGNMENT_DIRECTORY_NAME, "alignment")
    }

    String[] getMergedBamSuffixList() {
        return configValues.get("mergedBamSuffixList", "merged.bam.dupmarked.bam,merged.mdup.bam,merged.bam.rmdup.bam").toString().split(StringConstants.COMMA)
    }

    boolean getUseMergedBamsFromInputDirectory() {
        return configValues.getBoolean("useMergedBamsFromInputDirectory", false)
    }

    boolean getSearchMergedBamFilesWithPID() {
        return configValues.getBoolean("searchMergedBamFilesWithPID", false)
    }

    boolean getSearchMergedBamWithSeparator() {
        return configValues.getBoolean("searchMergedBamWithSeparator", false)
    }

    List<String> getPossibleControlSampleNamePrefixes() {
        return configValues.get(COConstants.CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES, "( control )").toStringList(" ", ["(", ")"] as String[])
    }

    List<String> getPossibleTumorSampleNamePrefixes() {
        return configValues.get(COConstants.CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES, "( tumor )").toStringList(" ", ["(", ")"] as String[])
    }

    MethodForSampleFromFilenameExtraction getSelectedSampleExtractionMethod() {
        try {
            String value = configValues.get(COConstants.CVALUE_SELECT_SAMPLE_EXTRACTION_METHOD)
            return value as MethodForSampleFromFilenameExtraction
        } catch (Exception ex) {
            throw new ConfigurationError(
                    [
                            "Value for selectSampleExtractionMethod is wrong, needs to be one of:",
                            MethodForSampleFromFilenameExtraction.values()
                    ].flatten().join("\n\t- "), context.configuration)
        }
    }

    /**
     * For sample extraction method version 2
     * @return
     */
    boolean getMatchExactSampleNames() {
        return configValues.getBoolean(CVALUE_MATCH_EXACT_SAMPLE_NAMES, false)
    }

    /**
     * For sample extraction method version 2
     * @return
     */
    boolean getAllowSampleTerminationWithIndex() {
        return configValues.getBoolean(CVALUE_ALLOW_SAMPLE_TERMINATION_WITH_INDEX, true)
    }

    boolean getUseLowerCaseFilenamesForSampleExtraction() {
        return configValues.getBoolean(CVALUE_USE_LOWER_CASE_FILENAMES_FOR_SAMPLE_EXTRACTION, true)
    }

    boolean getExtractSampleNameOnlyFromBamFiles() {
        return configValues.getBoolean(CVALUE_EXTRACT_SAMPLE_NAME_ONLY_FROM_BAM_FILES, false)
    }
}
