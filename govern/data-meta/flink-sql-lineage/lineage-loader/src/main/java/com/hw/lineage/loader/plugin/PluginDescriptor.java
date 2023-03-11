package com.hw.lineage.loader.plugin;

import java.net.URL;
import java.util.Arrays;

/**
 * @description: Descriptive meta information for a plugin.
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class PluginDescriptor {

    /**
     * Unique identifier of the plugin.
     */
    private final String pluginId;

    /**
     * URLs to the plugin resources code. Usually this contains URLs of the jars that will be loaded
     * for the plugin.
     */
    private final URL[] pluginResourceURLs;

    /**
     * String patterns of classes that should be excluded from loading out of the plugin resources.
     * See ChildFirstClassLoader's field alwaysParentFirstPatterns.
     */
    private final String[] loaderExcludePatterns;

    public PluginDescriptor(
            String pluginId, URL[] pluginResourceURLs, String[] loaderExcludePatterns) {
        this.pluginId = pluginId;
        this.pluginResourceURLs = pluginResourceURLs;
        this.loaderExcludePatterns = loaderExcludePatterns;
    }

    public String getPluginId() {
        return pluginId;
    }

    public URL[] getPluginResourceURLs() {
        return pluginResourceURLs;
    }

    public String[] getLoaderExcludePatterns() {
        return loaderExcludePatterns;
    }

    @Override
    public String toString() {
        return "PluginDescriptor{"
                + "pluginId='"
                + pluginId
                + '\''
                + ", pluginResourceURLs="
                + Arrays.toString(pluginResourceURLs)
                + ", loaderExcludePatterns="
                + Arrays.toString(loaderExcludePatterns)
                + '}';
    }
}
