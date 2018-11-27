/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/COWorkflowsBasePlugin/LICENSE).
 */
package de.dkfz.b080.co.common

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

@CompileStatic
class Identifier {

    private String value

    Identifier(String value) {
        assert(value != null)
        this.value = value
    }

    String toString() {
        return value
    }

    int hashCode() {
        return value.hashCode()
    }

}

@CompileStatic
@InheritConstructors
class LibraryID extends Identifier {
    LibraryID(String value) {
        super(value)
    }
}

@CompileStatic
@InheritConstructors
class RunID extends Identifier {
    RunID(String value) {
        super(value)
    }
}

@CompileStatic
@InheritConstructors
class LaneID extends Identifier {
    LaneID(String value) {
        super(value)
    }
}

@CompileStatic
@InheritConstructors
class IndexID extends Identifier {
    IndexID(String value) {
        super(value)
    }
}
