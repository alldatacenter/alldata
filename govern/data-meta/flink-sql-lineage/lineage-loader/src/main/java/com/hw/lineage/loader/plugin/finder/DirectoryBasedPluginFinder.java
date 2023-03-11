package com.hw.lineage.loader.plugin.finder;

import com.hw.lineage.common.util.function.FunctionUtils;
import com.hw.lineage.loader.plugin.PluginDescriptor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is used to create a collection of {@link PluginDescriptor} based on directory
 * structure for a given plugin root folder.
 *
 * <p>The expected structure is as follows: the given plugins root folder, containing the plugins
 * folder. One plugin folder contains all resources (jar files) belonging to a plugin. The name of
 * the plugin folder becomes the plugin id.
 *
 * <pre>
 * plugins-root-folder/
 *            |------------plugin-a/ (folder of plugin a)
 *            |                |-plugin-a-1.jar
 *            |                |-plugin-a-2.jar
 *            |                |-lib /
 *            |                     |-plugin-a-lib-1.jar
 *            |                     |-plugin-a-lib-2.jar
 *            |
 *            |------------plugin-b/
 *            |                |-plugin-b-1.jar
 *           ...               |-...
 * </pre>
 *
 * @description: DirectoryBasedPluginFinder
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class DirectoryBasedPluginFinder implements PluginFinder {

    /**
     * Pattern to match jar files in a directory.
     */
    private static final String JAR_MATCHER_PATTERN = "glob:**.jar";

    /**
     * Root directory to the plugin folders.
     */
    private final Path pluginsRootDir;

    /**
     * Matcher for jar files in the filesystem of the root folder.
     */
    private final PathMatcher jarFileMatcher;

    public DirectoryBasedPluginFinder(Path pluginsRootDir) {
        this.pluginsRootDir = pluginsRootDir;
        this.jarFileMatcher = pluginsRootDir.getFileSystem().getPathMatcher(JAR_MATCHER_PATTERN);
    }

    @Override
    public Collection<PluginDescriptor> findPlugins() throws IOException {

        if (!Files.isDirectory(pluginsRootDir)) {
            throw new IOException(
                    "Plugins root directory [" + pluginsRootDir + "] does not exist!");
        }
        try (Stream<Path> stream = Files.list(pluginsRootDir)) {
            return stream.filter(Files::isDirectory)
                    .map(
                            FunctionUtils.uncheckedFunction(
                                    this::createPluginDescriptorForSubDirectory))
                    .collect(Collectors.toList());
        }
    }

    private PluginDescriptor createPluginDescriptorForSubDirectory(Path subDirectory)
            throws IOException {
        URL[] urls = createJarURLsFromDirectory(subDirectory);
        Arrays.sort(urls, Comparator.comparing(URL::toString));
        // the plugin directories.
        return new PluginDescriptor(subDirectory.getFileName().toString(), urls, new String[0]);
    }

    /**
     * Recursively traverse all jars in the subDirectory
     */
    private URL[] createJarURLsFromDirectory(Path subDirectory) throws IOException {
        try (Stream<Path> stream = Files.walk(subDirectory)) {
            URL[] urls =
                    stream.filter((Path p) -> Files.isRegularFile(p) && jarFileMatcher.matches(p))
                            .map(FunctionUtils.uncheckedFunction((Path p) -> p.toUri().toURL()))
                            .toArray(URL[]::new);

            if (urls.length < 1) {
                throw new IOException(
                        "Cannot find any jar files for plugin in directory ["
                                + subDirectory
                                + "]."
                                + " Please provide the jar files for the plugin or delete the directory.");
            }

            return urls;
        }
    }
}
