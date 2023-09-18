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
package io.datavines.engine.core.config;

import io.datavines.common.config.Config;
import io.datavines.common.config.ConfigRuntimeException;
import io.datavines.common.config.DataVinesJobConfig;
import io.datavines.common.config.EnvConfig;
import io.datavines.engine.api.component.Component;
import io.datavines.engine.api.env.RuntimeEnvironment;
import io.datavines.engine.core.utils.JsonUtils;
import io.datavines.spi.PluginLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.datavines.engine.api.EngineConstants.PLUGIN_TYPE;
import static io.datavines.engine.api.EngineConstants.TYPE;

public class ConfigParser {

    private static final Logger logger = LoggerFactory.getLogger(ConfigParser.class);

    private final String configFile;

    private final DataVinesJobConfig config;

    private final EnvConfig envConfig;

    private final RuntimeEnvironment env;

    public ConfigParser(String configFile){
        this.configFile = configFile;
        this.config = load();
        this.envConfig = config.getEnvConfig();
        this.env = createRuntimeEnvironment();
    }

    private DataVinesJobConfig load() {

        if (configFile.isEmpty()) {
            throw new ConfigRuntimeException("Please specify config file");
        }

        logger.info("Loading config file: " + configFile);

        DataVinesJobConfig config = JsonUtils.parseObject(configFile, DataVinesJobConfig.class);

        logger.info("config after parse: " + JsonUtils.toJsonString(config));

        return config;
    }

    private RuntimeEnvironment createRuntimeEnvironment() {
        RuntimeEnvironment env = PluginLoader
                .getPluginLoader(RuntimeEnvironment.class)
                .getNewPlugin(envConfig.getEngine());
        Config config = new Config(envConfig.getConfig());
        config.put(TYPE, envConfig.getType());
        env.setConfig(config);
        env.prepare();
        return env;
    }

    public RuntimeEnvironment getRuntimeEnvironment() {
        return env;
    }

    public List<Component> getSourcePlugins() {
        List<Component> sourcePluginList = new ArrayList<>();
        config.getSourceParameters().forEach(sourceConfig -> {
            String pluginName = envConfig.getEngine() + "-" + sourceConfig.getPlugin()+"-source";
            Component component = PluginLoader
                    .getPluginLoader(Component.class)
                    .getNewPlugin(pluginName);
            sourceConfig.getConfig().put(PLUGIN_TYPE, sourceConfig.getType());
            component.setConfig(new Config(sourceConfig.getConfig()));
            sourcePluginList.add(component);
        });
        return sourcePluginList;
    }

    public List<Component> getSinkPlugins() {
        List<Component> sinkPluginList = new ArrayList<>();
        config.getSinkParameters().forEach(sinkConfig -> {
            String pluginName = envConfig.getEngine() + "-" + sinkConfig.getPlugin()+"-sink";
            Component component = PluginLoader
                    .getPluginLoader(Component.class)
                    .getNewPlugin(pluginName);
            sinkConfig.getConfig().put(PLUGIN_TYPE, sinkConfig.getType());
            component.setConfig(new Config(sinkConfig.getConfig()));
            sinkPluginList.add(component);
        });
        return sinkPluginList;
    }

    public List<Component> getTransformPlugins() {
        List<Component> transformPluginList = new ArrayList<>();
        config.getTransformParameters().forEach(transformConfig -> {
            String pluginName = envConfig.getEngine() + "-" + transformConfig.getPlugin() + "-transform";
            Component component = PluginLoader
                    .getPluginLoader(Component.class)
                    .getNewPlugin(pluginName);
            transformConfig.getConfig().put(PLUGIN_TYPE, transformConfig.getType());
            component.setConfig(new Config(transformConfig.getConfig()));
            transformPluginList.add(component);
        });
        return transformPluginList;
    }
}
