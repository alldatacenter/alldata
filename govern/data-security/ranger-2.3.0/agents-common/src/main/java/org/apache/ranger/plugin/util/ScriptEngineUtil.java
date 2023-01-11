/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.util;


import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.apache.ranger.plugin.conditionevaluator.RangerScriptConditionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.List;

public class ScriptEngineUtil {
    private static final Logger LOG = LoggerFactory.getLogger(RangerScriptConditionEvaluator.class);


    public static ScriptEngine createScriptEngine(String engineName, String serviceType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> ScriptEngineUtil.createScriptEngine(engineName=" + engineName + ", serviceType=" + serviceType + ")");
        }
        ScriptEngine ret = null;

        try {
            ScriptEngineManager manager = new ScriptEngineManager();

            if (LOG.isDebugEnabled()) {
                List<ScriptEngineFactory> factories = manager.getEngineFactories();

                if (CollectionUtils.isEmpty(factories)) {
                    LOG.debug("List of scriptEngineFactories is empty!!");
                } else {
                    for (ScriptEngineFactory factory : factories) {
                        LOG.debug("engineName=" + factory.getEngineName() + ", language=" + factory.getLanguageName());
                    }
                }
            }

            ret = manager.getEngineByName(engineName);
        } catch (Throwable exp) {
            LOG.error("RangerScriptConditionEvaluator.init() failed", exp);
        }

        LOG.debug((ret == null ? " Failed to create " : " Created ") + "Script Engine '" + engineName + "' in a default manner.");

        if (ret == null) {
            LOG.warn("Will try to get script-engine from plugin-class-loader for service-type:[" + serviceType + "]");

            RangerPluginClassLoader pluginClassLoader;

            try {
                pluginClassLoader = RangerPluginClassLoader.getInstance(serviceType, null);

                if (pluginClassLoader != null) {
                    ret = pluginClassLoader.getScriptEngine(engineName);
                } else {
                    LOG.error("Cannot get script-engine from null pluginClassLoader");
                }
            } catch (Throwable exp) {
                LOG.error("RangerScriptConditionEvaluator.init() failed", exp);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== ScriptEngineUtil.createScriptEngine(engineName=" + engineName + ", serviceType=" + serviceType + ") : ret=" + ret);
        }
        return ret;
    }
}
