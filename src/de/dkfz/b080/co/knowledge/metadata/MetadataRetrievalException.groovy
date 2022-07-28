package de.dkfz.b080.co.knowledge.metadata

import de.dkfz.b080.co.common.COConfig
import de.dkfz.b080.co.common.COConstants

class MetadataRetrievalException extends Exception {

    boolean metadatCliOption
    COConfig configuration

    MetadataRetrievalException(boolean metadataCliOption,
                               COConfig configuration,
                               Exception exception,
                               boolean enableSuppression = false,
                               boolean writableStackTrace = false) {
        super("Could not obtain metadata", exception, enableSuppression, writableStackTrace)
        self.metadataCliOption = metadataCliOption
        self.configuration = configuration
    }

    @Override
    String getMessage() {
        return "Could not obtain metadata (" + [
                "metadatatable=$metadatCliOption",
                "${COConstants.CVALUE_SAMPLE_LIST}=${configuration.sampleList}",
                "${COConstants.CVALUE_FASTQ_LIST}=${configuration.fastqList}",
                "${COConstants.FLAG_EXTRACT_SAMPLES_FROM_OUTPUT_FILES}=${configuration.extractSamplesFromOutputFiles}",
                "${COConstants.CVALUE_EXTRACT_SAMPLE_NAME_ONLY_FROM_BAM_FILES}=${configuration.extractSamplesFromBamList}"
        ].join(", ") + ")"

    }

}
