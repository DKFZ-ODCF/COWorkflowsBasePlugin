package de.dkfz.b080.co.common

import static COConstants.*
import de.dkfz.roddy.execution.io.MetadataTableFactory
import de.dkfz.roddy.plugins.LibrariesFactory
import groovy.transform.CompileStatic
import org.junit.Test

@CompileStatic
class MetadataTableTest {

    public File resourceDir = new File("test/resources")
    public String correctTable = "InputTableTest_CorrectTable1.tsv"
    public String damagedTable = "InputTableTest_DamagedInputTable.tsv"

    public static final Map<String, String> columnIDMap = [
            (INPUT_TABLE_SAMPLECOL_NAME)    : "Sample",
            (INPUT_TABLE_MARKCOL_NAME)      : "Library",
            (INPUT_TABLE_DATASETCOL_NAME)   : "PID",
            (INPUT_TABLE_READLAYOUTCOL_NAME): "ReadLayout",
            (INPUT_TABLE_RUNCOL_NAME)       : "Run",
            (INPUT_TABLE_MATECOL_NAME)      : "Mate",
            (INPUT_TABLE_FASTQCOL_NAME)     : "SequenceFile"
    ]
    public static final List<String> mandatoryColumns = [
            INPUT_TABLE_DATASETCOL_NAME,
            INPUT_TABLE_FASTQCOL_NAME,
            INPUT_TABLE_SAMPLECOL_NAME,
            INPUT_TABLE_MARKCOL_NAME,
            INPUT_TABLE_READLAYOUTCOL_NAME,
            INPUT_TABLE_RUNCOL_NAME,
            INPUT_TABLE_MATECOL_NAME
    ]

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