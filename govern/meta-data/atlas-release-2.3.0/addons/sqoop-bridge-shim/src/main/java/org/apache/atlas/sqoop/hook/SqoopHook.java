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

package org.apache.atlas.sqoop.hook;


import org.apache.atlas.plugin.classloader.AtlasPluginClassLoader;
import org.apache.sqoop.SqoopJobDataPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sqoop hook used for atlas entity registration.
 */
public class SqoopHook extends SqoopJobDataPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(SqoopHook.class);

    private static final String ATLAS_PLUGIN_TYPE = "sqoop";
    private static final String ATLAS_SQOOP_HOOK_IMPL_CLASSNAME = "org.apache.atlas.sqoop.hook.SqoopHook";

    private AtlasPluginClassLoader atlasPluginClassLoader = null;
    private SqoopJobDataPublisher sqoopHookImpl = null;

    public SqoopHook() {
        this.initialize();
    }

    @Override
    public void publish(SqoopJobDataPublisher.Data data) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> SqoopHook.run({})", data);
        }

        try {
            activatePluginClassLoader();
            sqoopHookImpl.publish(data);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== SqoopHook.run({})", data);
        }
    }

    private void initialize() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HiveHook.initialize()");
        }

        try {
            atlasPluginClassLoader = AtlasPluginClassLoader.getInstance(ATLAS_PLUGIN_TYPE, this.getClass());

            @SuppressWarnings("unchecked")
            Class<SqoopJobDataPublisher> cls = (Class<SqoopJobDataPublisher>) Class
                    .forName(ATLAS_SQOOP_HOOK_IMPL_CLASSNAME, true, atlasPluginClassLoader);

            activatePluginClassLoader();

            sqoopHookImpl = cls.newInstance();
        } catch (Exception excp) {
            LOG.error("Error instantiating Atlas hook implementation", excp);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HiveHook.initialize()");
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
