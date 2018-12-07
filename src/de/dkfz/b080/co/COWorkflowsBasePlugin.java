/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/COWorkflowsBasePlugin/LICENSE).
 */
package de.dkfz.b080.co;

import de.dkfz.roddy.plugins.BasePlugin;

/**

 * TODO Recreate class. Put in dependencies to other workflows, descriptions, capabilities (like ui settings, components) etc.
 */
public class COWorkflowsBasePlugin extends BasePlugin {

    public static final String CURRENT_VERSION_STRING = "1.2.3";
    public static final String CURRENT_VERSION_BUILD_DATE = "Wed Nov 14 15:10:04 CET 2018";

    @Override
    public String getVersionInfo() {
        return "Roddy plugin: " + this.getClass().getName() + ", V " + CURRENT_VERSION_STRING + " built at " + CURRENT_VERSION_BUILD_DATE;
    }
}
