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

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.plugin.Plugin;
import org.apache.inlong.manager.common.plugin.PluginDefinition;
import org.apache.inlong.manager.workflow.plugin.ProcessPlugin;
import org.apache.inlong.manager.workflow.plugin.sort.PollerPlugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Test class for load plugin.
 */
public class PluginClassLoaderTest {

    /**
     * The test plugin jar was packaged from manager-plugins,
     * naming it to `manager-plugin-example.jar`.
     */
    @Test
    public void testLoadPlugin() {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath();
        PluginClassLoader pluginClassLoader = PluginClassLoader.getFromPluginUrl(path + "plugins",
                Thread.currentThread().getContextClassLoader());
        Map<String, PluginDefinition> pluginDefinitionMap = pluginClassLoader.getPluginDefinitions();
        Assertions.assertEquals(1, pluginDefinitionMap.size());

        PluginDefinition pluginDefinition = Lists.newArrayList(pluginDefinitionMap.values()).get(0);
        Assertions.assertNotNull(pluginDefinition);
        List<String> classNames = pluginDefinition.getPluginClasses();
        Assertions.assertTrue(CollectionUtils.isNotEmpty(classNames));

        for (String name : classNames) {
            try {
                Class<?> cls = pluginClassLoader.loadClass(name);
                Plugin plugin = (Plugin) cls.getDeclaredConstructor().newInstance();
                Assertions.assertTrue(plugin instanceof ProcessPlugin
                        || plugin instanceof PollerPlugin);
            } catch (Exception e) {
                Assertions.assertTrue(e instanceof ClassNotFoundException);
                Assertions.fail();
            }
        }
    }

}
