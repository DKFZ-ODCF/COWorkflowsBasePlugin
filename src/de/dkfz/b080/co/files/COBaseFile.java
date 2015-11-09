package de.dkfz.b080.co.files;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.jobs.JobResult;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileGroup;
import de.dkfz.roddy.knowledge.files.FileStageSettings;

import java.io.File;
import java.util.List;

/**
 */
public abstract class COBaseFile<COFileStageSettings> extends BaseFile {
    public COBaseFile(FileGroup parentFileGroup, FileStageSettings settings, JobResult jobResult) {
        super(parentFileGroup, settings, jobResult);
    }

    public COBaseFile(FileGroup parentFileGroup, FileStageSettings settings) {
        super(parentFileGroup, settings);
    }

    public COBaseFile(BaseFile parentFile, FileStageSettings fileStage, JobResult jobResult) {
        super(parentFile, fileStage, jobResult);
    }

    public COBaseFile(BaseFile parentFile) {
        super(parentFile);
    }

    public COBaseFile(BaseFile parentFile, FileStageSettings settings) {
        super(parentFile, settings);
    }

    public COBaseFile(File path, ExecutionContext executionContext, JobResult jobResult, List<BaseFile> parentFiles, FileStageSettings settings) {
        super(path, executionContext, jobResult, parentFiles, settings);
    }
}
