package de.dkfz.b080.co.common

import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.RecursiveOverridableMapContainerForConfigurationValues
import de.dkfz.roddy.core.ExecutionContext
import static de.dkfz.b080.co.files.COConstants.*

@groovy.transform.CompileStatic
class COConfig {

    public ExecutionContext context

    public COConfig(ExecutionContext context) {
        this.context = context
    }

    // This is used so often, it should maybe be part of ExecutionContext.
    public RecursiveOverridableMapContainerForConfigurationValues getConfigValues() {
        return context.getConfiguration().getConfigurationValues()
    }

    public setConfig(String flagName, String value, String typeName) {
        configValues.put(flagName, value, typeName)
    }

    public boolean getExtractSamplesFromFastqFileList() {
        return !getFastqList().isEmpty()
    }

    public boolean getExtractSamplesFromOutputFiles() {
        return configValues.getBoolean(FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES, false)
    }

    public boolean getEnforceAtomicSampleName() {
        return configValues.getBoolean(FLAG_ENFORCE_ATOMIC_SAMPLE_NAME, false)
    }

    public boolean getExtractSamplesFromBamList() {
        return !getBamList().isEmpty()
    }

    public List<String> getFastqList() {
        return configValues.getString("fastq_list", "").split(StringConstants.SPLIT_SEMICOLON) as List<String>
    }

    public List<String> getBamList() {
        return configValues.getString("bamfile_list", "").split(StringConstants.SPLIT_SEMICOLON) as List<String>
    }

    public String getSequenceDirectory() {
        return configValues.get("sequenceDirectory").toFile(context).getAbsolutePath()
    }

    public String getAlignmentFolderName() {
        return configValues.getString(CVALUE_ALIGNMENT_DIRECTORY_NAME, "alignment")
    }

    public String[] getMergedBamSuffixList() {
        return configValues.get("mergedBamSuffixList", "merged.bam.dupmarked.bam").toString().split(StringConstants.COMMA)
    }

    public boolean getUseMergedBamsFromInputDirectory() {
        return configValues.getBoolean("useMergedBamsFromInputDirectory", false)
    }

    public boolean getSearchMergedBamFilesWithPID() {
        return configValues.getBoolean("searchMergedBamFilesWithPID", false)
    }

    public List<String> getPossibleControlSampleNamePrefixes() {
        return configValues.get("possibleControlSampleNamePrefixes").toStringList(" ", ["(", ")"] as String[])
    }

    public List<String> getPossibleTumorSampleNamePrefixes() {
        return configValues.get("possibleTumorSampleNamePrefixes").toStringList(" ", ["(", ")"] as String[])
    }

}
