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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.inlong.manager.workflow.plugin.Plugin;
import org.apache.inlong.manager.workflow.plugin.PluginBinder;
import org.apache.inlong.manager.workflow.plugin.PluginDefinition;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PluginService is designed to load all plugin from ${BASE_PATH}/plugins
 * this service must be initialized after all other beans.
 */
@Service
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class PluginService implements InitializingBean {


    public static final String DEFAULT_PLUGIN_LOC = "plugins";

    @Getter
    private final List<Plugin> plugins = new ArrayList<>();

    @Setter
    @Getter
    @Value("${plugin.location?:}")
    private String pluginLoc;

    @Getter
    @Autowired
    private List<PluginBinder> pluginBinders;

    public PluginService() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isEmpty(pluginLoc)) {
            pluginLoc = DEFAULT_PLUGIN_LOC;
        }
        log.info("pluginLoc:{}", pluginLoc);
        pluginReload();
    }

    /**
     * Reload the plugin from the plugin path
     */
    public void pluginReload() {
        Path path = Paths.get(pluginLoc).toAbsolutePath();
        log.info("search for plugin in {}", path);
        if (!path.toFile().exists()) {
            log.warn("plugin directory not found");
            return;
        }
        PluginClassLoader pluginLoader = PluginClassLoader.getFromPluginUrl(path.toString(),
                Thread.currentThread().getContextClassLoader());
        Map<String, PluginDefinition> pluginDefinitions = pluginLoader.getPluginDefinitions();
        if (MapUtils.isEmpty(pluginDefinitions)) {
            log.warn("pluginDefinition not found in {}", pluginLoc);
            return;
        }
        List<Plugin> plugins = new ArrayList<>();
        for (PluginDefinition pluginDefinition : pluginDefinitions.values()) {
            String pluginClassName = pluginDefinition.getPluginClass();
            try {
                Class<?> pluginClass = pluginLoader.loadClass(pluginClassName);
                Object plugin = pluginClass.getDeclaredConstructor().newInstance();
                plugins.add((Plugin) plugin);
            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        this.plugins.addAll(plugins);
        for (PluginBinder pluginBinder : pluginBinders) {
            for (Plugin plugin : plugins) {
                log.info(String.format("PluginBinder:%s load Plugin:%s",
                        pluginBinder.getClass().getSimpleName(), plugin.getClass().getSimpleName()));
                pluginBinder.acceptPlugin(plugin);
            }
        }
    }

}
