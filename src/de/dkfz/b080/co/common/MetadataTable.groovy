package de.dkfz.b080.co.common

import de.dkfz.roddy.execution.io.BaseMetadataTable
import groovy.transform.CompileStatic

import static de.dkfz.b080.co.files.COConstants.*

/**
 * MetadataTable keeps an overview over all factors relevant for guiding the analyses, such as PID, SampleName / TissueType,
 * Library ID, FastQ file, Read Number. Plugins should subclass and override the getMandatoryColumns() and
 * getOptionalColumns() methods.
 */
@CompileStatic
public class MetadataTable  extends BaseMetadataTable<MetadataTable> {

    public MetadataTable(BaseMetadataTable BaseMetadataTable) {
        this(BaseMetadataTable.headerMap, BaseMetadataTable.records);
    }

    public MetadataTable(Map<String, Integer> newHeaderMap, List<Map<String, String>> newTable) {
        super(newHeaderMap, newTable)
    }

    @Override
    protected List<String> _getAdditionalMandatoryColumnNames() {
        return [
                INPUT_TABLE_SAMPLE_NAME,     // e.g. tumor, control
                INPUT_TABLE_LIBRARY,         // library identifier
                INPUT_TABLE_RUN_ID,          // e.g. run150626_ST-E00204_0045_AH5MK5CCXX
                INPUT_TABLE_READ_NUMBER,     // 1 or 2 representing read 1 or read 2
        ] as List<String>
    }

    @Override
    protected void _assertCustom() {
        assertUniqueFastq()
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

    public MetadataTable subsetBySample(String sampleName) {
        return (MetadataTable)subsetByColumn(INPUT_TABLE_SAMPLE_NAME, sampleName);
    }

    public MetadataTable subsetByLibrary(String library) {
        return (MetadataTable)subsetByColumn(INPUT_TABLE_LIBRARY, library);
    }

    public List<String> listSampleNames() {
        return listColumn(INPUT_TABLE_SAMPLE_NAME).unique()
    }

    public List<String> listRunIDs() {
        return listColumn(INPUT_TABLE_RUN_ID).unique()
    }

    public List<String> listLibraries() {
        return listColumn(INPUT_TABLE_LIBRARY).unique()
    }

}
