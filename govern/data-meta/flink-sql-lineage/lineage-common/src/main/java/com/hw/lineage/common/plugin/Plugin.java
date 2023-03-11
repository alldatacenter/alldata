package com.hw.lineage.common.plugin;

import static com.hw.lineage.common.util.Preconditions.checkNotNull;

/**
 * Interface for plugins. Plugins typically extend this interface in their SPI and the concrete
 * implementations of a service then implement the SPI contract.
 *
 * @description: Plugin
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface Plugin {

    /**
     * Helper method to get the class loader used to load the plugin. This may be needed for some
     * plugins that use dynamic class loading afterwards the plugin was loaded.
     *
     * @return the class loader used to load the plugin.
     */
    default ClassLoader getClassLoader() {
        return checkNotNull(
                this.getClass().getClassLoader(),
                "%s plugin with null class loader",
                this.getClass().getName());
    }
}
