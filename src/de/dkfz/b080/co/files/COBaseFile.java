/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
 */

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
