/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
 */
package de.dkfz.b080.co.files;

import de.dkfz.b080.co.common.BasicCOProjectsRuntimeService;
import de.dkfz.b080.co.common.COConfig;
import de.dkfz.b080.co.common.COConstants;
import de.dkfz.roddy.core.Analysis;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextError;
import de.dkfz.roddy.core.Project;
import de.dkfz.roddy.tools.LoggerWrapper;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * A sample is either control or tumor. It has a name and a path and belongs to a project.
 *
 * @author michael
 */
public class Sample implements Comparable<Sample>, Serializable {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(Sample.class.getName());

    /**
     * A list of libraries which might be filled (or not)
     */
    private List<String> libraries;

    public enum SampleType {
        CONTROL,
        TUMOR,
        UNKNOWN
    }

    private transient final Project project;

    private transient final Analysis analysis;

    private final String name;

    private final File path;

    private transient final ExecutionContext executionContext;

    private final SampleType sampleType;

    public Sample(ExecutionContext context, String name) {
        this(context, name, null);
    }

    public Sample(ExecutionContext context, File path) {
        this(context, path.getName(), path);
    }

    public Sample(ExecutionContext context, String name, File path) {
        this.name = name;
        this.path = path;
        this.executionContext = context;
        this.analysis = context.getAnalysis();
        this.project = context.getProject();
        COConfig cfg = new COConfig(context);

        SampleType tempSampleType = determineSampleType(context, name);
        if (tempSampleType == SampleType.UNKNOWN)
            logger.always("Sample type is not known for name '" + name + "'. I know " +
                    "'" + String.join("', '", cfg.getPossibleControlSampleNamePrefixes()) + "' (control) and " +
                    "'" + String.join("', '", cfg.getPossibleTumorSampleNamePrefixes()) + "' (tumor).");

        sampleType = tempSampleType;
    }

    @Override
    public int compareTo(Sample o) {
        int compareSampleTypeResult = sampleType.name().compareTo(o.sampleType.name());
        if (compareSampleTypeResult != 0) return compareSampleTypeResult;
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sample sample = (Sample) o;
        return name.equals(sample.name) &&
                sampleType == sample.sampleType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sampleType);
    }

    public static SampleType determineSampleType(ExecutionContext context, String sampleName) {
        COConfig cfg = new COConfig(context);
        SampleType tempSampleType = SampleType.UNKNOWN;
        if (isInSampleList(cfg, cfg.getPossibleControlSampleNamePrefixes(), sampleName)) {
            if (isInSampleList(cfg, cfg.getPossibleTumorSampleNamePrefixes(), sampleName)) {
                context.addError(ExecutionContextError.EXECUTION_SETUP_INVALID.expand(
                        "Sample name '" + sampleName + "' matches both values of " +
                                COConstants.CVALUE_POSSIBLE_TUMOR_SAMPLE_NAME_PREFIXES + " and " +
                                COConstants.CVALUE_POSSIBLE_CONTROL_SAMPLE_NAME_PREFIXES));
                tempSampleType = SampleType.UNKNOWN;
            } else {
                tempSampleType = SampleType.CONTROL;
            }
        } else if (isInSampleList(cfg, cfg.getPossibleTumorSampleNamePrefixes(), sampleName)) {
            tempSampleType = SampleType.TUMOR;
        }
        return tempSampleType;
    }

    private static boolean isInSampleList(COConfig cfg, List<String> possibleControlSampleNamePrefixes, String sampleName) {
        for (String s : possibleControlSampleNamePrefixes) {
            if (cfg.getSearchMergedBamWithSeparator()) {
                if (sampleName.equals(s))
                    return true;
            } else if (sampleName.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public SampleType getSampleType() {
        return sampleType;
    }

    public File getPath() {
        return path;
    }

    public Project getProject() {
        return executionContext.getProject();
    }

    public SampleType getType() {
        return sampleType;
    }

    public List<String> getLibraries() {
        if (libraries == null) {
            libraries = ((BasicCOProjectsRuntimeService) analysis.getRuntimeService()).getMetadataAccessor().getLibraries(executionContext, this);
        }
        return libraries;
    }

    @Override
    public String toString() {
        return "Sample{" + "name=" + name + ", path=" + path + ", executionContext=" + executionContext + '}';
    }

}
