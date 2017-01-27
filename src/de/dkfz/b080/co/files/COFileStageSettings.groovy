package de.dkfz.b080.co.files;

import de.dkfz.b080.co.common.IndexID;
import de.dkfz.b080.co.common.LaneID;
import de.dkfz.b080.co.common.LibraryID;
import de.dkfz.b080.co.common.RunID;
import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.knowledge.files.FileStage;
import de.dkfz.roddy.knowledge.files.FileStageSettings
import groovy.transform.CompileStatic;

@CompileStatic
public class COFileStageSettings extends FileStageSettings<COFileStageSettings> {

    private final LaneID laneId;
    private final IndexID index;
    private final int numericIndex;
    private final RunID runID;
    private final Sample sample;
    private final LibraryID libraryID;

    // Convenience constructor
    public COFileStageSettings(String laneId, String index, int numericIndex, String runID, String libraryID, Sample sample, DataSet dataSet, FileStage stage) {
        this(laneId == null ? (LaneID) null : new LaneID(laneId),
                index == null ? (IndexID) null : new IndexID(index),
                numericIndex,
                runID == null ? (RunID) null : new RunID(runID),
                libraryID == null ? (LibraryID) null : new LibraryID(libraryID),
                sample,
                dataSet,
                stage)
    }

    public COFileStageSettings(LaneID laneId, IndexID index, int numericIndex, RunID runID, LibraryID libraryID, Sample sample, DataSet dataSet, FileStage stage) {
        super(dataSet, stage)
        this.laneId = laneId
        this.index = index
        this.numericIndex = numericIndex
        this.runID = runID
        this.libraryID = libraryID
        this.sample = sample
    }

    public COFileStageSettings(LaneID laneId, RunID runID, LibraryID libraryID, Sample sample, DataSet dataSet) {
        this(laneId, null, -1, runID, libraryID, sample, dataSet, COFileStage.LANE);
    }

    public COFileStageSettings(RunID runID, LibraryID libraryID, Sample sample, DataSet dataSet) {
        this(null, null, -1, runID, libraryID, sample, dataSet, COFileStage.LANE);
    }

    public COFileStageSettings(LibraryID libraryID, Sample sample, DataSet dataSet) {
        this(null, null, -1, null, libraryID, sample, dataSet, COFileStage.LIBRARY);
    }

    public COFileStageSettings(Sample sample, DataSet dataSet) {
        this(null, null, -1, null, (LibraryID) null, sample, dataSet, COFileStage.SAMPLE);
    }

    public COFileStageSettings(DataSet dataSet) {
        this(null, null, -1, null, (LibraryID) null, null, dataSet, COFileStage.PID);
    }

    public LaneID getLaneId() {
        return laneId;
    }

    public IndexID getIndex() {
        return index;
    }

    public int getNumericIndex() {
        return numericIndex;
    }

    public RunID getRunID() {
        return runID;
    }

    public Sample getSample() {
        return sample;
    }

    public String getLibraryID() { return libraryID; }

    @Override
    public COFileStageSettings copy() {
        return new COFileStageSettings(laneId, index, numericIndex, runID, libraryID, sample, dataSet, stage);
    }

    @Override
    public COFileStageSettings decreaseLevel() {
        if (stage == COFileStage.INDEXEDLANE)
            return new COFileStageSettings(laneId, runID, libraryID, sample, dataSet);
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
    public String getIDString() {
        if (stage == COFileStage.INDEXEDLANE)
            return String.format("%s_%s_%s_%s_%s_%s", dataSet, sample.getName(), libraryID, runID, laneId, index);
        if (stage == COFileStage.LANE)
            return String.format("%s_%s_%s_%s_%s", dataSet, sample.getName(), libraryID, runID, laneId);
        if (stage == COFileStage.RUN)
            return String.format("%s_%s_%s_%s", dataSet, sample.getName(), libraryID, runID);
        if (stage == COFileStage.LIBRARY)
            return String.format("%s_%s_%s", dataSet, sample.getName(), libraryID);
        if (stage == COFileStage.SAMPLE)
            return String.format("%s_%s", dataSet, sample.getName(), dataSet);
        return String.format("%s", dataSet);
    }

    @Override
    public String fillStringContent(String temp) {
        if (sample != null) temp = temp.replace("\${sample}", getSample().getName().toString());
        if (libraryID != null) temp = temp.replace("\${library}", getLibraryID().toString());
        if (runID != null) temp = temp.replace("\${run}", getRunID().toString());
        if (laneId != null) temp = temp.replace("\${lane}", getLaneId().toString());
        if (index != null) temp = temp.replace("\${laneindex}", getIndex().toString());
        return temp;
    }

    @Override
    public String fillStringContentWithArrayValues(int index, String temp) {
        if (sample != null) temp = temp.replace("\${sample[${index}]}", getSample().getName())
        if (libraryID != null) temp = temp.replace("\${library[${index}]}", getLibraryID().toString())
        if (runID != null) temp = temp.replace("\${run[${index}]}", getRunID().toString())
        if (laneId != null) temp = temp.replace("\${lane[${index}]}", getLaneId().toString())
        if (this.index != null) temp = temp.replace("\${laneindex[${index}]}", getIndex().toString())
        return temp;
    }
}
