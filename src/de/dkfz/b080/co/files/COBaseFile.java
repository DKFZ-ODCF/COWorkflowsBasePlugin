package de.dkfz.b080.co.files;

import de.dkfz.roddy.knowledge.files.BaseFile;

/**
 */
public abstract class COBaseFile<COFileStageSettings> extends BaseFile {
    public COBaseFile(ConstructionHelperForBaseFiles helper) {
        super(helper);
    }

    public COBaseFile(BaseFile parent) {
        super(parent);
    }
}
