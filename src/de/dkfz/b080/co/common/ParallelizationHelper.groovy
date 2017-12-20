package de.dkfz.b080.co.common

import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.jobs.JobManager
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.knowledge.files.IndexedFileObjects
import de.dkfz.roddy.knowledge.methods.GenericMethod
import groovy.transform.CompileStatic

import java.util.stream.Stream

/**
 * Created by heinold on 13.01.16.
 */
@CompileStatic
class ParallelizationHelper {

    /**
     * This code fragment runs thing in parallel. The run is different for various runtime systems (Local, PBS).
    */
    static IndexedFileObjects runParallel(String indicesID, String toolID, BaseFile firstFile, BaseFile otherFile, String indexParameterName, LinkedHashMap<String,String> parameters = [:]) {
        ExecutionContext executionContext = firstFile.getExecutionContext();
        List<String> indices = executionContext.getConfiguration().getConfigurationValues().get(indicesID).toStringList();

        //First one executes locally or via ssh but without a cluster system.
        Stream<String> stream = JobManager.getInstance().executesWithoutJobSystem() ? indices.parallelStream() : indices.stream();
        Map<String, FileObject> map = stream.collect { index ->
            LinkedHashMap<String, String> indexMap = new LinkedHashMap((indexParameterName): index)
            indexMap.putAll(parameters)
            new MapEntry(index, callWithOptionalSecondaryBam(toolID, firstFile, otherFile, indexMap))
        }.collectEntries()

        return new IndexedFileObjects(indices, map, executionContext);
    }

    /**
     * Called within runParallel
     */
    static FileObject callWithOptionalSecondaryBam(String toolID, BaseFile THIS, BaseFile otherBam, LinkedHashMap<String,String> parameters = [:]) {
        if(otherBam == null)
            return GenericMethod.callGenericTool(toolID, THIS, parameters)
        else
            return GenericMethod.callGenericTool(toolID, THIS, otherBam, parameters)
    }
}
