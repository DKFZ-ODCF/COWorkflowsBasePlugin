/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE.txt).
 */
package de.dkfz.b080.co.knowledge.metadata.sampleextractorstrategies

import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyConversionHelperMethods
import groovy.transform.CompileStatic

@CompileStatic
class SampleFromFilenameExtractorVersionTwo extends SampleFromFilenameExtractor {

    private static LoggerWrapper logger = LoggerWrapper.getLogger(SampleFromFilenameExtractorVersionTwo.class.getName())

    boolean matchExactSampleNames

    boolean allowSampleTerminationWithIndex

    boolean useLowerCaseFilenamesForSampleExtraction

    String filename

    String[] splitFilename

    List<String> possibleSamplePrefixes

    SampleFromFilenameExtractorVersionTwo(File file, List<String> possibleSamplePrefixes, boolean matchExactSampleNames, boolean allowSampleTerminationWithIndex, boolean useLowerCaseFilenamesForSampleExtraction) {
        super(file)
        this.possibleSamplePrefixes = prepareListOfPossibleSamples(possibleSamplePrefixes)
        this.matchExactSampleNames = matchExactSampleNames
        this.allowSampleTerminationWithIndex = allowSampleTerminationWithIndex
        this.useLowerCaseFilenamesForSampleExtraction = useLowerCaseFilenamesForSampleExtraction
        this.filename = prepareFilename()
        this.splitFilename = filename.split(StringConstants.SPLIT_UNDERSCORE)
    }

    @Override
    String extract() {
        String extractedSampleName = findFirstMatchOfSampleInPossibleSamplesList(filename)

        if (!matchExactSampleNames && extractedSampleName) {
            String[] sampleSplit = extractedSampleName.split(StringConstants.SPLIT_UNDERSCORE)
            if (sampleSplit.last().isInteger()) {
                extractedSampleName = extractedSampleName[0..-3]
                allowSampleTerminationWithIndex = true
            }
        }

        if (matchExactSampleNames && extractedSampleName == null)
            return null

        extractedSampleName = adjustExtractedSampleValueToFilename(extractedSampleName, splitFilename)

        if (allowSampleTerminationWithIndex)
            extractedSampleName = "check for index in filename and adjust extracted sample name if necessary"(filename, extractedSampleName)
        return extractedSampleName
    }

    List<String> prepareListOfPossibleSamples(List<String> possibleSampleNames) {
        // Get list of all known samples. Sort and revert them, so we can use them properly.
        // Get rid of empty samples! Sort and reverse for first match searches. (e.g. control_abc comes before control!)
        possibleSampleNames
                .findAll()
                .sort()
                .reverse()
    }

    String prepareFilename() {
        String filename = file.name
        if (useLowerCaseFilenamesForSampleExtraction)
            filename = filename.toLowerCase()
        return filename
    }

    /** Note, that this function uses matchExactSampleNames only as guideline, but if the
     *  filename ends with an underscore or an integer, it does not match exactly, but allows
     *  for extension of the resulting match with an underscore.
     *
     * @param filename
     * @return Some kind of possibly extended sample name prefix.
     */
    String findFirstMatchOfSampleInPossibleSamplesList(String filename) {
        possibleSamplePrefixes.find { String sampleID ->
            String optionalTerminalUnderscore
            if (matchExactSampleNames) {
                if (sampleID.endsWith("_")) {
                    logger.always("A sample name was given with an underscore at the end. We assume this is intentional and set matchExactSampleNames for '$sampleID'")
                    optionalTerminalUnderscore = ""
                } else {
                    optionalTerminalUnderscore = "_"
                }
            } else {
                optionalTerminalUnderscore = ""
                String[] sampleSplit = sampleID.split(StringConstants.SPLIT_UNDERSCORE)
                if (sampleSplit.last().isInteger()) {
                    logger.always("A sample name was given with an index at the end. We assume this is intentional and set allowSampleTerminationWithIndex for '$sampleID'")
                    sampleID = sampleID[0..-2]
                }
            }
            filename.startsWith(sampleID + optionalTerminalUnderscore)
        }
    }

    String adjustExtractedSampleValueToFilename(String extractedSampleName, String[] splitFilename) {
        if (extractedSampleName == null) // Fallback, in case we do not want an exact match but did not find anything.
            extractedSampleName = splitFilename[0]
        else {
            if (extractedSampleName.endsWith("_"))
                extractedSampleName = extractedSampleName[0..-2] // In this special case, we remove the "_" char at the end.

            // A sample can contain underscore characters "_". Here, we make sure, that the whole samplename is returned
            // by including the count of underscore characters in the sample name.
            extractedSampleName = splitFilename[0..(extractedSampleName.count("_"))].join("_")
        }
        extractedSampleName
    }

    String "check for index in filename and adjust extracted sample name if necessary"(String filename, String extractedSampleName) {
        String indexPart = filename[(extractedSampleName.size() + 1)..-1].split(StringConstants.SPLIT_UNDERSCORE)[0]
        if (RoddyConversionHelperMethods.isInteger(indexPart)) {
            extractedSampleName += "_$indexPart"
        }
        extractedSampleName
    }
}
