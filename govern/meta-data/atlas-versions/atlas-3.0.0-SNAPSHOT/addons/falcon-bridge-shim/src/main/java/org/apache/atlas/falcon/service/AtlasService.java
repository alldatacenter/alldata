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

package org.apache.atlas.falcon.service;


import org.apache.atlas.plugin.classloader.AtlasPluginClassLoader;
import org.apache.falcon.FalconException;
import org.apache.falcon.entity.store.ConfigurationStore;
import org.apache.falcon.entity.v0.Entity;
import org.apache.falcon.service.ConfigurationChangeListener;
import org.apache.falcon.service.FalconService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Falcon hook used for atlas entity registration.
 */
public class AtlasService implements FalconService, ConfigurationChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasService.class);

    private static final String ATLAS_PLUGIN_TYPE = "falcon";
    private static final String ATLAS_FALCON_HOOK_IMPL_CLASSNAME = "org.apache.atlas.falcon.service.AtlasService";

    private AtlasPluginClassLoader atlasPluginClassLoader = null;
    private FalconService falconServiceImpl = null;
    private ConfigurationChangeListener configChangeListenerImpl = null;

    public AtlasService() {
        this.initialize();
    }

    @Override
    public String getName() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasService.getName()");
        }

        String ret = null;

        try {
            activatePluginClassLoader();
            ret = falconServiceImpl.getName();
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasService.getName()");
        }

        return ret;
    }

    @Override
    public void init() throws FalconException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasService.init()");
        }

        try {
            activatePluginClassLoader();

            ConfigurationStore.get().registerListener(this);

            falconServiceImpl.init();
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasService.init()");
        }
    }

    @Override
    public void destroy() throws FalconException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasService.destroy()");
        }

        try {
            activatePluginClassLoader();

            ConfigurationStore.get().unregisterListener(this);

            falconServiceImpl.destroy();
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasService.destroy()");
        }
    }

    @Override
    public void onAdd(Entity entity) throws FalconException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasService.onAdd({})", entity);
        }

        try {
            activatePluginClassLoader();
            configChangeListenerImpl.onAdd(entity);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasService.onAdd({})", entity);
        }
    }

    @Override
    public void onRemove(Entity entity) throws FalconException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasService.onRemove({})", entity);
        }

        try {
            activatePluginClassLoader();
            configChangeListenerImpl.onRemove(entity);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasService.onRemove({})", entity);
        }
    }

    @Override
    public void onChange(Entity entity, Entity entity1) throws FalconException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasService.onChange({}, {})", entity, entity1);
        }

        try {
            activatePluginClassLoader();
            configChangeListenerImpl.onChange(entity, entity1);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasService.onChange({}, {})", entity, entity1);
        }
    }

    @Override
    public void onReload(Entity entity) throws FalconException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasService.onReload({})", entity);
        }

        try {
            activatePluginClassLoader();
            configChangeListenerImpl.onReload(entity);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasService.onReload({})", entity);
        }
    }

    private void initialize() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasService.initialize()");
        }

        try {
            atlasPluginClassLoader = AtlasPluginClassLoader.getInstance(ATLAS_PLUGIN_TYPE, this.getClass());

            Class<?> cls = Class.forName(ATLAS_FALCON_HOOK_IMPL_CLASSNAME, true, atlasPluginClassLoader);

            activatePluginClassLoader();

            Object atlasService = cls.newInstance();

            falconServiceImpl = (FalconService) atlasService;
            configChangeListenerImpl = (ConfigurationChangeListener) atlasService;
        } catch (Exception excp) {
            LOG.error("Error instantiating Atlas hook implementation", excp);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasService.initialize()");
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
