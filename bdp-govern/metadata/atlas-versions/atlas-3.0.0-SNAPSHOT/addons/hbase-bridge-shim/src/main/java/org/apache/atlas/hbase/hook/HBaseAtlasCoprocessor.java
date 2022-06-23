/**
 *
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
package org.apache.atlas.hbase.hook;


import org.apache.atlas.plugin.classloader.AtlasPluginClassLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.SnapshotDescription;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.coprocessor.MasterCoprocessor;
import org.apache.hadoop.hbase.coprocessor.MasterCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.MasterObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionObserver;
import org.apache.hadoop.hbase.coprocessor.RegionServerObserver;

import java.io.IOException;
import java.util.Optional;


public class HBaseAtlasCoprocessor implements MasterCoprocessor, MasterObserver, RegionObserver, RegionServerObserver {
    public static final Log LOG = LogFactory.getLog(HBaseAtlasCoprocessor.class);

    private static final String ATLAS_PLUGIN_TYPE               = "hbase";
    private static final String ATLAS_HBASE_HOOK_IMPL_CLASSNAME = "org.apache.atlas.hbase.hook.HBaseAtlasCoprocessor";

    private AtlasPluginClassLoader  atlasPluginClassLoader      = null;
    private Object                  impl                        = null;
    private MasterObserver          implMasterObserver          = null;
    private RegionObserver          implRegionObserver          = null;
    private RegionServerObserver    implRegionServerObserver    = null;
    private MasterCoprocessor	    implMasterCoprocessor	    = null;

    public HBaseAtlasCoprocessor() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.HBaseAtlasCoprocessor()");
        }

        this.init();

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.HBaseAtlasCoprocessor()");
        }
    }

    private void init(){
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.init()");
        }

        try {
            atlasPluginClassLoader = AtlasPluginClassLoader.getInstance(ATLAS_PLUGIN_TYPE, this.getClass());

            @SuppressWarnings("unchecked")
            Class<?> cls = Class.forName(ATLAS_HBASE_HOOK_IMPL_CLASSNAME, true, atlasPluginClassLoader);

            activatePluginClassLoader();

            impl                     = cls.newInstance();
            implMasterObserver       = (MasterObserver)impl;
            implRegionObserver       = (RegionObserver)impl;
            implRegionServerObserver = (RegionServerObserver)impl;
            implMasterCoprocessor 	 = (MasterCoprocessor)impl;

        } catch (Exception e) {
            // check what need to be done
            LOG.error("Error Enabling RangerHbasePlugin", e);
        } finally {
            deactivatePluginClassLoader();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.init()");
        }
    }

    @Override
    public Optional<MasterObserver> getMasterObserver() {
        return Optional.<MasterObserver>of(this);
    }

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.start()");
        }

        try {
            activatePluginClassLoader();
            if (env instanceof MasterCoprocessorEnvironment) {
                implMasterCoprocessor.start(env);
            }
        } finally {
            deactivatePluginClassLoader();
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.start()");
        }
    }

    @Override
    public void postCreateTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableDescriptor desc, RegionInfo[] regions) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.postCreateTable()");
        }

        try {
            activatePluginClassLoader();
            implMasterObserver.postCreateTable(ctx, desc, regions);
        } finally {
            deactivatePluginClassLoader();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postCreateTable()");
        }
    }

    @Override
    public void postModifyTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, TableDescriptor htd) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.postModifyTable()");
        }

        try {
            activatePluginClassLoader();
            implMasterObserver.postModifyTable(ctx, tableName, htd);
        } finally {
            deactivatePluginClassLoader();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postModifyTable()");
        }
    }

    @Override
    public void postDeleteTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.postDeleteTable()");
        }

        try {
            activatePluginClassLoader();
            implMasterObserver.postDeleteTable(ctx, tableName);
        } finally {
            deactivatePluginClassLoader();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postDeleteTable()");
        }
    }

    @Override
    public void postCreateNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.preCreateNamespace()");
        }

        try {
            activatePluginClassLoader();
            implMasterObserver.postCreateNamespace(ctx, ns);
        } finally {
            deactivatePluginClassLoader();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.preCreateNamespace()");
        }
    }

    @Override
    public void postDeleteNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, String ns) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.preDeleteNamespace()");
        }

        try {
            activatePluginClassLoader();
            implMasterObserver.postDeleteNamespace(ctx, ns);
        } finally {
            deactivatePluginClassLoader();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.preDeleteNamespace()");
        }
    }
    @Override
    public void postModifyNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.preModifyNamespace()");
        }

        try {
            activatePluginClassLoader();
            implMasterObserver.preModifyNamespace(ctx, ns);
        } finally {
            deactivatePluginClassLoader();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.preModifyNamespace()");
        }
    }

    @Override
    public void postCloneSnapshot(ObserverContext<MasterCoprocessorEnvironment> observerContext, SnapshotDescription snapshot, TableDescriptor tableDescriptor) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.postCloneSnapshot()");
        }

        try {
            activatePluginClassLoader();
            implMasterObserver.postCloneSnapshot(observerContext,snapshot,tableDescriptor);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postCloneSnapshot()");
        }
    }

    @Override
    public void postRestoreSnapshot(ObserverContext<MasterCoprocessorEnvironment> observerContext, SnapshotDescription snapshot, TableDescriptor tableDescriptor) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.postRestoreSnapshot()");
        }

        try {
            activatePluginClassLoader();
            implMasterObserver.postRestoreSnapshot(observerContext,snapshot,tableDescriptor);
        } finally {
            deactivatePluginClassLoader();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postRestoreSnapshot()");
        }
    }

    private void activatePluginClassLoader() {
        if(atlasPluginClassLoader != null) {
            atlasPluginClassLoader.activate();
        }
    }

    private void deactivatePluginClassLoader() {
        if(atlasPluginClassLoader != null) {
            atlasPluginClassLoader.deactivate();
        }
    }

}
