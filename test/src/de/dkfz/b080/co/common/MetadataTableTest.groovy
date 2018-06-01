package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.COConstants
import de.dkfz.roddy.execution.io.MetadataTableFactory
import de.dkfz.roddy.plugins.LibrariesFactory
import groovy.transform.CompileStatic
import org.junit.Test

@CompileStatic
class MetadataTableTest {

    public File resourceDir = new File("test/resources");
    public String correctTable = "InputTableTest_CorrectTable1.tsv"
    public String damagedTable = "InputTableTest_DamagedInputTable.tsv"

    public static final Map<String, String> columnIDMap = [
            (COConstants.INPUT_TABLE_SAMPLECOL_NAME)       : "Sample",
            (COConstants.INPUT_TABLE_MARKCOL_NAME)         : "Library",
            (COConstants.INPUT_TABLE_DATASETCOL_NAME)      : "PID",
            (COConstants.INPUT_TABLE_READLAYOUTCOL_NAME)   : "ReadLayout",
            (COConstants.INPUT_TABLE_RUNCOL_NAME)          : "Run",
            (COConstants.INPUT_TABLE_MATECOL_NAME)         : "Mate",
            (COConstants.INPUT_TABLE_FILECOL_NAME)         : "SequenceFile"
    ];
    public static final List<String> mandatoryColumns = [
            COConstants.INPUT_TABLE_DATASETCOL_NAME,
            COConstants.INPUT_TABLE_FILECOL_NAME,
            COConstants.INPUT_TABLE_SAMPLECOL_NAME,
            COConstants.INPUT_TABLE_MARKCOL_NAME,
            COConstants.INPUT_TABLE_READLAYOUTCOL_NAME,
            COConstants.INPUT_TABLE_RUNCOL_NAME,
            COConstants.INPUT_TABLE_MATECOL_NAME
    ];

    private MetadataTable readTable(String table) {
        String testFileName = LibrariesFactory.getGroovyClassLoader().getResource(table).file
        return new MetadataTable(MetadataTableFactory.readTable(new File(testFileName), "tsv", columnIDMap, mandatoryColumns))
    }

    @Test
    void testGetHeader() throws Exception {
        MetadataTable table = readTable(correctTable)
        def keys = table.getHeaderMap().keySet()
        assert keys.size() == 6
        assert keys.containsAll(["PID", "Sample", "Library", "Run", "Mate", "SequenceFile"])
    }


    @Test
    void testListSampleNames() throws Exception {
        MetadataTable table = readTable(correctTable)
        assert table.listSampleNames().containsAll(["tumor", "control"])
    }

}