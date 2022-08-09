/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.impala.hook;

import org.apache.atlas.plugin.classloader.AtlasPluginClassLoader;
import org.apache.impala.hooks.QueryCompleteContext;
import org.apache.impala.hooks.QueryEventHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to convert lineage records from Impala to lineage notifications and
 * send them to Atlas.
 */
public class ImpalaLineageHook implements QueryEventHook {
    private static final Logger LOG = LoggerFactory.getLogger(ImpalaLineageHook.class);

    private static final String ATLAS_PLUGIN_TYPE_IMPALA = "impala";
    private static final String ATLAS_IMPALA_LINEAGE_HOOK_IMPL_CLASSNAME =
        "org.apache.atlas.impala.hook.ImpalaHook";

    private AtlasPluginClassLoader atlasPluginClassLoader = null;
    private QueryEventHook impalaLineageHookImpl;

    public ImpalaLineageHook() {
    }

    /**
     * Execute Impala hook
     */
    public void onQueryComplete(QueryCompleteContext context) {
        LOG.debug("==> ImpalaLineageHook.onQueryComplete()");

        try {
            activatePluginClassLoader();
            impalaLineageHookImpl.onQueryComplete(context);
        } catch (Exception ex) {
            String errorMessage = String.format("Error in processing impala lineage: {}", context.getLineageGraph());
            LOG.error(errorMessage, ex);
        } finally {
            deactivatePluginClassLoader();
        }

        LOG.debug("<== ImpalaLineageHook.onQueryComplete()");
    }

    /**
     * Initialization of Impala hook
     */
    public void onImpalaStartup() {
        LOG.debug("==> ImpalaLineageHook.onImpalaStartup()");

        try {
            atlasPluginClassLoader = AtlasPluginClassLoader.getInstance(ATLAS_PLUGIN_TYPE_IMPALA, this.getClass());

            @SuppressWarnings("unchecked")
            Class<QueryEventHook> cls = (Class<QueryEventHook>) Class
                .forName(ATLAS_IMPALA_LINEAGE_HOOK_IMPL_CLASSNAME, true, atlasPluginClassLoader);

            activatePluginClassLoader();

            impalaLineageHookImpl = cls.newInstance();
            impalaLineageHookImpl.onImpalaStartup();
        } catch (Exception excp) {
            LOG.error("Error instantiating Atlas hook implementation for Impala lineage", excp);
        } finally {
            deactivatePluginClassLoader();
        }

        LOG.debug("<== ImpalaLineageHook.onImpalaStartup()");
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