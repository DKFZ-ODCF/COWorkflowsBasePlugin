package de.dkfz.b080.co.files;

import de.dkfz.roddy.knowledge.files.BaseFile;

/**
 * Just a simple text file for various purposes
 */
public class TextFile extends BaseFile {
    public TextFile(BaseFile parentFile) {
        super(parentFile);
    }

    public TextFile(BamFile parentFile) { super(parentFile); }

    public TextFile(BaseFile parentFile, String nameSelectionTag, boolean sourceFile) {
        this(parentFile);
        this.overrideFilenameUsingSelectionTag(nameSelectionTag);
        if(sourceFile)
            setAsSourceFile();
    }
}
