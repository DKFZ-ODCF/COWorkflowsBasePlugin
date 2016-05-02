package de.dkfz.b080.co.common

import de.dkfz.roddy.execution.io.BaseMetadataTable
import de.dkfz.roddy.execution.io.MetadataTableFactory
import de.dkfz.roddy.plugins.LibrariesFactory
import groovy.transform.CompileStatic
import org.junit.Test

@CompileStatic
public class MetadataTableTest {

    public File resourceDir = new File("test/resources");
    public String correctTable = "InputTableTest_CorrectTable1.tsv"
    public String damagedTable = "InputTableTest_DamagedInputTable.tsv"

    public static final Map<String, String> columnIDMap = [
            "mergeCol"       : "Sample",
            "markCol"        : "Library",
            "datasetCol"     : "PID",
            "readLayoutCol"  : "ReadLayout",
            "runCol"         : "Run",
            "mateCol"        : "Mate",
            "sequenceFileCol": "SequenceFile"
    ];
    public static final List<String> mandatoryColumns = [
            "datasetCol", "sequenceFileCol", "mergeCol", "markCol", "readLayoutCol", "runCol"
    ];

    private BaseMetadataTable readTable(String table) {
        String testFileName = LibrariesFactory.groovyClassLoader.getResource(table).file

        BaseMetadataTable baseMetadataTable = MetadataTableFactory.readTable(new File(testFileName), "tsv", columnIDMap, mandatoryColumns)
        BaseMetadataTable inputTable = new BaseMetadataTable(baseMetadataTable)
        return inputTable;
    }

    @Test
    public void testGetHeader() throws Exception {
        BaseMetadataTable table = readTable(correctTable)
        def keys = table.getHeaderMap().keySet()
        assert keys.size() == 6
        assert keys.containsAll(["PID", "Sample", "Library", "Run", "Mate", "SequenceFile"])
    }

/*
    @Test
    public void testListSampleNames() throws Exception {
        BaseMetadataTable table = readTable(correctTable)
        assert table.listSampleNames().containsAll(["tumor", "control"])
    }
*/
}