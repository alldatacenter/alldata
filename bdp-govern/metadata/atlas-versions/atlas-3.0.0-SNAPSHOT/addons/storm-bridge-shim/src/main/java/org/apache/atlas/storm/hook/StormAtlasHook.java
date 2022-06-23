/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.storm.hook;


import org.apache.atlas.plugin.classloader.AtlasPluginClassLoader;
import org.apache.storm.ISubmitterHook;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.generated.TopologyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Storm hook used for atlas entity registration.
 */
public class StormAtlasHook implements ISubmitterHook {
    private static final Logger LOG = LoggerFactory.getLogger(StormAtlasHook.class);


    private static final String ATLAS_PLUGIN_TYPE = "storm";
    private static final String ATLAS_STORM_HOOK_IMPL_CLASSNAME = "org.apache.atlas.storm.hook.StormAtlasHook";

    private AtlasPluginClassLoader atlasPluginClassLoader = null;
    private ISubmitterHook stormHook = null;


    public StormAtlasHook() {
        this.initialize();
    }

    @Override
    public void notify(TopologyInfo topologyInfo, Map stormConf, StormTopology stormTopology)
        throws IllegalAccessException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> StormAtlasHook.notify({}, {}, {})", topologyInfo, stormConf, stormTopology);
        }

        try {
            activatePluginClassLoader();
            stormHook.notify(topologyInfo, stormConf, stormTopology);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== StormAtlasHook.notify({}, {}, {})", topologyInfo, stormConf, stormTopology);
        }
    }

    private void initialize() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> StormAtlasHook.initialize()");
        }

        try {
            atlasPluginClassLoader = AtlasPluginClassLoader.getInstance(ATLAS_PLUGIN_TYPE, this.getClass());

            @SuppressWarnings("unchecked")
            Class<ISubmitterHook> cls = (Class<ISubmitterHook>) Class
                    .forName(ATLAS_STORM_HOOK_IMPL_CLASSNAME, true, atlasPluginClassLoader);

            activatePluginClassLoader();

            stormHook = cls.newInstance();
        } catch (Exception excp) {
            LOG.error("Error instantiating Atlas hook implementation", excp);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== StormAtlasHook.initialize()");
        }
    }

    private void activatePluginClassLoader() {
        if (atlasPluginClassLoader != null) {
            atlasPluginClassLoader.activate();
        }
    }

    private void deactivatePluginClassLoader() {
        if (atlasPluginClassLoader != null) {
            atlasPluginClassLoader.deactivate();
        }
    }
}
