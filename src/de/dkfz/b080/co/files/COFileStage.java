/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
 */
package de.dkfz.b080.co.files;

import de.dkfz.roddy.knowledge.files.FileStage;

/**
 */
public class COFileStage extends FileStage {

    public static final COFileStage PID = new COFileStage(0);
    public static final COFileStage SAMPLE = new COFileStage(PID, 2);
    public static final COFileStage LIBRARY = new COFileStage(SAMPLE, 4);
    public static final COFileStage RUN = new COFileStage(LIBRARY, 8);
    public static final COFileStage LANE = new COFileStage(RUN, 16);
    public static final COFileStage INDEXEDLANE = new COFileStage(LANE, 32);

    static {}

    COFileStage(FileStage successor, int value) {
        super(successor, value);
    }

    COFileStage(int value) {
        super(value);
    }
}
