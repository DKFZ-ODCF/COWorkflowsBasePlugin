package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ContextResource
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertFalse

/**
 * Created by heinold on 10.07.16.
 */
@CompileStatic
class WorkflowUsingMergedBamsTest extends ContextResource {

    @Rule
    private TemporaryFolder temporaryFolder = new TemporaryFolder()

    private WorkflowUsingMergedBams createMockupWorkflow() {
        return new WorkflowUsingMergedBams() {

            @Override
            protected boolean execute(ExecutionContext context, BasicBamFile bamControlMerged, BasicBamFile bamTumorMerged) {
                return true
            }
        }
    }

    private ExecutionContext getContext() {
        return createSimpleContext(WorkflowUsingMergedBamsTest.class)
    }

    private setContextForNoControl(ExecutionContext context) {
        context.getConfigurationValues().add(new ConfigurationValue(WorkflowUsingMergedBams.IS_NO_CONTROL_WORKFLOW, "true"))
    }

    private setContextForMT(ExecutionContext context) {
        context.getConfigurationValues().add(new ConfigurationValue(WorkflowUsingMergedBams.WORKFLOW_SUPPORTS_MULTI_TUMOR_SAMPLES, "true"))
    }

    private BasicBamFile[] getControlBam(ExecutionContext context) {
        [BaseFile.constructSourceFile(BasicBamFile, new File(context.getOutputDirectory(), "control.bam"), context) as BasicBamFile] as BasicBamFile[]
    }

    private BasicBamFile[] getTumorBam(ExecutionContext context) {
        [BaseFile.constructSourceFile(BasicBamFile, new File(context.getOutputDirectory(), "tumor.bam"), context) as BasicBamFile] as BasicBamFile[]
    }

    private BasicBamFile[] getTumorBamArray(ExecutionContext context) {
        [
                new BasicBamFile(BaseFile.constructSourceFile(BasicBamFile, new File(context.getOutputDirectory(), "tumor_01.bam"), context)),
                new BasicBamFile(BaseFile.constructSourceFile(BasicBamFile, new File(context.getOutputDirectory(), "tumor_02.bam"), context)),
                new BasicBamFile(BaseFile.constructSourceFile(BasicBamFile, new File(context.getOutputDirectory(), "tumor_03.bam"), context))
        ] as BasicBamFile[]
    }

    private BasicBamFile[] getEmptyBam() {
        [null] as BasicBamFile[]
    }


    @Test
    void checkInitialFilesWithValidEntries() throws Exception {
        ExecutionContext context = getContext()

        // Control + Tumor
        assert createMockupWorkflow().checkInitialFiles(context, getControlBam(context) + getTumorBam(context))

        // Control + Multitumor
        setContextForMT(context)
        assert createMockupWorkflow().checkInitialFiles(context, getControlBam(context) + getTumorBamArray(context))

        // No control + Tumor
        context = getContext()
        setContextForNoControl(context)
        assert createMockupWorkflow().checkInitialFiles(context, getEmptyBam() + getTumorBam(context))

        // No control + Multitumor
        setContextForMT(context)
        assert createMockupWorkflow().checkInitialFiles(context, getEmptyBam() + getTumorBamArray(context))
    }

    @Test
    void checkInitialFilesWithInvalidEntries() throws Exception {
        ExecutionContext context = getContext()

        // Without tumor bam
        assertFalse createMockupWorkflow().checkInitialFiles(context, getControlBam(context) + getEmptyBam())
        assert context.getErrors().size() == 1

        // Without tumor bam
        context = getContext()
        assertFalse createMockupWorkflow().checkInitialFiles(context, getEmptyBam())
        assert context.getErrors().size() == 2

        // Without control and runflag
        context = getContext()
        assertFalse createMockupWorkflow().checkInitialFiles(context, getEmptyBam() + getTumorBam(context))
        assert context.getErrors().size() == 1

        context = getContext()
        assertFalse createMockupWorkflow().checkInitialFiles(context, getEmptyBam() + getTumorBamArray(context))
        assert context.getErrors().size() == 1
    }

}