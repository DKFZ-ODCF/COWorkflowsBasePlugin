/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.COConstants
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
            "fileCol"        : "SequenceFile"
    ];
    public static final List<String> mandatoryColumns = [
            COConstants.INPUT_TABLE_DATASETCOL_NAME,
            COConstants.INPUT_TABLE_FILECOL_NAME,
            COConstants.INPUT_TABLE_MERGECOL_NAME,
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
    public void testGetHeader() throws Exception {
        MetadataTable table = readTable(correctTable)
        def keys = table.getHeaderMap().keySet()
        assert keys.size() == 6
        assert keys.containsAll(["PID", "Sample", "Library", "Run", "Mate", "SequenceFile"])
    }

    @Test
    public void testListSampleNames() throws Exception {
        MetadataTable table = readTable(correctTable)
        assert table.listSampleNames().containsAll(["tumor", "control"])
    }

}