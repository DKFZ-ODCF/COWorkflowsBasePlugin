/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.common

import de.dkfz.b080.co.files.BasicBamFile
import de.dkfz.b080.co.files.COFileStageSettings
import de.dkfz.b080.co.files.Sample
import de.dkfz.roddy.RoddyTestSpec
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic

/**
 * Created by heinold on 10.07.16.
 */
class WorkflowUsingMergedBamsSpec extends RoddyTestSpec {

    static final boolean CONTROL = false
    static final boolean NOCONTROL = true
    static final boolean SINGLETUMOR = false
    static final boolean MULTITUMOR = true

    static List<Boolean> NULL = null
    static List<Boolean> EMPTY_BAM = []
    static List<Boolean> CONTROL_BAM = [true]
    static List<Boolean> TUMOR_BAM = [false]
    static List<Boolean> TUMOR_BAM_ARRAY = [false, false, false]

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

    @CompileStatic
    private ExecutionContext getContext(boolean noControl, boolean multiTumor) {
        ExecutionContext context = contextResource.createSimpleContext(WorkflowUsingMergedBamsSpec.class)

        if (noControl) context.getConfigurationValues().add(new ConfigurationValue(WorkflowUsingMergedBams.IS_NO_CONTROL_WORKFLOW, "true"))
        if (multiTumor) context.getConfigurationValues().add(new ConfigurationValue(WorkflowUsingMergedBams.WORKFLOW_SUPPORTS_MULTI_TUMOR_SAMPLES, "true"))
        context
    }

    @CompileStatic
    private BasicBamFile getControlBam(ExecutionContext context) {
        BaseFile.constructSourceFile(BasicBamFile, new File(context.getOutputDirectory(), "control.bam"), context, new COFileStageSettings(new Sample(context, "control"), context.dataSet)) as BasicBamFile
    }

    @CompileStatic
    private BasicBamFile getTumorBam(ExecutionContext context, String id = "") {
        BaseFile.constructSourceFile(BasicBamFile, new File(context.getOutputDirectory(), "tumor${id}.bam"), context, new COFileStageSettings(new Sample(context, "tumor"), context.dataSet)) as BasicBamFile
    }

    @CompileStatic
    private BasicBamFile[] bamArrayFromBooleanList(ExecutionContext context, List files) {
        if (!files) return null
        boolean multiTumor = files.count { it == false } > 1
        int tumorIndex = 1
        List<BasicBamFile> resultFiles = []
        for (int i = 0; i < files.size(); i++) {
            if (files[i]) {
                resultFiles << getControlBam(context)
            } else {
                if (multiTumor)
                    resultFiles << getTumorBam(context, String.format("_%02d", tumorIndex++))
                else
                    resultFiles << getTumorBam(context)
            }
        }
        return resultFiles.flatten() as BasicBamFile[]
    }

    void "check initial files with valid entries for control/tumor workflows without exceptions"(boolean controlFlag, boolean tumorFlag, List<Boolean> initialFiles) {
        when:
        ExecutionContext context = getContext(controlFlag, tumorFlag)
        def _initialFiles = bamArrayFromBooleanList(context, initialFiles)
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, _initialFiles)
        context.errors.each { println(it) }

        then:
        result == true

        where:
        controlFlag | tumorFlag   | initialFiles
        CONTROL     | SINGLETUMOR | CONTROL_BAM + TUMOR_BAM        // Control + Tumor
        CONTROL     | MULTITUMOR  | CONTROL_BAM + TUMOR_BAM_ARRAY   // Control + Multitumor
    }

    void "check initial files with valid entries for no-control workflows without exceptions"(boolean controlFlag, boolean tumorFlag, List<Boolean> initialFiles) {
        when:
        ExecutionContext context = getContext(controlFlag, tumorFlag)
        def _initialFiles = bamArrayFromBooleanList(context, initialFiles)
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, _initialFiles)
        context.errors.each { println(it) }

        then:
        result == true

        where:
        controlFlag | tumorFlag   | initialFiles
        NOCONTROL   | SINGLETUMOR | TUMOR_BAM                 // No control + Tumor
        NOCONTROL   | MULTITUMOR  | TUMOR_BAM_ARRAY            // No control + Multitumor
    }

    void "check initial files with invalid entries for control/tumor workflows without exceptions"(boolean controlFlag, boolean tumorFlag, List<Boolean> initialFiles, int errorCount) {
        when:
        ExecutionContext context = getContext(controlFlag, tumorFlag)
        def _initialFiles = bamArrayFromBooleanList(context, initialFiles)
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, _initialFiles)
        context.errors.each { println(it) }

        then:
        result == false
        context.getErrors().size() == errorCount

        where:
        controlFlag | tumorFlag   | initialFiles    | errorCount
        CONTROL     | SINGLETUMOR | NULL            | 1
        CONTROL     | SINGLETUMOR | EMPTY_BAM       | 1
        CONTROL     | SINGLETUMOR | CONTROL_BAM     | 1
        CONTROL     | SINGLETUMOR | TUMOR_BAM       | 1
        CONTROL     | SINGLETUMOR | TUMOR_BAM_ARRAY | 1
    }

    void "check initial files with invalid entries for no-control workflows without exceptions"(boolean controlFlag, boolean tumorFlag, List<Boolean> initialFiles, int errorCount) {
        when:
        ExecutionContext context = getContext(controlFlag, tumorFlag)
        def _initialFiles = bamArrayFromBooleanList(context, initialFiles)
        boolean result = createMockupWorkflow(context).checkInitialFiles(context, _initialFiles)
        context.errors.each { println(it) }

        then:
        result == false
        context.getErrors().size() == errorCount

        where:
        controlFlag | tumorFlag   | initialFiles                  | errorCount
        NOCONTROL   | SINGLETUMOR | CONTROL_BAM                   | 1
        NOCONTROL   | SINGLETUMOR | CONTROL_BAM + TUMOR_BAM       | 1
        NOCONTROL   | SINGLETUMOR | CONTROL_BAM + TUMOR_BAM_ARRAY | 1
    }

    void "check initial files with invalid entries for control/tumor workflows with exceptions"(boolean controlFlag, boolean tumorFlag, List<Boolean> initialFiles, Class<Throwable> expectedException) {
        when:
        ExecutionContext context = getContext(controlFlag, tumorFlag)
        def _initialFiles = bamArrayFromBooleanList(context, initialFiles)
        createMockupWorkflow(context).checkInitialFiles(controlFlag, tumorFlag, _initialFiles)

        then:
        thrown(expectedException)

        where:
        controlFlag | tumorFlag   | initialFiles                | expectedException
        CONTROL     | SINGLETUMOR | EMPTY_BAM                   | RuntimeException
        CONTROL     | SINGLETUMOR | EMPTY_BAM + TUMOR_BAM       | RuntimeException
        CONTROL     | SINGLETUMOR | EMPTY_BAM + TUMOR_BAM_ARRAY | RuntimeException
        NOCONTROL   | SINGLETUMOR | EMPTY_BAM                   | RuntimeException
        NOCONTROL   | SINGLETUMOR | CONTROL_BAM + EMPTY_BAM     | RuntimeException
    }

}