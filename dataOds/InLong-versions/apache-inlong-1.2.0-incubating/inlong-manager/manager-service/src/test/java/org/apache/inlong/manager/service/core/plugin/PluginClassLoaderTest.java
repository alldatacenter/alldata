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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.workflow.plugin.Plugin;
import org.apache.inlong.manager.workflow.plugin.PluginDefinition;
import org.apache.inlong.manager.workflow.plugin.ProcessPlugin;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Test class for load plugin.
 */
public class PluginClassLoaderTest {

    @Test
    public void testLoadPlugin() {

        String path = this.getClass().getClassLoader().getResource("").getPath();
        PluginClassLoader pluginClassLoader = PluginClassLoader.getFromPluginUrl(path + "plugins",
                Thread.currentThread().getContextClassLoader());
        Map<String, PluginDefinition> pluginDefinitionMap = pluginClassLoader.getPluginDefinitions();
        Assert.assertEquals(1, pluginDefinitionMap.size());
        PluginDefinition pluginDefinition = Lists.newArrayList(pluginDefinitionMap.values()).get(0);
        Assert.assertNotNull(pluginDefinition);
        String pluginClass = pluginDefinition.getPluginClass();
        Assert.assertTrue(StringUtils.isNotEmpty(pluginClass));
        try {
            Class cls = pluginClassLoader.loadClass(pluginClass);
            Plugin plugin = (Plugin) cls.getDeclaredConstructor().newInstance();
            Assert.assertTrue(plugin instanceof ProcessPlugin);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException e) {
            Assert.assertTrue(e instanceof ClassNotFoundException);
            Assert.fail();
        }
    }

}
