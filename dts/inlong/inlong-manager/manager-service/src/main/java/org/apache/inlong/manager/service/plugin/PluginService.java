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

package org.apache.inlong.manager.service.plugin;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.plugin.Plugin;
import org.apache.inlong.manager.common.plugin.PluginBinder;
import org.apache.inlong.manager.common.plugin.PluginDefinition;
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

    public static final String DEFAULT_PLUGIN_LOCATION = "plugins";

    @Getter
    private final List<Plugin> plugins = new ArrayList<>();

    @Setter
    @Getter
    @Value("${plugin.location:plugins}")
    private String pluginLocation;

    @Getter
    @Autowired
    private List<PluginBinder> pluginBinders;

    public PluginService() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isEmpty(pluginLocation)) {
            pluginLocation = DEFAULT_PLUGIN_LOCATION;
        }
        log.info("plugin location is {}", pluginLocation);
        pluginReload();
    }

    /**
     * Reload the plugin from the plugin path
     */
    public void pluginReload() {
        Path path = Paths.get(pluginLocation).toAbsolutePath();
        log.info("search for plugin in {}", path);
        if (!path.toFile().exists()) {
            log.warn("plugin directory not found");
            return;
        }

        PluginClassLoader pluginLoader = PluginClassLoader.getFromPluginUrl(path.toString(),
                Thread.currentThread().getContextClassLoader());
        Map<String, PluginDefinition> pluginDefinitions = pluginLoader.getPluginDefinitions();
        if (MapUtils.isEmpty(pluginDefinitions)) {
            log.warn("plugin definition not found in {}", pluginLocation);
            return;
        }

        List<Plugin> plugins = new ArrayList<>();
        for (PluginDefinition pluginDefinition : pluginDefinitions.values()) {
            List<String> classNames = pluginDefinition.getPluginClasses();
            for (String name : classNames) {
                try {
                    Class<?> pluginClass = pluginLoader.loadClass(name);
                    Object plugin = pluginClass.getDeclaredConstructor().newInstance();
                    plugins.add((Plugin) plugin);
                } catch (Throwable e) {
                    log.error("create plugin instance error: ", e);
                    throw new BusinessException("create plugin instance error: " + e.getMessage());
                }
            }
        }
        this.plugins.addAll(plugins);

        for (PluginBinder binder : pluginBinders) {
            for (Plugin plugin : plugins) {
                binder.acceptPlugin(plugin);
                log.info("plugin {} loaded by plugin binder {}", plugin.getClass(), binder.getClass());
            }
        }
    }

}
