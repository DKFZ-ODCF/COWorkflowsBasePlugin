package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.COFileStageSettings
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ContextResource
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic
import org.junit.ClassRule
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by heinold on 10.07.16.
 */
class WorkflowUsingMergedBamsSpec extends Specification {

    @ClassRule
    static ContextResource contextResource = new ContextResource() {
        {
            before()
        }
    }

    @Ignore
    @CompileStatic
    private WorkflowUsingMergedBams createMockupWorkflow(ExecutionContext _context) {
        WorkflowUsingMergedBams w = new WorkflowUsingMergedBams() {

            @Override
            protected boolean execute(ExecutionContext context, BasicBamFile bamControlMerged, BasicBamFile bamTumorMerged) {
                return true
            }
        }
        w.setContext(_context)
        return w
    }

    @Ignore
    @CompileStatic
    private ExecutionContext getContext(boolean noControl, boolean multiTumor) {
        ExecutionContext context = contextResource.createSimpleContext(WorkflowUsingMergedBamsSpec.class)
        if (noControl) context.getConfigurationValues().add(new ConfigurationValue(WorkflowUsingMergedBams.IS_NO_CONTROL_WORKFLOW, "true"))
        if (multiTumor) context.getConfigurationValues().add(new ConfigurationValue(WorkflowUsingMergedBams.WORKFLOW_SUPPORTS_MULTI_TUMOR_SAMPLES, "true"))
        context
    }

    @Ignore
    @CompileStatic
    private BasicBamFile[] getControlBam(ExecutionContext context) {
        [BaseFile.constructSourceFile(BasicBamFile, new File(context.getOutputDirectory(), "control.bam"), context, new COFileStageSettings(new Sample(context, "control"), context.dataSet)) as BasicBamFile] as BasicBamFile[]
    }

    @Ignore
    @CompileStatic
    private BasicBamFile[] getTumorBam(ExecutionContext context, String id = "") {
        [BaseFile.constructSourceFile(BasicBamFile, new File(context.getOutputDirectory(), "tumor${id}.bam"), context, new COFileStageSettings(new Sample(context, "tumor"), context.dataSet)) as BasicBamFile] as BasicBamFile[]
    }

    @Ignore
    @CompileStatic
    private BasicBamFile[] getTumorBamArray(ExecutionContext context) {
        ["_01", "_02", "_03"].collect { String id -> getTumorBam(context, id) }.flatten() as BasicBamFile[]
    }

    @Ignore
    @CompileStatic
    private BasicBamFile[] getEmptyBam() {
        [null] as BasicBamFile[]
    }


    void checkInitialFilesWithValidEntriesForControl(ExecutionContext context, BasicBamFile[] initialFiles) {
        when:
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, initialFiles)
        context.errors.each { println(it) }

        then:
        result == true

        where:
        context                  | initialFiles
        getContext(false, false) | getControlBam(context) + getTumorBam(context)        // Control + Tumor
        getContext(false, true)  | getControlBam(context) + getTumorBamArray(context)   // Control + Multitumor
    }

    void checkInitialFilesWithValidEntriesForNoControl(ExecutionContext context, BasicBamFile[] initialFiles) {
        when:
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, initialFiles)
        context.errors.each { println(it) }

        then:
        result == true

        where:
        context                 | initialFiles
        getContext(true, false) | getTumorBam(context)                 // No control + Tumor
        getContext(true, true)  | getTumorBamArray(context)            // No control + Multitumor
    }

    void checkInitialFilesWithInvalidEntriesControl(ExecutionContext context, BasicBamFile[] initialFiles, int errorCount) {
        when:
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, initialFiles)
        context.errors.each { println(it) }

        then:
        result == false
        context.getErrors().size() == errorCount

        where:
        context                  | initialFiles                              | errorCount
        getContext(false, false) | new BasicBamFile[0]                       | 1                // Without anything
        getContext(false, false) | getEmptyBam()                             | 1                // Without tumor bam
        getContext(false, false) | getEmptyBam() + getTumorBam(context)      | 1                // Without control and runflag
        getContext(false, false) | getEmptyBam() + getTumorBamArray(context) | 1                // Without control and runflags
    }

    void checkInitialFilesWithInvalidEntriesNoControl(ExecutionContext context, BasicBamFile[] initialFiles, int errorCount) {
        when:
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, initialFiles)
        context.errors.each { println(it) }

        then:
        result == false
        context.getErrors().size() == errorCount

        where:
        context                  | initialFiles                                       | errorCount

        getContext(true, false) | getControlBam(context)                             | 1                // Without tumor bam
        getContext(true, false) | getControlBam(context) + getEmptyBam()             | 1                // Without tumor bam
        getContext(true, false) | getControlBam(context) + getTumorBam(context)      | 1                // Without control and runflag
        getContext(true, false) | getControlBam(context) + getTumorBamArray(context) | 1                // Without control and runflags
    }

}