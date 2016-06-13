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

    @Test void testMatchPathElement() throws Exception {
        assert BasicCOProjectsRuntimeService.matchPathElement("/a/b/\${sample}/d", '${sample}') == 3
    }

    @Test void testMatchPathElementInFiles () throws Exception {
        assert BasicCOProjectsRuntimeService.matchPathElementInFiles("/a/b/c/\${sample}",
                '${sample}',
                [new File("/a/b/c/sampleName1/bla/bla/bla"),
                 new File("/a/b/c/sampleName2/bla/blub/")] as List<String>) == ["sampleName1", "sampleName2"] as List<String>
    }

    @Test
    public void testMatchPathElementInFiles_tooShortPath() throws Exception {
        try {
            BasicCOProjectsRuntimeService.matchPathElementInFiles("/a/b/c/\${sample}", '${sample}', [new File("/a/b/c/")] as List<String>)
        } catch (RuntimeException e) {
            assert e.message.startsWith("Path to file")
        }
    }
}