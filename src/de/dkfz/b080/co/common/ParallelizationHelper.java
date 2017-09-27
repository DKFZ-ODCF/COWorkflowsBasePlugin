/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.common;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.jobs.JobManager;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileObject;
import de.dkfz.roddy.knowledge.files.IndexedFileObjects;
import de.dkfz.roddy.knowledge.methods.GenericMethod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by heinold on 13.01.16.
 */
public class ParallelizationHelper {

    /**
     * This code fragment runs thing in parallel. The run is different for various runtime systems (Local, PBS).
    */
    public static IndexedFileObjects runParallel(String indicesID, String toolID, BaseFile firstFile, BaseFile otherFile, String indexParameterName) {
        ExecutionContext executionContext = firstFile.getExecutionContext();
        List<String> indices = executionContext.getConfiguration().getConfigurationValues().get(indicesID).toStringList();
        Map<String, FileObject> map = new LinkedHashMap<>();

        //First one executes locally or via ssh but without a cluster system.
        Stream<String> stream = JobManager.getInstance().executesWithoutJobSystem() ? indices.parallelStream() : indices.stream();
        stream.forEach(index -> callWithIndex(toolID, index, indexParameterName, map, firstFile, otherFile));

        return new IndexedFileObjects(indices, map, executionContext);
    }

    /**
     * Called within runParallel
     */
    public static void callWithIndex(String toolID, String index, String indexParameterName, Map<String, FileObject> map, BaseFile THIS, BaseFile otherBam ) {
        FileObject callResult;
        if(otherBam == null)
            callResult = GenericMethod.callGenericTool(toolID, THIS, indexParameterName + index);
        else
            callResult = GenericMethod.callGenericTool(toolID, THIS, otherBam, indexParameterName + index);
        synchronized (map) {
            map.put(index, callResult);
        }
    }
}
