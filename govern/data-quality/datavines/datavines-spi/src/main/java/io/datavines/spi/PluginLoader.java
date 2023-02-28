/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.spi;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datavines.spi.utils.ClassUtils;
import io.datavines.spi.utils.Holder;
import io.datavines.spi.utils.UnsafeStringWriter;

/**
 * PluginLoader
 */
public class PluginLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    private static final String PLUGIN_DIR = "META-INF/plugins/";

    private static final ConcurrentMap<Class<?>, PluginLoader<?>> PLUGIN_LOADERS = new ConcurrentHashMap<>(64);

    private static final ConcurrentMap<Class<?>, Object> PLUGIN_INSTANCES = new ConcurrentHashMap<>(64);

    private final Class<?> type;

    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();

    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    
    private final Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();

    private final Object pluginLock = new Object();

    private PluginLoader(Class<?> type) {
        this.type = type;
    }

    private static <T> boolean withPluginAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> PluginLoader<T> getPluginLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Plugin type == null");
        }

        if (!type.isInterface()) {
            throw new IllegalArgumentException("Plugin type (" + type + ") is not an interface!");
        }

        if (!withPluginAnnotation(type)) {
            throw new IllegalArgumentException("Plugin type (" + type +
                    ") is not an plugin, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        }

        PluginLoader<T> loader = (PluginLoader<T>) PLUGIN_LOADERS.get(type);
        if (loader == null) {
            PLUGIN_LOADERS.putIfAbsent(type, new PluginLoader<T>(type));
            loader = (PluginLoader<T>) PLUGIN_LOADERS.get(type);
        }

        return loader;
    }

    public static <T> void resetPluginLoader(Class<T> type) {
        PluginLoader<?> loader = PLUGIN_LOADERS.get(type);
        if (loader != null) {
            // Remove all instances associated with this loader as well
            Map<String, Class<?>> classes = loader.getPluginClasses();
            for (Map.Entry<String, Class<?>> entry : classes.entrySet()) {
                PLUGIN_INSTANCES.remove(entry.getValue());
            }
            classes.clear();
            PLUGIN_LOADERS.remove(type);
        }
    }

    private static ClassLoader findClassLoader() {
        return ClassUtils.getClassLoader(PluginLoader.class);
    }

    public String getPluginName(T pluginInstance) {
        return getPluginName(pluginInstance.getClass());
    }

    public String getPluginName(Class<?> pluginClass) {
        // load class
        getPluginClasses();
        return cachedNames.get(pluginClass);
    }

    @SuppressWarnings("unchecked")
    public T getLoadedPlugin(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Plugin name == null");
        }
        Holder<Object> holder = getOrCreateHolder(name);
        return (T) holder.get();
    }

    private Holder<Object> getOrCreateHolder(String name) {
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        return holder;
    }

    /**
     * Return the list of plugins which are already loaded.
     */
    public Set<String> getLoadedPlugins() {
        return Collections.unmodifiableSet(new TreeSet<>(cachedInstances.keySet()));
    }

    @SuppressWarnings("unchecked")
    public List<T> getLoadedPluginInstances() {
        List<T> instances = new ArrayList<>();
        cachedInstances.values().forEach(holder -> instances.add((T) holder.get()));
        return instances;
    }

    /**
     * Find the plugin with the given name. If the specified name is not found, then {@link IllegalStateException}
     * will be thrown.
     */
    @SuppressWarnings("unchecked")
    public T getOrCreatePlugin(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Plugin name == null");
        }
        
        final Holder<Object> holder = getOrCreateHolder(name);
        Object instance = holder.get();
        if (instance == null) {
            synchronized (pluginLock) {
                instance = holder.get();
                if (instance == null) {
                    instance = createPlugin(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    @SuppressWarnings("unchecked")
    public T getNewPlugin(String name){
        Class<?> clazz = getPluginClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        try {
            T instance = (T) clazz.newInstance();
            PLUGIN_INSTANCES.putIfAbsent(clazz,instance);
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Plugin instance (name: " + name + ", class: " +
                    type + ") couldn't be instantiated: " + t.getMessage(), t);
        }
    }
    
    public boolean hasPlugin(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Plugin name == null");
        }
        Class<?> c = this.getPluginClass(name);
        return c != null;
    }

    public Set<String> getSupportedPlugins() {
        Map<String, Class<?>> classes = getPluginClasses();
        return Collections.unmodifiableSet(new TreeSet<>(classes.keySet()));
    }

    public Set<T> getSupportedPluginInstances() {
        List<T> instances = new LinkedList<>();
        Set<String> supportedPlugins = getSupportedPlugins();
        if (CollectionUtils.isNotEmpty(supportedPlugins)) {
            for (String name : supportedPlugins) {
                instances.add(getOrCreatePlugin(name));
            }
        }
        // sort the Prioritized instances
        instances.sort(Prioritized.COMPARATOR);
        return new LinkedHashSet<>(instances);
    }

    /**
     * Add Or Replace the existing plugin via API
     *
     * @param name  plugin name
     * @param clazz plugin class
     * @throws IllegalStateException when plugin to be placed doesn't exist
     * @deprecated not recommended any longer, and use only when test
     */
    public void addOrReplacePlugin(String name, Class<?> clazz) {
        getPluginClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + " doesn't implement Plugin " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + " can't be interface!");
        }

        if (StringUtils.isBlank(name)) {
            throw new IllegalStateException("Plugin name is blank (Plugin " + type + ")!");
        }
        if (!cachedClasses.get().containsKey(name)) {
            throw new IllegalStateException("Plugin name " +
                    name + " doesn't exist (Plugin " + type + ")!");
        }

        cachedNames.put(clazz, name);
        cachedClasses.get().put(name, clazz);
        cachedInstances.remove(name);
    }
    
    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such plugin " + type.getName() + " by name " + name);
        
        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }

            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(toString(entry.getValue()));
        }
        return new IllegalStateException(buf.toString());
    }

    @SuppressWarnings("unchecked")
    private T createPlugin(String name) {
        Class<?> clazz = getPluginClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        try {
            T instance = (T) PLUGIN_INSTANCES.get(clazz);
            if (instance == null) {
                PLUGIN_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) PLUGIN_INSTANCES.get(clazz);
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Plugin instance (name: " + name + ", class: " +
                    type + ") couldn't be instantiated: " + t.getMessage(), t);
        }
    }

    private boolean containsPlugin(String name) {
        return getPluginClasses().containsKey(name);
    }

    private Class<?> getPluginClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("Plugin type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Plugin name == null");
        }
        return getPluginClasses().get(name);
    }

    private Map<String, Class<?>> getPluginClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = loadPluginClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * synchronized in getPluginClasses
     */
    private Map<String, Class<?>> loadPluginClasses() {
        
        Map<String, Class<?>> pluginClasses = new HashMap<>();
        loadDirectory(pluginClasses,type.getName());
        
        return pluginClasses;
    }

    private void loadDirectory(Map<String, Class<?>> pluginClasses, String type) {
        String fileName = PLUGIN_DIR + type;
        try {
            Enumeration<java.net.URL> urls = null;
            ClassLoader classLoader = findClassLoader();

            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }

            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceUrl = urls.nextElement();
                    loadResource(pluginClasses, classLoader, resourceUrl);
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading plugin class (interface: " +
                    type + ", description file: " + fileName + ").", t);
        }
    }

    private void loadResource(Map<String, Class<?>> pluginClasses, ClassLoader classLoader, java.net.URL resourceUrl) {
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final int ci = line.indexOf('#');
                    if (ci >= 0) {
                        line = line.substring(0, ci);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        try {
                            String name = null;
                            int i = line.indexOf('=');
                            if (i > 0) {
                                name = line.substring(0, i).trim();
                                line = line.substring(i + 1).trim();
                            }
                            if (line.length() > 0 ) {
                                loadClass(pluginClasses, resourceUrl, Class.forName(line, true, classLoader), name);
                            }
                        } catch (Throwable t) {
                            IllegalStateException e = new IllegalStateException("Failed to load plugin class (interface: " + type + ", class line: " + line + ") in " + resourceUrl + ", cause: " + t.getMessage(), t);
                            exceptions.put(line, e);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading plugin class (interface: " +
                    type + ", class file: " + resourceUrl + ") in " + resourceUrl, t);
        }
    }

    private void loadClass(Map<String, Class<?>> pluginClasses,
                           java.net.URL resourceUrl, Class<?> clazz, String name) {
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading plugin class (interface: " +
                    type + ", class line: " + clazz.getName() + "), class "
                    + clazz.getName() + " is not subtype of interface.");
        }

        if (StringUtils.isEmpty(name)) {
            name = findAnnotationName(clazz);
            if (name.length() == 0) {
                throw new IllegalStateException("No such plugin name for the class " + clazz.getName() + " in the config " + resourceUrl);
            }
        }

        String[] names = NAME_SEPARATOR.split(name);
        if (ArrayUtils.isNotEmpty(names)) {
            for (String n : names) {
                cacheName(clazz, n);
                saveInPluginClass(pluginClasses, clazz, n);
            }
        }
    }

    /**
     * cache name
     */
    private void cacheName(Class<?> clazz, String name) {
        if (!cachedNames.containsKey(clazz)) {
            cachedNames.put(clazz, name);
        }
    }

    /**
     * put clazz in pluginClasses
     */
    private void saveInPluginClass(Map<String, Class<?>> pluginClasses, Class<?> clazz, String name) {
        Class<?> c = pluginClasses.get(name);
        if (c == null) {
            pluginClasses.put(name, clazz);
        } else if (c != clazz) {
            String duplicateMsg = "Duplicate plugin " + type.getName() + " name " + name + " on " + c.getName() + " and " + clazz.getName();
            logger.error(duplicateMsg);
            throw new IllegalStateException(duplicateMsg);
        }
    }

    private String findAnnotationName(Class<?> clazz) {

        String name = clazz.getSimpleName();
        if (name.endsWith(type.getSimpleName())) {
            name = name.substring(0, name.length() - type.getSimpleName().length());
        }
        return name.toLowerCase();
    }
    
    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

    /**
     * @param e
     * @return string
     */
    public static String toString(Throwable e) {
        UnsafeStringWriter w = new UnsafeStringWriter();
        PrintWriter p = new PrintWriter(w);
        p.print(e.getClass().getName());
        if (e.getMessage() != null) {
            p.print(": " + e.getMessage());
        }
        p.println();
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }

}
