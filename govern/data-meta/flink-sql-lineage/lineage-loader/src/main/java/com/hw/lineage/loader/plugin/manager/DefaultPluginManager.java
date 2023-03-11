package com.hw.lineage.loader.plugin.manager;

import com.hw.lineage.loader.plugin.PluginDescriptor;
import com.hw.lineage.loader.plugin.PluginLoader;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;

/**
 * @description: Default implementation of {@link PluginManager}.
 * @author: HamaWhite
 * @version: 1.0.0
 */
@ThreadSafe
public class DefaultPluginManager implements PluginManager {

    /**
     * Parent-classloader to all classloader that are used for plugin loading. We expect that this
     * is thread-safe.
     */
    private final ClassLoader parentClassLoader;

    /**
     * A collection of descriptions of all plugins known to this plugin manager.
     */
    private final Collection<PluginDescriptor> pluginDescriptors;

    /**
     * List of patterns for classes that should always be resolved from the parent ClassLoader.
     */
    private final String[] alwaysParentFirstPatterns;

    public DefaultPluginManager(
            Collection<PluginDescriptor> pluginDescriptors, String[] alwaysParentFirstPatterns) {
        this(
                pluginDescriptors,
                DefaultPluginManager.class.getClassLoader(),
                alwaysParentFirstPatterns);
    }

    public DefaultPluginManager(
            Collection<PluginDescriptor> pluginDescriptors,
            ClassLoader parentClassLoader,
            String[] alwaysParentFirstPatterns) {
        this.pluginDescriptors = pluginDescriptors;
        this.parentClassLoader = parentClassLoader;
        this.alwaysParentFirstPatterns = alwaysParentFirstPatterns;
    }

    @Override
    public <P> Map<String, Iterator<P>> load(Class<P> service) {
        Map<String, Iterator<P>> pluginIteratorMap = new HashMap<>(pluginDescriptors.size());
        for (PluginDescriptor pluginDescriptor : pluginDescriptors) {
            PluginLoader pluginLoader =
                    PluginLoader.create(
                            pluginDescriptor, parentClassLoader, alwaysParentFirstPatterns);
            pluginIteratorMap.put(pluginDescriptor.getPluginId(), pluginLoader.load(service));
        }
        return pluginIteratorMap;
    }

    @Override
    public String toString() {
        return "PluginManager{"
                + "parentClassLoader="
                + parentClassLoader
                + ", pluginDescriptors="
                + pluginDescriptors
                + ", alwaysParentFirstPatterns="
                + Arrays.toString(alwaysParentFirstPatterns)
                + '}';
    }
}
