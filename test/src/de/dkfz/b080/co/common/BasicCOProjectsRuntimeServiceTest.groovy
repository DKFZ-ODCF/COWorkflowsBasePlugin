package de.dkfz.b080.co.common;

import de.dkfz.roddy.config.AnalysisConfiguration;
import de.dkfz.roddy.core.*
import org.junit.Rule;
import org.junit.Test
import org.junit.rules.ExpectedException;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by kensche on 10.06.16.
 */
public class BasicCOProjectsRuntimeServiceTest {

    @Test
    public void testExtractSampleNamesFromFastqList() throws Exception {
        assert BasicCOProjectsRuntimeService.extractSampleNamesFromFastqList([new File("/a/b/c/sampleName/bla/bla/bla")] as List<String>, "/a/b/c/\${sample}") == ["sampleName"] as List<String>
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testExtractSampleNamesFromFastqList_tooLongFastqPath() throws Exception {
        thrown.expect(RuntimeException.class)
        thrown.expectMessage(startsWith("Path to fastq_list file"))
        BasicCOProjectsRuntimeService.extractSampleNamesFromFastqList([new File("/a/b/c/")] as List<String>, "/a/b/c/\${sample}")
    }
}