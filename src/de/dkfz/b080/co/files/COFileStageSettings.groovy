/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
 */
package de.dkfz.b080.co.files;

import de.dkfz.b080.co.common.IndexID;
import de.dkfz.b080.co.common.LaneID;
import de.dkfz.b080.co.common.LibraryID;
import de.dkfz.b080.co.common.RunID;
import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.knowledge.files.FileStage
import de.dkfz.roddy.knowledge.files.FileStageSettings
import groovy.transform.CompileStatic;

@CompileStatic
class COFileStageSettings extends FileStageSettings<COFileStageSettings> {

    final LaneID laneID;
    final IndexID index;
    final int numericIndex;
    final RunID runID;
    final Sample sample;
    final LibraryID libraryID;

    // Convenience constructor
    COFileStageSettings(String laneID, String index, int numericIndex, String runID, String libraryID, Sample sample, DataSet dataSet, FileStage stage) {
        this(laneID == null ? (LaneID) null : new LaneID(laneID),
                index == null ? (IndexID) null : new IndexID(index),
                numericIndex,
                runID == null ? (RunID) null : new RunID(runID),
                libraryID == null ? (LibraryID) null : new LibraryID(libraryID),
                sample,
                dataSet,
                stage)
    }

    COFileStageSettings(LaneID laneID, IndexID index, int numericIndex, RunID runID, LibraryID libraryID, Sample sample, DataSet dataSet, FileStage stage) {
        super(dataSet, stage)
        this.laneID = laneID
        this.index = index
        this.numericIndex = numericIndex
        this.runID = runID
        this.libraryID = libraryID
        this.sample = sample
    }

    COFileStageSettings(LaneID laneID, RunID runID, LibraryID libraryID, Sample sample, DataSet dataSet) {
        this(laneID, null, -1, runID, libraryID, sample, dataSet, COFileStage.LANE);
    }

    COFileStageSettings(RunID runID, LibraryID libraryID, Sample sample, DataSet dataSet) {
        this(null, null, -1, runID, libraryID, sample, dataSet, COFileStage.LANE);
    }

    COFileStageSettings(LibraryID libraryID, Sample sample, DataSet dataSet) {
        this(null, null, -1, null, libraryID, sample, dataSet, COFileStage.LIBRARY);
    }

    COFileStageSettings(Sample sample, DataSet dataSet) {
        this(null, null, -1, null, (LibraryID) null, sample, dataSet, COFileStage.SAMPLE);
    }

    COFileStageSettings(DataSet dataSet) {
        this(null, null, -1, null, (LibraryID) null, null, dataSet, COFileStage.PID);
    }

    @Override
    COFileStageSettings copy() {
        return new COFileStageSettings(laneID, index, numericIndex, runID, libraryID, sample, dataSet, stage);
    }

    @Override
    COFileStageSettings decreaseLevel() {
        if (stage == COFileStage.INDEXEDLANE)
            return new COFileStageSettings(laneID, runID, libraryID, sample, dataSet);
        if (stage == COFileStage.LANE)
            return new COFileStageSettings(runID, libraryID, sample, dataSet);
        if (stage == COFileStage.RUN)
            return new COFileStageSettings(libraryID, sample, dataSet);
        if (stage == COFileStage.LIBRARY)
            return new COFileStageSettings(sample, dataSet);
        if (stage == COFileStage.SAMPLE)
            return new COFileStageSettings(dataSet);

        return copy();

    }

    @Override
    String getIDString() {
        if (stage == COFileStage.INDEXEDLANE)
            return String.format("%s_%s_%s_%s_%s_%s",
                    dataSet.id, sample.name, libraryID.toString(), runID.toString(), laneID.toString(), index);
        if (stage == COFileStage.LANE)
            return String.format("%s_%s_%s_%s_%s",
                    dataSet.id, sample.name, libraryID.toString(), runID.toString(), laneID.toString());
        if (stage == COFileStage.RUN)
            return String.format("%s_%s_%s_%s",
                    dataSet.id, sample.name, libraryID.toString(), runID.toString());
        if (stage == COFileStage.LIBRARY)
            return String.format("%s_%s_%s",
                    dataSet.id, sample.name, libraryID.toString());
        if (stage == COFileStage.SAMPLE)
            return String.format("%s_%s",
                    dataSet.id, sample.name);
        return String.format("%s",
                dataSet.id);
    }

    @Override
    String fillStringContent(String temp) {
        if (sample != null) temp = temp.replace("\${sample}", sample.name.toString());
        if (libraryID != null) temp = temp.replace("\${library}", libraryID.toString());
        if (runID != null) temp = temp.replace("\${run}", runID.toString());
        if (laneID != null) temp = temp.replace("\${lane}", laneID.toString());
        if (index != null) temp = temp.replace("\${laneindex}", index.toString());
        return temp;
    }

    @Override
    String fillStringContentWithArrayValues(int index, String temp) {
        if (sample != null) temp = temp.replace("\${sample[${index}]}", sample.name)
        if (libraryID != null) temp = temp.replace("\${library[${index}]}", libraryID.toString())
        if (runID != null) temp = temp.replace("\${run[${index}]}", runID.toString())
        if (laneID != null) temp = temp.replace("\${lane[${index}]}", laneID.toString())
        if (this.index != null) temp = temp.replace("\${laneindex[${index}]}", index.toString())
        return temp;
    }

    @Deprecated
    LaneID getLaneId() {
        return laneID
    }
}
