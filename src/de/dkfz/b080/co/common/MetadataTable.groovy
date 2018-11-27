/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
 */
package de.dkfz.b080.co.common

import de.dkfz.roddy.execution.io.BaseMetadataTable
import groovy.transform.CompileStatic

import static COConstants.*

/**
 * MetadataTable keeps an overview over all factors relevant for guiding the analyses, such as PID, SampleName / TissueType,
 * Library ID, FastQ file, Read Number. Plugins should subclass and override the getMandatoryColumns() and
 * getOptionalColumns() methods.
 */
@CompileStatic
class MetadataTable extends BaseMetadataTable {

    MetadataTable(BaseMetadataTable baseMetadataTable) {
        super(baseMetadataTable)
    }

    private void assertUniqueFastq() {
        // ... what about BAMs?
        Map<String, Integer> tooFrequentFiles = records.countBy {
            it.get(INPUT_TABLE_FILE)
        }.findAll { file, count ->
            count > 1
        }
        if (tooFrequentFiles.size() > 0) {
            throw new RuntimeException("Files occur too often in input table: ${tooFrequentFiles}")
        }
    }

    @Override
    BaseMetadataTable subsetByColumn(String columnName, String value) {
        return new MetadataTable(super.subsetByColumn(columnName, value));
    }

    MetadataTable subsetBySample(String sampleName) {
        return (MetadataTable)subsetByColumn(INPUT_TABLE_SAMPLECOL_NAME, sampleName);
    }

    MetadataTable subsetByRun(String runId) {
        return (MetadataTable)subsetByColumn(INPUT_TABLE_RUNCOL_NAME, runId);
    }

    MetadataTable subsetByLibrary(String library) {
        return (MetadataTable)subsetByColumn(INPUT_TABLE_MARKCOL_NAME, library);
    }

    List<String> listSampleNames() {
        return listColumn(INPUT_TABLE_SAMPLECOL_NAME).unique()
    }

    List<String> listRunIDs() {
        return listColumn(INPUT_TABLE_RUNCOL_NAME).unique()
    }

    List<String> listLibraries() {
        return listColumn(INPUT_TABLE_MARKCOL_NAME).unique()
    }

}
