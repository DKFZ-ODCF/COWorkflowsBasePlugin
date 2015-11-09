package de.dkfz.b080.co.methods
///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package pipelines.common
//
//import de.dkfz.roddy.core.ExecutionContext
//import de.dkfz.roddy.execution.jobs.JobDependencyID;
//import de.dkfz.roddy.execution.jobs.JobResult;
//import de.dkfz.roddy.knowledge.files.*
//
//import java.util.*;
//
//@groovy.transform.CompileStatic
//class QCPipelineScriptFileServiceHelper {
//
//    static List<LaneFileGroup> bundleFiles(ExecutionContext runningProcess, Sample sample, String runName, List<File> files) {
//        List<File> sortedFiles = [];
//        LinkedHashMap<String, LinkedList<File>> sortedFileGroups = new LinkedHashMap<String, LinkedList<File>>();
//        List<LaneFileGroup> fileGroups = new LinkedList<LaneFileGroup>();
//        if (files.isEmpty()) {
//            return fileGroups;
//        }
//        sortedFiles = files.sort { File f -> f.name }
//
//        boolean[] paired = new boolean[sortedFiles.size()];
//        boolean singleEndProcessing = runningProcess.getConfiguration().getConfigurationValues().getBoolean("useSingleEndProcessing", false);
//        if (singleEndProcessing) {
//            for (int i = 0; i < sortedFiles.size(); i++) {
//                File _f0 = sortedFiles[i];
//                File _f1 = new File(_f0.getAbsolutePath() + "_dummySecondary");
//                String indexFile = "R1";
//                String index2 = "R2";
//                String lane = String.format("L%03d", i);
//                String id = String.format("%s_%s_%s_%s", runningProcess.getDataSet().getId(), sample.getName(), runName, lane, indexFile);
//
//
//                JobResult result = new JobResult(runningProcess, null, JobDependencyID.getFileExistedFakeJob(), false, null, null, null);
//                LinkedList<LaneFile> filesInGroup = new LinkedList<LaneFile>(Arrays.asList(
//                        new LaneFile(_f0, runningProcess, result, null, new FileStageSettings(id, indexFile, 0, runName, sample, runningProcess.getDataSet(), FileStage.INDEXEDLANE)),
//                        new LaneFile(_f1, runningProcess, result, null, new FileStageSettings(id, index2, 1, runName, sample, runningProcess.getDataSet(), FileStage.INDEXEDLANE))));
//                filesInGroup[1].setFileIsValid();
//                fileGroups << new LaneFileGroup(runningProcess, id, runName, sample, filesInGroup)
//            }
//        } else {
//            for (int i = 0; i < sortedFiles.size() - 1; i++) {
//                File _f0 = sortedFiles[i];
//                File _f1 = sortedFiles[i + 1];
//                String f0 = _f0.name;
//                String f1 = _f1.name;
//                int diffCount = 0;
//                if (f0.size() == f1.size()) {
//                    for (int c = 0; c < f0.size(); c++) {
//                        if (f0[c] != f1[c])
//                            diffCount++;
//                    }
//                }
//                if (diffCount > 1) {
//                    continue;   //Files are not equal enough so skip to the next pair
//                } else {
//                    paired[i] = true;    //Detect single files with this
//                    paired[i + 1] = true;
//                    i++;
//                    String[] blocks0 = f0.split("_").reverse();
//                    String[] blocks1 = f1.split("_").reverse(); //Rightmost non unique block is the indexFile
//                    String index0 = "";
//                    String index1 = "";
//                    int indexOfIndex = blocks0.size() - 1;
//                    for (int b = 0; b < blocks0.size(); b++) {
//                        indexOfIndex--;
//                        if (blocks0[b] == blocks1[b]) continue;
//                        index0 = blocks0[b];
//                        index1 = blocks1[b];
//                        break;
//                    }
//                    blocks0 = blocks0.reverse().toList()[0..indexOfIndex];
//                    String id = blocks0.join("_");
//
//                    LinkedList<LaneFile> filesInGroup = new LinkedList<LaneFile>();
//
//                    JobResult result = new JobResult(runningProcess, null, JobDependencyID.getFileExistedFakeJob(), false, null, null, null);
//                    filesInGroup << new LaneFile(_f0, runningProcess, result, null, new FileStageSettings(id, index0, 0, runName, sample, runningProcess.getDataSet(), FileStage.INDEXEDLANE));
//                    filesInGroup << new LaneFile(_f1, runningProcess, result, null, new FileStageSettings(id, index1, 1, runName, sample, runningProcess.getDataSet(), FileStage.INDEXEDLANE));
//
//                    fileGroups << new LaneFileGroup(runningProcess, id, runName, sample, filesInGroup)
//                }
//            }
//        }
//        return fileGroups;
//    }
//}