package de.dkfz.b080.co.files;

import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.knowledge.files.FileStage;
import de.dkfz.roddy.knowledge.files.FileStageSettings;

/**
 */
public class COFileStageSettings extends FileStageSettings<COFileStageSettings> {

    private final String laneId;
    private final String index;
    private final int numericIndex;
    private final String runID;
    private final Sample sample;
    private final String libraryID;

    public COFileStageSettings(String laneId, String index, int numericIndex, String runID, String libraryID, Sample sample, DataSet dataSet, FileStage stage) {
        super(dataSet, stage);
        this.laneId = laneId;
        this.index = index;
        this.numericIndex = numericIndex;
        this.runID = runID;
        this.libraryID = libraryID;
        this.sample = sample;
    }

    public COFileStageSettings(String laneId, String runID, String libraryID, Sample sample, DataSet dataSet) {
        this(laneId, "", -1, runID, libraryID, sample, dataSet, COFileStage.LANE);
    }

    public COFileStageSettings(String runID, String libraryID, Sample sample, DataSet dataSet) {
        this("", "", -1, runID, libraryID, sample, dataSet, COFileStage.LANE);
    }

    public COFileStageSettings(String libraryID, Sample sample, DataSet dataSet) {
        this("", "", -1, "", libraryID, sample, dataSet, COFileStage.LIBRARY);
    }

    public COFileStageSettings(Sample sample, DataSet dataSet) {
        this("", "", -1, "", "", sample, dataSet, COFileStage.SAMPLE);
    }

    public COFileStageSettings(DataSet dataSet) {
        this("", "", -1, "", "", null, dataSet, COFileStage.PID);
    }

    public String getLaneId() {
        return laneId;
    }

    public String getIndex() {
        return index;
    }

    public int getNumericIndex() {
        return numericIndex;
    }

    public String getRunID() {
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
        if (sample != null) temp = temp.replace("${sample}", getSample().getName());
        if (libraryID != null) temp = temp.replace("${library}", getLibraryID());
        if (runID != null) temp = temp.replace("${run}", getRunID());
        if (laneId != null) temp = temp.replace("${lane}", getLaneId());
        if (index != null) temp = temp.replace("${laneindex}", getIndex());
        return temp;
    }

    @Override
    public String fillStringContentWithArrayValues(int index, String temp) {
        if (sample != null) temp = temp.replace(String.format("${sample[%d]}", index), getSample().getName());
        if (libraryID != null) temp = temp.replace(String.format("${library[%d]}", index), getLibraryID());
        if (runID != null) temp = temp.replace(String.format("${run[%d]}", index), getRunID());
        if (laneId != null) temp = temp.replace(String.format("${lane[%d]}", index), getLaneId());
        if (this.index != null) temp = temp.replace(String.format("${laneindex[%d]}", index), getIndex());
        return temp;
    }
}
