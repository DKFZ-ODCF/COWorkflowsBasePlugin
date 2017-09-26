/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/COWorkflowsBasePlugin/LICENSE.txt).
 */

package de.dkfz.b080.co.files;

import de.dkfz.roddy.knowledge.files.BaseFile;

/**
 * Created by heinold on 14.01.16.
 */
public class BasicBamFile extends COBaseFile {
    public BasicBamFile(ConstructionHelperForBaseFiles helper) {
        super(helper);
    }

    public BasicBamFile(BaseFile parent) {
        super(parent);
    }
}
