/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.core.plugin;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.workflow.plugin.PluginDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Plugin class loader.
 */
@Slf4j
public class PluginClassLoader extends URLClassLoader {

    public static final String PLUGIN_PATH = "META-INF/plugin.yaml";

    public static final String WINDOWS_PREFIX = "win";

    /**
     * plugin.yaml should less than 1k
     */
    public static final int PLUGIN_DEF_CAPACITY = 1024;
    private final File pluginDirectory;
    /**
     * pluginName -> pluginDefinition
     */
    private Map<String, PluginDefinition> pluginDefinitionMap = new HashMap<>();
    private ObjectMapper yamlMapper;
    private String osName;

    private PluginClassLoader(URL url, ClassLoader parent, String osName) throws IOException {
        super(new URL[]{url}, parent);
        this.pluginDirectory = new File(url.getPath());
        this.osName = osName;
        initYamlMapper();
        loadPluginDefinition();
    }

    /**
     * Get pluginClassLoader by plugin url.
     */
    public static PluginClassLoader getFromPluginUrl(String url, ClassLoader parent) {
        log.info("ClassLoaderPath:{}", url);
        checkClassLoader(parent);
        checkUrl(url);
        return AccessController.doPrivileged(new PrivilegedAction<PluginClassLoader>() {
            @SneakyThrows
            @Override
            public PluginClassLoader run() {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.startsWith(WINDOWS_PREFIX)) {
                    return new PluginClassLoader(new URL("file:///" + url), parent, os);
                } else {
                    return new PluginClassLoader(new URL("file://" + url), parent, os);
                }
            }
        });
    }

    private static void checkClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new RuntimeException("parent classLoader should not be null");
        }
    }

    private static void checkUrl(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url should not be empty");
        }
        File pluginDirectory = new File(url);
        if (!pluginDirectory.exists()) {
            throw new RuntimeException(String.format("pluginDirectory '%s' is not exists", pluginDirectory));
        }
        if (!pluginDirectory.isDirectory()) {
            throw new RuntimeException(String.format("pluginDirectory '%s' should be directory", pluginDirectory));
        }
        if (!pluginDirectory.canRead()) {
            throw new RuntimeException(String.format("pluginDirectory '%s' is not readable", pluginDirectory));
        }
    }

    public Map<String, PluginDefinition> getPluginDefinitions() {
        return this.pluginDefinitionMap;
    }

    private void initYamlMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        mapper.setSerializationInclusion(Include.NON_NULL);
        this.yamlMapper = mapper;
    }

    /**
     * load pluginDefinition in **.jar/META-INF/plugin.yaml
     */
    private void loadPluginDefinition() throws IOException {
        File[] files = pluginDirectory.listFiles();
        if (files == null) {
            log.warn("plugin directory {} has no files", pluginDirectory);
            return;
        }

        List<PluginDefinition> definitions = new ArrayList<>();
        for (File jarFile : files) {
            if (!jarFile.getName().endsWith(".jar")) {
                log.warn("{} is not valid plugin jar, skip to load", jarFile);
                continue;
            }
            log.info("{} is valid plugin jar, start to load", jarFile);
            JarFile pluginJar = new JarFile(jarFile);
            String pluginDef = readPluginDef(pluginJar);
            pluginDef = pluginDef.replaceAll("[\\x00]+", "");
            PluginDefinition definition = yamlMapper.readValue(pluginDef, PluginDefinition.class);
            if (osName.startsWith(WINDOWS_PREFIX)) {
                addURL(new URL("file:///" + jarFile.getAbsolutePath()));
            } else {
                addURL(new URL("file://" + jarFile.getAbsolutePath()));
            }
            checkPluginValid(jarFile, definition);
            definitions.add(definition);
        }
        pluginDefinitionMap = definitions.stream()
                .collect(Collectors.toMap(PluginDefinition::getName, definition -> definition));
    }

    private void checkPluginValid(File jarFile, PluginDefinition pluginDefinition) {
        if (StringUtils.isEmpty(pluginDefinition.getName())) {
            throw new RuntimeException(String.format("%s should define pluginName in plugin.yaml", jarFile.getName()));
        }
        if (StringUtils.isEmpty(pluginDefinition.getPluginClass())) {
            throw new RuntimeException(String.format("%s should define pluginClass in plugin.yaml", jarFile.getName()));
        }
        try {
            this.loadClass(pluginDefinition.getPluginClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    String.format("pluginClass '%s' could not be found in %s", pluginDefinition.getPluginClass(),
                            jarFile.getName()));
        }
        if (StringUtils.isEmpty(pluginDefinition.getDescription())) {
            log.warn(String.format("%s should define description in plugin.yaml", jarFile.getName()));
        }
        if (StringUtils.isEmpty(pluginDefinition.getJavaVersion())) {
            throw new RuntimeException(String.format("%s should define javaVersion in plugin.yaml", jarFile.getName()));
        }
        JarHell.checkJavaVersion(pluginDefinition.getName(), pluginDefinition.getJavaVersion());
    }

    private String readPluginDef(JarFile jar) throws IOException {
        JarEntry entry = jar.getJarEntry(PLUGIN_PATH);
        if (entry == null) {
            throw new RuntimeException(String.format("%s is not found in jar '%s'", PLUGIN_PATH, jar.getName()));
        }
        ByteBuffer buffer = ByteBuffer.allocate(PLUGIN_DEF_CAPACITY);
        int bt;
        try (InputStream is = jar.getInputStream(entry)) {
            while ((bt = is.read()) != -1) {
                buffer.put((byte) bt);
            }
        }
        return new String(buffer.array(), StandardCharsets.UTF_8);
    }

}
