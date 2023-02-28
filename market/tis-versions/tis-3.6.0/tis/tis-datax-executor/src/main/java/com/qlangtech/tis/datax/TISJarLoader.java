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

package com.qlangtech.tis.datax;

import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.extension.UberClassLoader;

import java.net.URL;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-15 06:40
 **/
public class TISJarLoader extends com.alibaba.datax.core.util.container.JarLoader {
    private final PluginManager pluginManager;

    public TISJarLoader(PluginManager pluginManager) {
        super(new String[]{"."});
        this.pluginManager = pluginManager;
    }

    public URL getResource(String name) {
        URL url = pluginManager.uberClassLoader.getResource(name);
        if (url == null) {
            return super.getResource(name);
        }
        return url;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            UberClassLoader classLoader = pluginManager.uberClassLoader;
            return classLoader.findClass(name);
        } catch (Throwable e) {
            throw new RuntimeException("className:" + name + ",scan the plugins:"
                    + pluginManager.activePlugins.stream().map((p) -> p.getDisplayName()).collect(Collectors.joining(",")), e);
        }
    }
}
