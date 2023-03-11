package com.hw.lineage.loader.plugin.manager;

import java.util.Iterator;
import java.util.Map;

/**
 * PluginManager is responsible for managing cluster plugins which are loaded using separate class
 * loaders so that their dependencies don't interfere with Lineage's dependencies.
 *
 * @description: PluginManager
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface PluginManager {

    /**
     * Returns in pluginId->iterator over all available implementations of the given service interface (SPI)
     * in echo plugin known to this plugin manager instance.
     *
     * @param service the service interface (SPI) for which implementations are requested.
     * @param <P>     Type of the requested plugin service.
     * @return Map<pluginId, iterator>
     */
    <P> Map<String, Iterator<P>> load(Class<P> service);

}
