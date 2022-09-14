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

import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.workflow.plugin.Plugin;
import org.apache.inlong.manager.workflow.plugin.ProcessPlugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test class for reload plugin.
 */
public class PluginServiceTest extends ServiceBaseTest {

    public static final String PLUGIN_NAME = "plugins";

    @Autowired
    PluginService pluginService;

    @Test
    public void testReloadPlugin() {
        String path = null;
        try {
            path = Paths.get(this.getClass().getClassLoader().getResource("").toURI()).toString();
        } catch (URISyntaxException e) {
            Assertions.fail(e.getMessage());
        }
        pluginService.setPluginLoc(path + File.separator + PLUGIN_NAME);
        pluginService.pluginReload();
        List<Plugin> pluginList = pluginService.getPlugins();
        Assertions.assertTrue(pluginList.size() > 0);
        Assertions.assertTrue(pluginList.get(0) instanceof ProcessPlugin);
    }

}
