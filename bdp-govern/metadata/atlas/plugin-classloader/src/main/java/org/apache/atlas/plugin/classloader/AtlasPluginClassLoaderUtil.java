/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.plugin.classloader;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 * AtlasPluginClassLoaderUtil used by AtlasPluginClassLoader.
 */
final class AtlasPluginClassLoaderUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasPluginClassLoaderUtil.class);

    private static final String ATLAS_PLUGIN_LIBDIR = "atlas-%-plugin-impl";

    private AtlasPluginClassLoaderUtil(){ }

    public static URL[] getFilesInDirectories(String[] libDirs) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasPluginClassLoaderUtil.getFilesInDirectories()");
        }

        List<URL> ret = new ArrayList<>();

        for (String libDir : libDirs) {
            getFilesInDirectory(libDir, ret);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasPluginClassLoaderUtil.getFilesInDirectories(): {} files", ret.size());
        }

        return ret.toArray(new URL[]{});
    }

    private static void getFilesInDirectory(String dirPath, List<URL> files) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasPluginClassLoaderUtil.getPluginFiles()");
        }

        if (dirPath != null) {
            try {
                File[] dirFiles = new File(dirPath).listFiles();

                if (dirFiles != null) {
                    for (File dirFile : dirFiles) {
                        try {
                            URL jarPath = dirFile.toURI().toURL();

                            if (LOG.isDebugEnabled()) {
                                LOG.debug("getFilesInDirectory('{}'): adding {}", dirPath, dirFile.getAbsolutePath());
                            }

                            files.add(jarPath);
                        } catch (Exception excp) {
                            LOG.warn("getFilesInDirectory('{}'): failed to get URI for file {}", dirPath, dirFile
                                    .getAbsolutePath(), excp);
                        }
                    }
                }
            } catch (Exception excp) {
                LOG.warn("getFilesInDirectory('{}'): error", dirPath, excp);
            }
        } else {
            LOG.warn("getFilesInDirectory('{}'): could not find directory in path {}", dirPath, dirPath);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasPluginClassLoaderUtil.getFilesInDirectory({})", dirPath);
        }
    }

    public static String getPluginImplLibPath(String pluginType, Class<?> pluginClass) throws URISyntaxException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasPluginClassLoaderUtil.getPluginImplLibPath for Class ({})", pluginClass.getName());
        }

        URI uri = pluginClass.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path path = Paths.get(URI.create(uri.toString()));
        String ret = path.getParent().toString() + File.separatorChar + ATLAS_PLUGIN_LIBDIR.replaceAll("%", pluginType);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasPluginClassLoaderUtil.getPluginImplLibPath for Class {}): {})", pluginClass.getName(), ret);
        }

        return ret;
    }
}
