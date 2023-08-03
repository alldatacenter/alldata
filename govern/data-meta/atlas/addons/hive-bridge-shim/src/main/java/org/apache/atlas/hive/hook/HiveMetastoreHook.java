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
package org.apache.atlas.hive.hook;

import org.apache.atlas.plugin.classloader.AtlasPluginClassLoader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.MetaStoreEventListener;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hive Metastore hook to capture DDL operations for atlas entity registration.
 */
public class HiveMetastoreHook extends MetaStoreEventListener {
    private static final String ATLAS_PLUGIN_TYPE                        = "hive";
    private static final String ATLAS_HIVE_METASTORE_HOOK_IMPL_CLASSNAME = "org.apache.atlas.hive.hook.HiveMetastoreHookImpl";
    public  static final Logger LOG                                      = LoggerFactory.getLogger(HiveMetastoreHook.class);

    private AtlasPluginClassLoader atlasPluginClassLoader = null;
    private MetaStoreEventListener atlasMetastoreHookImpl = null;
    private Configuration          config;

    public HiveMetastoreHook(Configuration config) {
        super(config);

        this.config   = config;

        this.initialize();
    }

    private void initialize() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HiveMetastoreHook.initialize()");
        }

        try {
            atlasPluginClassLoader = AtlasPluginClassLoader.getInstance(ATLAS_PLUGIN_TYPE, this.getClass());

            @SuppressWarnings("unchecked")
            Class<MetaStoreEventListener> cls = (Class<MetaStoreEventListener>)
                    Class.forName(ATLAS_HIVE_METASTORE_HOOK_IMPL_CLASSNAME, true, atlasPluginClassLoader);

            activatePluginClassLoader();

            atlasMetastoreHookImpl = cls.getDeclaredConstructor(Configuration.class).newInstance(config);
        } catch (Exception ex) {
            LOG.error("Error instantiating Atlas hook implementation", ex);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HiveMetastoreHook.initialize()");
        }
    }

    @Override
    public void onCreateTable(CreateTableEvent tableEvent) throws MetaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HiveMetastoreHook.onCreateTable()");
        }

        try {
            activatePluginClassLoader();

            atlasMetastoreHookImpl.onCreateTable(tableEvent);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HiveMetastoreHook.onCreateTable()");
        }
    }

    @Override
    public void onDropTable(DropTableEvent tableEvent) throws MetaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HiveMetastoreHook.onDropTable()");
        }

        try {
            activatePluginClassLoader();

            atlasMetastoreHookImpl.onDropTable(tableEvent);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HiveMetastoreHook.onDropTable()");
        }
    }

    @Override
    public void onAlterTable(AlterTableEvent tableEvent) throws MetaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HiveMetastoreHook.onAlterTable()");
        }

        try {
            activatePluginClassLoader();

            atlasMetastoreHookImpl.onAlterTable(tableEvent);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HiveMetastoreHook.onAlterTable()");
        }
    }

    @Override
    public void onCreateDatabase(CreateDatabaseEvent dbEvent) throws MetaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HiveMetastoreHook.onCreateDatabase()");
        }

        try {
            activatePluginClassLoader();

            atlasMetastoreHookImpl.onCreateDatabase(dbEvent);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HiveMetastoreHook.onCreateDatabase()");
        }
    }

    @Override
    public void onDropDatabase(DropDatabaseEvent dbEvent) throws MetaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HiveMetastoreHook.onDropDatabase()");
        }

        try {
            activatePluginClassLoader();

            atlasMetastoreHookImpl.onDropDatabase(dbEvent);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HiveMetastoreHook.onDropDatabase()");
        }
    }

    @Override
    public void onAlterDatabase(AlterDatabaseEvent dbEvent) throws MetaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HiveMetastoreHook.onAlterDatabase()");
        }

        try {
            activatePluginClassLoader();

            atlasMetastoreHookImpl.onAlterDatabase(dbEvent);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HiveMetastoreHook.onAlterDatabase()");
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