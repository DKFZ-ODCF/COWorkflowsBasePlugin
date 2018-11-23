package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.COFileStageSettings
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.RoddyTestSpec
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
class WorkflowUsingMergedBamsSpec extends RoddyTestSpec {

    static final boolean CONTROL = false
    static final boolean NOCONTROL = true
    static final boolean SINGLETUMOR = false
    static final boolean MULTITUMOR = true

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

    void "check initial files with valid entries for control/tumor workflows without exceptions"(ExecutionContext context, BasicBamFile[] initialFiles) {
        when:
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, initialFiles)
        context.errors.each { println(it) }

        then:
        result == true

        where:
        context                          | initialFiles
        getContext(CONTROL, SINGLETUMOR) | getControlBam(context) + getTumorBam(context)        // Control + Tumor
        getContext(CONTROL, MULTITUMOR)  | getControlBam(context) + getTumorBamArray(context)   // Control + Multitumor
    }

    void "check initial files with valid entries for no-control workflows without exceptions"(ExecutionContext context, BasicBamFile[] initialFiles) {
        when:
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, initialFiles)
        context.errors.each { println(it) }

        then:
        result == true

        where:
        context                            | initialFiles
        getContext(NOCONTROL, SINGLETUMOR) | getTumorBam(context)                 // No control + Tumor
        getContext(NOCONTROL, MULTITUMOR)  | getTumorBamArray(context)            // No control + Multitumor
    }

    void "check initial files with invalid entries for control/tumor workflows without exceptions"(ExecutionContext context, BasicBamFile[] initialFiles, int errorCount) {
        when:
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, initialFiles)
        context.errors.each { println(it) }

        then:
        result == false
        context.getErrors().size() == errorCount

        where:
        context                          | initialFiles              | errorCount
        getContext(CONTROL, SINGLETUMOR) | null                      | 1
        getContext(CONTROL, SINGLETUMOR) | new BasicBamFile[0]       | 1
        getContext(CONTROL, SINGLETUMOR) | getControlBam(context)    | 1
        getContext(CONTROL, SINGLETUMOR) | getTumorBam(context)      | 1
        getContext(CONTROL, SINGLETUMOR) | getTumorBamArray(context) | 1
    }

    void "check initial files with invalid entries for no-control workflows without exceptions"(ExecutionContext context, BasicBamFile[] initialFiles, int errorCount) {
        when:
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, initialFiles)
        context.errors.each { println(it) }

        then:
        result == false
        context.getErrors().size() == errorCount

        where:
        context                            | initialFiles                                       | errorCount
        getContext(NOCONTROL, SINGLETUMOR) | getControlBam(context)                             | 1
        getContext(NOCONTROL, SINGLETUMOR) | getControlBam(context) + getTumorBam(context)      | 1
        getContext(NOCONTROL, SINGLETUMOR) | getControlBam(context) + getTumorBamArray(context) | 1
    }

    void "check initial files with invalid entries for control/tumor workflows with exceptions"(ExecutionContext context, BasicBamFile[] initialFiles, Class<Throwable> expectedException) {
        when:
        createMockupWorkflow(context).checkInitialFiles(context, initialFiles)

        then:
        thrown(expectedException)

        where:
        context                            | initialFiles                              | expectedException
        getContext(CONTROL, SINGLETUMOR)   | getEmptyBam()                             | RuntimeException
        getContext(CONTROL, SINGLETUMOR)   | getEmptyBam() + getTumorBam(context)      | RuntimeException
        getContext(CONTROL, SINGLETUMOR)   | getEmptyBam() + getTumorBamArray(context) | RuntimeException
        getContext(NOCONTROL, SINGLETUMOR) | getEmptyBam()                             | RuntimeException
        getContext(NOCONTROL, SINGLETUMOR) | getControlBam(context) + getEmptyBam()    | RuntimeException
    }
}