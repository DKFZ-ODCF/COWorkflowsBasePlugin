package de.dkfz.b080.co.common

import org.junit.Test

/**
 * Created by kensche on 10.06.16.
 */
public class BasicCOProjectsRuntimeServiceTest {

    @Test void testIndexOfPathElement() throws Exception {
        assert BasicCOProjectsRuntimeService.indexOfPathElement("/a/b/\${sample}/d", '${sample}') == 3
    }

    @Test void testgrepPathElementFromFilenames () throws Exception {
        assert BasicCOProjectsRuntimeService.grepPathElementFromFilenames("/a/b/c/\${sample}",
                '${sample}',
                [new File("/a/b/c/sampleName1/bla/bla/bla"),
                 new File("/a/b/c/sampleName2/bla/blub/")] as List<String>) == ["sampleName1", "sampleName2"] as List<String>
    }

    @Test
    public void testgrepPathElementFromFilenames_tooShortPath() throws Exception {
        try {
            BasicCOProjectsRuntimeService.grepPathElementFromFilenames("/a/b/c/\${sample}", '${sample}', [new File("/a/b/c/")] as List<String>)
        } catch (RuntimeException e) {
            assert e.message.startsWith("Path to file")
        }
    }
}