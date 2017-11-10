/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
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

    public int hashCode() {
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
