package de.dkfz.b080.co.files;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.jobs.JobDependencyID;
import de.dkfz.roddy.execution.jobs.JobResult;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileGroup;
import de.dkfz.roddy.knowledge.files.FileStageSettings;

import java.io.File;
import java.util.List;

/**
 * Created by heinold on 14.01.16.
 */
public class BasicBamFile extends COBaseFile {

    public BasicBamFile(FileGroup parentFileGroup, FileStageSettings settings, JobResult jobResult) {
        super(parentFileGroup, settings, jobResult);
    }

    public BasicBamFile(FileGroup parentFileGroup, FileStageSettings settings) {
        super(parentFileGroup, settings);
    }

    public BasicBamFile(BaseFile parentFile, FileStageSettings fileStage, JobResult jobResult) {
        super(parentFile, fileStage, jobResult);
    }

    public BasicBamFile(BaseFile parentFile) {
        super(parentFile);
    }

    public BasicBamFile(BaseFile parentFile, FileStageSettings settings) {
        super(parentFile, settings);
    }

    public BasicBamFile(File path, ExecutionContext executionContext, JobResult jobResult, List parentFiles, FileStageSettings settings) {
        super(path, executionContext, jobResult, parentFiles, settings);
    }
}
