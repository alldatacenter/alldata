/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.extension;

import com.qlangtech.tis.extension.util.ClassLoaderReflectionToolkit;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * {@link ClassLoader} that can see all plugins.
 */
public final class UberClassLoader extends ClassLoader {

    private PluginManager pluginManager;
    /**
     * Make generated types visible.
     * Keyed by the generated class name.
     */
    private ConcurrentMap<String, WeakReference<Class>> generatedClasses = new ConcurrentHashMap<String, WeakReference<Class>>();

    private final Set<String> acceptedPlugins;

    /**
     * Cache of loaded, or known to be unloadable, classes.
     */
    public final Map<String, Class<?>> loaded = new HashMap<String, Class<?>>();

    public UberClassLoader(PluginManager pluginManager) {
        this(pluginManager, null);
    }

    public UberClassLoader(PluginManager pluginManager, Set<String> acceptedPlugins) {
        super(PluginManager.class.getClassLoader());
        this.pluginManager = pluginManager;
        this.acceptedPlugins = acceptedPlugins;
    }

    public void addNamedClass(String className, Class c) {
        generatedClasses.put(className, new WeakReference<Class>(c));
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        WeakReference<Class> wc = generatedClasses.get(name);
        if (wc != null) {
            Class c = wc.get();
            if (c != null) {
                return c;
            } else {
                generatedClasses.remove(name, wc);
            }

        }
        if (name.startsWith("SimpleTemplateScript")) {
            // cf. groovy.text.SimpleTemplateEngine
            throw new ClassNotFoundException("ignoring " + name);
        }
        synchronized (loaded) {
            Class<?> c = null;
            if ((c = loaded.get(name)) != null) {
                // Class<?> c = loaded.get(name);
                return c;
//                if (c != null) {
//                    return c;
//                } else {
//                    throw new ClassNotFoundException("cached miss for " + name);
//                }
            }
        }
        if (PluginManager.FAST_LOOKUP) {
            for (PluginWrapper p : pluginManager.activePlugins) {
                if (!accept(p)) {
                    continue;
                }
                try {
                    Class<?> c = ClassLoaderReflectionToolkit._findLoadedClass(p.classLoader, name);
                    if (c != null) {
                        synchronized (loaded) {
                            loaded.put(name, c);
                        }
                        return c;
                    }
                    // calling findClass twice appears to cause LinkageError: duplicate class def
                    c = ClassLoaderReflectionToolkit._findClass(p.classLoader, name);
                    synchronized (loaded) {
                        loaded.put(name, c);
                    }
                    return c;
                } catch (ClassNotFoundException e) {
                    // not found. try next
                }
            }
        } else {
            for (PluginWrapper p : pluginManager.activePlugins) {
                try {
                    return p.classLoader.loadClass(name);
                } catch (ClassNotFoundException e) {
                    // not found. try next
                }
            }
        }
        synchronized (loaded) {
            loaded.put(name, null);
        }
        // not found in any of the classloader. delegate.
        throw new ClassNotFoundException(name
                + (acceptedPlugins != null
                ? "\naccepted plugins:" + acceptedPlugins.stream().collect(Collectors.joining(","))
                : StringUtils.EMPTY)
                + "\n,scan plugins:"
                + pluginManager.activePlugins.stream().map((p) -> p.getShortName()).collect(Collectors.joining(",")));
    }

    @Override
    protected URL findResource(String name) {
        if (PluginManager.FAST_LOOKUP) {
            for (PluginWrapper p : pluginManager.activePlugins) {
                URL url = null;
                if (accept(p)) {
                    url = ClassLoaderReflectionToolkit._findResource(p.classLoader, name);
                }
                if (url != null) {
                    return url;
                }
            }
        } else {
            for (PluginWrapper p : pluginManager.activePlugins) {
                URL url = p.classLoader.getResource(name);
                if (url != null)
                    return url;
            }
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        List<URL> resources = new ArrayList<URL>();
        if (PluginManager.FAST_LOOKUP) {
            for (PluginWrapper p : pluginManager.activePlugins) {
                if (accept(p)) {
                    resources.addAll(Collections.list(ClassLoaderReflectionToolkit._findResources(p.classLoader, name)));
                }
            }
        } else {
            for (PluginWrapper p : pluginManager.activePlugins) {
                resources.addAll(Collections.list(p.classLoader.getResources(name)));
            }
        }
        return Collections.enumeration(resources);
    }

    protected boolean accept(PluginWrapper p) {
        if (this.acceptedPlugins == null) {
            return true;
        }
        return this.acceptedPlugins.contains(p.getShortName());
    }

    @Override
    public String toString() {
        // only for debugging purpose
        return "classLoader " + getClass().getName();
    }
}
