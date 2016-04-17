package de.dkfz.b080.co.common

import de.dkfz.roddy.plugins.LibrariesFactory
import groovy.transform.CompileStatic
import org.junit.Test

@CompileStatic
public class MetadataTableTest {

    public File resourceDir = new File("test/resources");
    public String correctTable = "InputTableTest_CorrectTable1.tsv"
    public String damagedTable = "InputTableTest_DamagedInputTable.tsv"

    private MetadataTable readTable(String table) {
        String testFileName = LibrariesFactory.groovyClassLoader.getResource(table).file
        FileReader testFile = new FileReader(testFileName)
        MetadataTable inputTable = new MetadataTable(MetadataTable.readTable(testFile, "tsv"))
        return inputTable;
    }

    @Test
    public void testGetHeader() throws Exception {
        MetadataTable table = readTable(correctTable)
        def keys = table.getHeaderMap().keySet()
        assert keys.size() == 6
        assert keys.containsAll(["PID", "SampleName", "Library", "RunId", "ReadNumber", "File"])
    }

    @Test
    public void testListSampleNames() throws Exception {
        MetadataTable table = readTable(correctTable)
        assert table.listSampleNames().containsAll(["tumor", "control"])
    }
}