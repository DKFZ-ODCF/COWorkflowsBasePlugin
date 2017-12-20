/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.b080.co.common

import groovy.transform.InheritConstructors

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

@InheritConstructors
class LibraryID extends Identifier {}

@InheritConstructors
class RunID extends Identifier {}

@InheritConstructors
class LaneID extends Identifier {}

@InheritConstructors
class IndexID extends Identifier {}
