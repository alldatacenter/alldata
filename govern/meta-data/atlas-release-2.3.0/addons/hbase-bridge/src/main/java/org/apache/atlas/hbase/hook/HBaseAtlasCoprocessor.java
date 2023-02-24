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

package org.apache.atlas.hbase.hook;


import org.apache.atlas.hbase.bridge.HBaseAtlasHook;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.client.SnapshotDescription;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.coprocessor.MasterCoprocessor;
import org.apache.hadoop.hbase.coprocessor.BulkLoadObserver;
import org.apache.hadoop.hbase.coprocessor.MasterCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.MasterObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionObserver;
import org.apache.hadoop.hbase.coprocessor.RegionServerObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HBaseAtlasCoprocessor implements MasterCoprocessor, MasterObserver, RegionObserver, RegionServerObserver  {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseAtlasCoprocessor.class);

    final HBaseAtlasHook hbaseAtlasHook;

    public HBaseAtlasCoprocessor() {
        hbaseAtlasHook = HBaseAtlasHook.getInstance();
    }

    @Override
    public void postCreateTable(ObserverContext<MasterCoprocessorEnvironment> observerContext, TableDescriptor tableDescriptor, RegionInfo[] hRegionInfos) throws IOException {
        LOG.info("==> HBaseAtlasCoprocessor.postCreateTable()");

        hbaseAtlasHook.sendHBaseTableOperation(tableDescriptor, null, HBaseAtlasHook.OPERATION.CREATE_TABLE, observerContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postCreateTable()");
        }
    }

    @Override
    public void postDeleteTable(ObserverContext<MasterCoprocessorEnvironment> observerContext, TableName tableName) throws IOException {
        LOG.info("==> HBaseAtlasCoprocessor.postDeleteTable()");
        hbaseAtlasHook.sendHBaseTableOperation(null, tableName, HBaseAtlasHook.OPERATION.DELETE_TABLE, observerContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postDeleteTable()");
        }
    }

    @Override
    public void postModifyTable(ObserverContext<MasterCoprocessorEnvironment> observerContext, TableName tableName, TableDescriptor tableDescriptor) throws IOException {
        LOG.info("==> HBaseAtlasCoprocessor.postModifyTable()");
        hbaseAtlasHook.sendHBaseTableOperation(tableDescriptor, tableName, HBaseAtlasHook.OPERATION.ALTER_TABLE, observerContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postModifyTable()");
        }
    }

    @Override
    public void postCreateNamespace(ObserverContext<MasterCoprocessorEnvironment> observerContext, NamespaceDescriptor namespaceDescriptor) throws IOException {
        LOG.info("==> HBaseAtlasCoprocessor.postCreateNamespace()");

        hbaseAtlasHook.sendHBaseNameSpaceOperation(namespaceDescriptor, null, HBaseAtlasHook.OPERATION.CREATE_NAMESPACE, observerContext);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postCreateNamespace()");
        }
    }

    @Override
    public void postDeleteNamespace(ObserverContext<MasterCoprocessorEnvironment> observerContext, String s) throws IOException {
        LOG.info("==> HBaseAtlasCoprocessor.postDeleteNamespace()");

        hbaseAtlasHook.sendHBaseNameSpaceOperation(null, s, HBaseAtlasHook.OPERATION.DELETE_NAMESPACE, observerContext);

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasCoprocessor.postDeleteNamespace()");
        }
    }

    @Override
    public void postModifyNamespace(ObserverContext<MasterCoprocessorEnvironment> observerContext, NamespaceDescriptor namespaceDescriptor) throws IOException {
        LOG.info("==> HBaseAtlasCoprocessor.postModifyNamespace()");

        hbaseAtlasHook.sendHBaseNameSpaceOperation(namespaceDescriptor, null, HBaseAtlasHook.OPERATION.ALTER_NAMESPACE, observerContext);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postModifyNamespace()");
        }
    }

    @Override
    public void postCloneSnapshot(ObserverContext<MasterCoprocessorEnvironment> observerContext, SnapshotDescription snapshot, TableDescriptor tableDescriptor) throws IOException {
        LOG.info("==> HBaseAtlasCoprocessor.postCloneSnapshot()");

        hbaseAtlasHook.sendHBaseTableOperation(tableDescriptor, null, HBaseAtlasHook.OPERATION.CREATE_TABLE, observerContext);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postCloneSnapshot()");
        }
    }

    @Override
    public void postRestoreSnapshot(ObserverContext<MasterCoprocessorEnvironment> observerContext, SnapshotDescription snapshot, TableDescriptor tableDescriptor) throws IOException {
        LOG.info("==> HBaseAtlasCoprocessor.postRestoreSnapshot()");

        hbaseAtlasHook.sendHBaseTableOperation(tableDescriptor, snapshot.getTableName(), HBaseAtlasHook.OPERATION.ALTER_TABLE, observerContext);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasCoprocessor.postRestoreSnapshot()");
        }
    }

}


