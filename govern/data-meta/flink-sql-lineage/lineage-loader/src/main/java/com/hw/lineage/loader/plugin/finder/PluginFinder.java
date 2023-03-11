package com.hw.lineage.loader.plugin.finder;

import com.hw.lineage.loader.plugin.PluginDescriptor;
import com.hw.lineage.loader.plugin.PluginLoader;

import java.io.IOException;
import java.util.Collection;

/**
 * Implementations of this interface provide mechanisms to locate plugins and create corresponding
 * {@link PluginDescriptor} objects. The result can then be used to initialize a {@link
 * PluginLoader}.
 *
 * @description: PluginFinder
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface PluginFinder {

    /**
     * Find plugins and return a corresponding collection of {@link PluginDescriptor} instances.
     *
     * @return a collection of {@link PluginDescriptor} instances for all found plugins.
     * @throws IOException thrown if a problem occurs during plugin search.
     */
    Collection<PluginDescriptor> findPlugins() throws IOException;
}
