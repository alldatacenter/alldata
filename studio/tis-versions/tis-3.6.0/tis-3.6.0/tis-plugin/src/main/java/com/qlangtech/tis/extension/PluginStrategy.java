/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.extension;

import com.qlangtech.tis.TIS;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface PluginStrategy {

    String FILE_NAME_timestamp2 = ".timestamp2";
    String KEY_MANIFEST_DEPENDENCIES = "Plugin-Dependencies";
    String KEY_MANIFEST_SHORTNAME = "Short-Name";
    String KEY_MANIFEST_PLUGIN_FIRST_CLASSLOADER = "PluginFirstClassLoader";
    String KEY_LAST_MODIFY_TIME = "Last-Modify-Time";

    String KEY_MANIFEST_PLUGIN_VERSION = "Plugin-Version";

    <T> List<ExtensionComponent<T>> findComponents(Class<T> extensionType, TIS tis);

    void updateDependency(PluginWrapper depender, PluginWrapper dependee);

    /**
     * Creates a plugin wrapper, which provides a management interface for the plugin
     *
     * @param archive Either a directory that points to a pre-exploded plugin, or an jpi file, or an jpl file.
     */
    PluginWrapper createPluginWrapper(File archive) throws IOException;

    /**
     * Finds the plugin name without actually unpacking anything {@link #createPluginWrapper} would.
     */
    String getShortName(File archive) throws IOException;

    /**
     * Loads the plugin and starts it.
     *
     * <p>
     * This should be done after all the classloaders are constructed for all
     * the plugins, so that dependencies can be properly loaded by plugins.
     */
    void load(PluginWrapper wrapper) throws IOException;

    /**
     * Optionally start services provided by the plugin. Should be called
     * when all plugins are loaded.
     *
     * @param plugin
     */
    void initializeComponents(PluginWrapper plugin);
}
