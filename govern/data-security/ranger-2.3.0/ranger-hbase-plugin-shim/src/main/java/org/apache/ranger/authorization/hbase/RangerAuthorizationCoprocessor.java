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
package org.apache.ranger.authorization.hbase;

import java.io.IOException;
import java.util.*;

import com.google.protobuf.Service;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.coprocessor.*;
import org.apache.hadoop.hbase.filter.ByteArrayComparable;
import org.apache.hadoop.hbase.io.FSDataInputStreamWrapper;
import org.apache.hadoop.hbase.io.Reference;
import org.apache.hadoop.hbase.io.hfile.CacheConfig;
import org.apache.hadoop.hbase.master.RegionPlan;
import org.apache.hadoop.hbase.net.Address;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos.CheckPermissionsRequest;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos.CheckPermissionsResponse;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos.GetUserPermissionsRequest;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos.GetUserPermissionsResponse;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos.GrantRequest;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos.GrantResponse;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos.RevokeRequest;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos.RevokeResponse;
import org.apache.hadoop.hbase.quotas.GlobalQuotaSettings;
import org.apache.hadoop.hbase.regionserver.*;
import org.apache.hadoop.hbase.regionserver.compactions.CompactionLifeCycleTracker;
import org.apache.hadoop.hbase.regionserver.compactions.CompactionRequest;
import org.apache.hadoop.hbase.regionserver.querymatcher.DeleteTracker;
import org.apache.hadoop.hbase.regionserver.Region.Operation;
import org.apache.hadoop.hbase.replication.ReplicationEndpoint;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.hadoop.hbase.wal.WALEdit;
import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

public class RangerAuthorizationCoprocessor implements RegionCoprocessor, MasterCoprocessor, RegionServerCoprocessor, MasterObserver, RegionObserver, RegionServerObserver, EndpointObserver, BulkLoadObserver, AccessControlProtos.AccessControlService.Interface {

	public static final Logger LOG = LoggerFactory.getLogger(RangerAuthorizationCoprocessor.class);
	private static final String   RANGER_PLUGIN_TYPE                      = "hbase";
	private static final String   RANGER_HBASE_AUTHORIZER_IMPL_CLASSNAME  = "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor";

	private RangerPluginClassLoader 							rangerPluginClassLoader 	= null;
	private Object                 								impl                     	= null;
	private MasterObserver      							    implMasterObserver       	= null;
	private RegionObserver       							    implRegionObserver       	= null;
	private RegionServerObserver 							    implRegionServerObserver 	= null;
	private BulkLoadObserver                                    implBulkLoadObserver     	= null;
	private AccessControlProtos.AccessControlService.Interface  implAccessControlService 	= null;
	private MasterCoprocessor									implMasterCoprocessor	 	= null;
	private RegionCoprocessor									implRegionCoprocessor	 	= null;
	private RegionServerCoprocessor								implRegionServerCoporcessor = null;
	//private EndpointObserver									implEndpointObserver		= null;

	public RangerAuthorizationCoprocessor() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.RangerAuthorizationCoprocessor()");
		}

		this.init();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.RangerAuthorizationCoprocessor()");
		}
	}

	private void init(){
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.init()");
		}

		try {

			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());

			@SuppressWarnings("unchecked")
			Class<?> cls = Class.forName(RANGER_HBASE_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();

			impl 					 = cls.newInstance();
			implAccessControlService = (AccessControlProtos.AccessControlService.Interface)impl;
			implMasterCoprocessor 	 = (MasterCoprocessor)impl;
			implRegionCoprocessor	 = (RegionCoprocessor)impl;
			implRegionServerCoporcessor = (RegionServerCoprocessor)impl;
			implMasterObserver       = (MasterObserver)impl;
			implRegionObserver       = (RegionObserver)impl;
			implRegionServerObserver = (RegionServerObserver)impl;
			implBulkLoadObserver     = (BulkLoadObserver)impl;
			//implEndpointObserver	 = (EndpointObserver)impl;

		} catch (Exception e) {
			// check what need to be done
			LOG.error("Error Enabling RangerHbasePlugin", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.init()");
		}
	}
	@Override
	public Optional<RegionObserver> getRegionObserver() {
		return Optional.<RegionObserver>of(this);
	}

	@Override
	public Optional<MasterObserver> getMasterObserver() {
		return Optional.<MasterObserver>of(this);
	}

	@Override
	public Optional<EndpointObserver> getEndpointObserver() {
		return Optional.<EndpointObserver>of(this);
	}

	@Override
	public Optional<BulkLoadObserver> getBulkLoadObserver() {
		return Optional.<BulkLoadObserver>of(this);
	}

	@Override
	public Optional<RegionServerObserver> getRegionServerObserver() {
		return Optional.<RegionServerObserver>of(this);
	}

	@Override
	public void start(CoprocessorEnvironment env) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.start()");
		}

		try {
			activatePluginClassLoader();
			if (env instanceof MasterCoprocessorEnvironment) {
				implMasterCoprocessor.start(env);
			} else if (env instanceof RegionServerCoprocessorEnvironment) {
				implRegionServerCoporcessor.start(env);
			} else if (env instanceof RegionCoprocessorEnvironment) {
				implRegionCoprocessor.start(env);
			}
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.start()");
		}
	}

	@Override
	public Iterable<Service> getServices() {
		return Collections.singleton(AccessControlProtos.AccessControlService.newReflectiveService(this));
	}

	@Override
	public void postScannerClose(ObserverContext<RegionCoprocessorEnvironment> c, InternalScanner s) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postScannerClose()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postScannerClose(c, s);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postScannerClose()");
		}
	}

	@Override
	public RegionScanner postScannerOpen(ObserverContext<RegionCoprocessorEnvironment> c, Scan scan, RegionScanner s) throws IOException {
		final RegionScanner ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postScannerOpen()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postScannerOpen(c, scan, s);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postScannerOpen()");
		}

		return ret;
	}

	@Override
	public void postStartMaster(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postStartMaster()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postStartMaster(ctx);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postStartMaster()");
		}

	}

	@Override
	public Result preAppend(ObserverContext<RegionCoprocessorEnvironment> c, Append append) throws IOException {
		final Result ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preAppend()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preAppend(c, append);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preAppend()");
		}

		return ret;
	}

	@Override
	public void preAssign(ObserverContext<MasterCoprocessorEnvironment> c, RegionInfo regionInfo) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preAssign()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preAssign(c, regionInfo);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preAssign()");
		}
	}

	@Override
	public void preBalance(ObserverContext<MasterCoprocessorEnvironment> c)	throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preBalance()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preBalance(c);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preBalance()");
		}
	}

	@Override
	public void preBalanceSwitch(ObserverContext<MasterCoprocessorEnvironment> c, boolean newValue) 	throws IOException {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preBalanceSwitch()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preBalanceSwitch(c, newValue);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preBalanceSwitch()");
		}

	}

	@Override
	public void preBulkLoadHFile(ObserverContext<RegionCoprocessorEnvironment> ctx, List<Pair<byte[], String>> familyPaths) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preBulkLoadHFile()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preBulkLoadHFile(ctx, familyPaths);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preBulkLoadHFile()");
		}

	}

	@Override
	public boolean preCheckAndDelete(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator compareOp, ByteArrayComparable comparator, Delete delete, boolean result) throws IOException {
		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCheckAndDelete()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preCheckAndDelete(c, row, family, qualifier, compareOp, comparator, delete, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCheckAndDelete()");
		}

		return ret;
	}

	@Override
	public boolean preCheckAndPut(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator compareOp, ByteArrayComparable comparator, Put put, boolean result) throws IOException {
		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCheckAndPut()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preCheckAndPut(c, row, family, qualifier, compareOp, comparator, put, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCheckAndPut()");
		}

		return ret;
	}

	@Override
	public void preCloneSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot, TableDescriptor hTableDescriptor) throws IOException {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCloneSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preCloneSnapshot(ctx, snapshot, hTableDescriptor);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCloneSnapshot()");
		}
	}

	@Override
	public void preClose(ObserverContext<RegionCoprocessorEnvironment> e,boolean abortRequested) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preClose()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preClose(e, abortRequested);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preClose()");
		}
	}

	@Override
	public void preCreateTable(ObserverContext<MasterCoprocessorEnvironment> c, TableDescriptor desc, RegionInfo[] regions) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCreateTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preCreateTable(c, desc, regions);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCreateTable()");
		}
	}

	@Override
	public void preDelete(ObserverContext<RegionCoprocessorEnvironment> c, Delete delete, WALEdit edit, Durability durability) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preDelete()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preDelete(c, delete, edit, durability);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preDelete()");
		}
	}

	@Override
	public void preDeleteSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preDeleteSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preDeleteSnapshot(ctx, snapshot);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preDeleteSnapshot()");
		}
	}

	@Override
	public void preDeleteTable(ObserverContext<MasterCoprocessorEnvironment> c, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preDeleteTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preDeleteTable(c, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preDeleteTable()");
		}
	}

	@Override
	public void preDisableTable(ObserverContext<MasterCoprocessorEnvironment> c, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preDisableTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preDisableTable(c, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preDisableTable()");
		}
	}

	@Override
	public void preEnableTable(ObserverContext<MasterCoprocessorEnvironment> c, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preEnableTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preEnableTable(c, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preEnableTable()");
		}
	}

	@Override
	public boolean preExists(ObserverContext<RegionCoprocessorEnvironment> c, Get get, boolean exists) throws IOException {
		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preExists()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preExists(c, get, exists);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preExists()");
		}

		return ret;
	}

	@Override
	public void preFlush(ObserverContext<RegionCoprocessorEnvironment> e, FlushLifeCycleTracker tracker) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preFlush()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preFlush(e, tracker);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preFlush()");
		}
	}

	@Override
	public Result preIncrement(ObserverContext<RegionCoprocessorEnvironment> c,	Increment increment) throws IOException {
		final Result ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preIncrement()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preIncrement(c, increment);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preIncrement()");
		}

		return ret;
	}

	@Override
	public void preModifyTable(ObserverContext<MasterCoprocessorEnvironment> c,	TableName tableName, TableDescriptor htd) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preModifyTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preModifyTable(c, tableName, htd);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preModifyTable()");
		}
	}

	@Override
	public void preMove(ObserverContext<MasterCoprocessorEnvironment> c, RegionInfo region, ServerName srcServer, ServerName destServer) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preMove()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preMove(c, region, srcServer, destServer);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preMove()");
		}
	}

	@Override
	public void preOpen(ObserverContext<RegionCoprocessorEnvironment> e) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preOpen()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preOpen(e);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preOpen()");
		}
	}

	@Override
	public void preRestoreSnapshot(	ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot, TableDescriptor hTableDescriptor) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preRestoreSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preRestoreSnapshot(ctx, snapshot, hTableDescriptor);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preRestoreSnapshot()");
		}
	}

	@Override
	public void preScannerClose(ObserverContext<RegionCoprocessorEnvironment> c, InternalScanner s) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preScannerClose()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preScannerClose(c, s);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preScannerClose()");
		}
	}

	@Override
	public boolean preScannerNext(ObserverContext<RegionCoprocessorEnvironment> c, InternalScanner s, List<Result> result, int limit, boolean hasNext) throws IOException {
		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preScannerNext()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preScannerNext(c, s, result, limit, hasNext);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preScannerNext()");
		}

		return ret;
	}

	@Override
	public void preScannerOpen(ObserverContext<RegionCoprocessorEnvironment> c, Scan scan) throws IOException {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preScannerOpen()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preScannerOpen(c, scan);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preScannerOpen()");
		}

	}

	@Override
	public void preShutdown(ObserverContext<MasterCoprocessorEnvironment> c) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preShutdown()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preShutdown(c);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preShutdown()");
		}
	}

	@Override
	public void preSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot, TableDescriptor hTableDescriptor)	throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preSnapshot(ctx, snapshot, hTableDescriptor);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preSnapshot()");
		}
	}

	@Override
	public void preStopMaster(ObserverContext<MasterCoprocessorEnvironment> c) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preStopMaster()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preStopMaster(c);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preStopMaster()");
		}
	}

	@Override
	public void preStopRegionServer( ObserverContext<RegionServerCoprocessorEnvironment> env) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preStopRegionServer()");
		}

		try {
			activatePluginClassLoader();
			implRegionServerObserver.preStopRegionServer(env);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preStopRegionServer()");
		}
	}

	@Override
	public void preUnassign(ObserverContext<MasterCoprocessorEnvironment> c, RegionInfo regionInfo, boolean force) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preUnassign()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preUnassign(c, regionInfo, force);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preUnassign()");
		}
	}

	@Override
	public void preSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName,	GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preSetUserQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preSetUserQuota(ctx, userName, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preSetUserQuota()");
		}
	}

	@Override
	public void preSetUserQuota( ObserverContext<MasterCoprocessorEnvironment> ctx, String userName, TableName tableName, GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preSetUserQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preSetUserQuota(ctx, userName, tableName, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preSetUserQuota()");
		}
	}

	@Override
	public void preSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName,	String namespace, GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preSetUserQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preSetUserQuota(ctx, userName, namespace, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preSetUserQuota()");
		}
	}

	@Override
	public void preSetTableQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preSetTableQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preSetTableQuota(ctx, tableName, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preSetTableQuota()");
		}
	}

	@Override
	public void preSetNamespaceQuota(ObserverContext<MasterCoprocessorEnvironment> ctx,	String namespace, GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preSetNamespaceQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preSetNamespaceQuota(ctx, namespace, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preSetNamespaceQuota()");
		}
	}

	@Override
	public void prePut(ObserverContext<RegionCoprocessorEnvironment> c,	Put put, WALEdit edit, Durability durability) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.prePut()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.prePut(c, put, edit, durability);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.prePut()");
		}
	}

	@Override
	public void preGetOp(ObserverContext<RegionCoprocessorEnvironment> rEnv, Get get, List<Cell> result) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preGetOp()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preGetOp(rEnv, get, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preGetOp()");
		}
	}

	@Override
	public void preRegionOffline( ObserverContext<MasterCoprocessorEnvironment> c, RegionInfo regionInfo) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preRegionOffline()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preRegionOffline(c, regionInfo);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preRegionOffline()");
		}
	}

	@Override
	public void preCreateNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx,NamespaceDescriptor ns) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCreateNamespace()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preCreateNamespace(ctx, ns);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCreateNamespace()");
		}
	}

	@Override
	public void preDeleteNamespace( ObserverContext<MasterCoprocessorEnvironment> ctx, String namespace) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preDeleteNamespace()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preDeleteNamespace(ctx, namespace);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preDeleteNamespace()");
		}
	}

	@Override
	public void preModifyNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preModifyNamespace()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preModifyNamespace(ctx, ns);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preModifyNamespace()");
		}
	}

	@Override
	public void postGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx, List<TableName> tableNamesList, List<TableDescriptor> descriptors, String regex) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postGetTableDescriptors()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postGetTableDescriptors(ctx, tableNamesList, descriptors, regex);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postGetTableDescriptors()");
		}
	}

	@Override
	public void prePrepareBulkLoad(ObserverContext<RegionCoprocessorEnvironment> ctx) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.prePrepareBulkLoad()");
		}

		try {
			activatePluginClassLoader();
			implBulkLoadObserver.prePrepareBulkLoad(ctx);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.prePrepareBulkLoad()");
		}
	}

	@Override
	public void preCleanupBulkLoad(ObserverContext<RegionCoprocessorEnvironment> ctx) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCleanupBulkLoad()");
		}

		try {
			activatePluginClassLoader();
			implBulkLoadObserver.preCleanupBulkLoad(ctx);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCleanupBulkLoad()");
		}
	}

	@Override
	public void grant(RpcController controller, GrantRequest request, RpcCallback<GrantResponse> done) {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.grant()");
		}

		try {
			activatePluginClassLoader();
			implAccessControlService.grant(controller, request, done);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.grant()");
		}
	}

	@Override
	public void revoke(RpcController controller, RevokeRequest request, RpcCallback<RevokeResponse> done) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.revoke()");
		}

		try {
			activatePluginClassLoader();
			implAccessControlService.revoke(controller, request, done);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.revoke()");
		}
	}

	@Override
	public void checkPermissions(RpcController controller, CheckPermissionsRequest request, RpcCallback<CheckPermissionsResponse> done) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.checkPermissions()");
		}

		try {
			activatePluginClassLoader();
			implAccessControlService.checkPermissions(controller, request, done);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.checkPermissions()");
		}
	}

	@Override
	public void hasPermission(RpcController controller, AccessControlProtos.HasPermissionRequest request, RpcCallback<AccessControlProtos.HasPermissionResponse> done) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.hasPermission()");
		}

		try {
			activatePluginClassLoader();
			implAccessControlService.hasPermission(controller, request, done);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.hasPermission()");
		}
	}

	@Override
	public void getUserPermissions(RpcController controller, GetUserPermissionsRequest request,	RpcCallback<GetUserPermissionsResponse> done) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.getUserPermissions()");
		}

		try {
			activatePluginClassLoader();
			implAccessControlService.getUserPermissions(controller, request, done);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.getUserPermissions()");
		}
	}

	@Override
	public void preRollWALWriterRequest(ObserverContext<RegionServerCoprocessorEnvironment> ctx) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preRollWALWriterRequest()");
		}

		try {
			activatePluginClassLoader();
			implRegionServerObserver.preRollWALWriterRequest(ctx);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preRollWALWriterRequest()");
		}
	}

	@Override
	public void postRollWALWriterRequest(ObserverContext<RegionServerCoprocessorEnvironment> ctx) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postRollWALWriterRequest()");
		}

		try {
			activatePluginClassLoader();
			implRegionServerObserver.postRollWALWriterRequest(ctx);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postRollWALWriterRequest()");
		}
	}

	@Override
	public ReplicationEndpoint postCreateReplicationEndPoint(ObserverContext<RegionServerCoprocessorEnvironment> ctx, ReplicationEndpoint endpoint) {

		final ReplicationEndpoint ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCreateReplicationEndPoint()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionServerObserver.postCreateReplicationEndPoint(ctx, endpoint);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCreateReplicationEndPoint()");
		}

		return ret;
	}

	@Override
	public void postOpen(ObserverContext<RegionCoprocessorEnvironment> c) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postOpen()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postOpen(c);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postOpen()");
		}
	}

	@Override
	public InternalScanner preFlush(ObserverContext<RegionCoprocessorEnvironment> c, Store store, InternalScanner scanner, FlushLifeCycleTracker tracker) throws IOException {

		final InternalScanner ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preFlush()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preFlush(c, store, scanner, tracker);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preFlush()");
		}

		return ret;
	}

	@Override
	public void postFlush(ObserverContext<RegionCoprocessorEnvironment> c, FlushLifeCycleTracker tracker) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postFlush()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postFlush(c, tracker);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postFlush()");
		}
	}

	@Override
	public void postFlush(ObserverContext<RegionCoprocessorEnvironment> c, Store store, StoreFile resultFile, FlushLifeCycleTracker tracker) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postFlush()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postFlush(c, store, resultFile, tracker);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postFlush()");
		}
	}

	@Override
	public void preCompactSelection(ObserverContext<RegionCoprocessorEnvironment> c, Store store, List<? extends StoreFile> candidates, CompactionLifeCycleTracker request) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCompactSelection()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preCompactSelection(c, store, candidates, request);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCompactSelection()");
		}
	}

	@Override
	public void postCompactSelection(ObserverContext<RegionCoprocessorEnvironment> c, Store store, List<? extends StoreFile> selected, CompactionLifeCycleTracker tracker, CompactionRequest request) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCompactSelection()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postCompactSelection(c, store, selected, tracker, request);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCompactSelection()");
		}
	}

	@Override
	public InternalScanner preCompact(ObserverContext<RegionCoprocessorEnvironment> c, Store store,	InternalScanner scanner,
									  ScanType scanType, CompactionLifeCycleTracker tracker, CompactionRequest request) throws IOException {

		final InternalScanner ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCompact()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preCompact(c, store, scanner, scanType, tracker, request);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCompact()");
		}

		return ret;
	}

	@Override
	public void postCompact(ObserverContext<RegionCoprocessorEnvironment> c, Store store, StoreFile resultFile,
							CompactionLifeCycleTracker tracker, CompactionRequest request) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCompact()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postCompact(c, store, resultFile, tracker, request);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCompact()");
		}
	}

	@Override
	public void postClose(ObserverContext<RegionCoprocessorEnvironment> c, boolean abortRequested) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postClose()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postClose(c, abortRequested);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postClose()");
		}
	}

	@Override
	public void postGetOp(ObserverContext<RegionCoprocessorEnvironment> c, Get get, List<Cell> result) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postGetOp()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postGetOp(c, get, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postGetOp()");
		}
	}

	@Override
	public boolean postExists(ObserverContext<RegionCoprocessorEnvironment> c, Get get, boolean exists) throws IOException {

		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postExists()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postExists(c, get, exists);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postExists()");
		}

		return ret;
	}

	@Override
	public void postPut(ObserverContext<RegionCoprocessorEnvironment> c, Put put, WALEdit edit, Durability durability) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postPut()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postPut(c, put, edit, durability);
		} catch (IOException e) {
			LOG.error("Failed during postPut()", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postPut()");
		}
	}

	@Override
	public void prePrepareTimeStampForDeleteVersion( ObserverContext<RegionCoprocessorEnvironment> c, Mutation mutation, Cell cell, byte[] byteNow, Get get) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.prePrepareTimeStampForDeleteVersion()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.prePrepareTimeStampForDeleteVersion(c, mutation, cell, byteNow, get);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.prePrepareTimeStampForDeleteVersion()");
		}
	}

	@Override
	public void postDelete(ObserverContext<RegionCoprocessorEnvironment> c,	Delete delete, WALEdit edit, Durability durability)	throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postDelete()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postDelete(c, delete, edit, durability);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postDelete()");
		}
	}

	@Override
	public void preBatchMutate(ObserverContext<RegionCoprocessorEnvironment> c,	MiniBatchOperationInProgress<Mutation> miniBatchOp)	throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preBatchMutate()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.preBatchMutate(c, miniBatchOp);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preBatchMutate()");
		}
	}

	@Override
	public void postBatchMutate(ObserverContext<RegionCoprocessorEnvironment> c, MiniBatchOperationInProgress<Mutation> miniBatchOp) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postBatchMutate()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postBatchMutate(c, miniBatchOp);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postBatchMutate()");
		}
	}

	@Override
	public void postStartRegionOperation(ObserverContext<RegionCoprocessorEnvironment> ctx,	Operation operation) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postStartRegionOperation()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postStartRegionOperation(ctx, operation);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postStartRegionOperation()");
		}
	}

	@Override
	public void postCloseRegionOperation(ObserverContext<RegionCoprocessorEnvironment> ctx, Operation operation) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCloseRegionOperation()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postCloseRegionOperation(ctx, operation);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCloseRegionOperation()");
		}
	}

	@Override
	public void postBatchMutateIndispensably(ObserverContext<RegionCoprocessorEnvironment> ctx,	MiniBatchOperationInProgress<Mutation> miniBatchOp, boolean success) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postBatchMutateIndispensably()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postBatchMutateIndispensably(ctx, miniBatchOp, success);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postBatchMutateIndispensably()");
		}
	}

	@Override
	public boolean preCheckAndPutAfterRowLock(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator compareOp,
											  ByteArrayComparable comparator, Put put, boolean result) throws IOException {
		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCheckAndPutAfterRowLock()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preCheckAndPutAfterRowLock(c, row, family, qualifier, compareOp, comparator, put, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCheckAndPutAfterRowLock()");
		}
		return ret;
	}

	@Override
	public boolean postCheckAndPut(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator compareOp,
								   ByteArrayComparable comparator, Put put, boolean result) throws IOException {
		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCheckAndPut()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postCheckAndPut(c, row, family, qualifier, compareOp, comparator, put, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCheckAndPut()");
		}
		return ret;
	}

	@Override
	public boolean preCheckAndDeleteAfterRowLock(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator compareOp,
												 ByteArrayComparable comparator, Delete delete, boolean result) throws IOException {
		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCheckAndDeleteAfterRowLock()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preCheckAndDeleteAfterRowLock(c, row, family, qualifier, compareOp, comparator, delete, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCheckAndDeleteAfterRowLock()");
		}
		return ret;
	}

	@Override
	public boolean postCheckAndDelete(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row,byte[] family, byte[] qualifier, CompareOperator compareOp,
									  ByteArrayComparable comparator, Delete delete, boolean result)	throws IOException {
		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCheckAndDelete()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postCheckAndDelete(c, row, family, qualifier, compareOp, comparator, delete, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCheckAndDelete()");
		}
		return ret;
	}

	@Override
	public Result preAppendAfterRowLock(ObserverContext<RegionCoprocessorEnvironment> c, Append append)	throws IOException {
		final Result ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preAppendAfterRowLock()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preAppendAfterRowLock(c, append);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preAppendAfterRowLock()");
		}
		return ret;
	}

	@Override
	public Result postAppend(ObserverContext<RegionCoprocessorEnvironment> c, Append append, Result result) throws IOException {
		final Result ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postAppend()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postAppend(c, append, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postAppend()");
		}

		return ret;
	}

	@Override
	public Result preIncrementAfterRowLock(ObserverContext<RegionCoprocessorEnvironment> c, Increment increment) throws IOException {
		final Result ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preIncrementAfterRowLock()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preIncrementAfterRowLock(c, increment);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preIncrementAfterRowLock()");
		}

		return ret;
	}

	@Override
	public Result postIncrement(ObserverContext<RegionCoprocessorEnvironment> c, Increment increment, Result result) throws IOException {
		final Result ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postIncrement()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postIncrement(c, increment, result);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postIncrement()");
		}

		return ret;
	}

	@Override
	public boolean postScannerNext(	ObserverContext<RegionCoprocessorEnvironment> c, InternalScanner s,	List<Result> result, int limit, boolean hasNext) throws IOException {
		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postScannerNext()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postScannerNext(c, s, result, limit, hasNext);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postScannerNext()");
		}

		return ret;
	}

	@Override
	public boolean postScannerFilterRow( ObserverContext<RegionCoprocessorEnvironment> c, InternalScanner s, Cell currentCell,  boolean hasMore) throws IOException {

		final boolean ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postScannerFilterRow()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postScannerFilterRow(c, s, currentCell, hasMore);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postScannerFilterRow()");
		}

		return ret;
	}

	@Override
	public void postBulkLoadHFile(ObserverContext<RegionCoprocessorEnvironment> ctx,	List<Pair<byte[], String>> familyPaths, Map<byte[], List<Path>> finalPaths) throws IOException {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postBulkLoadHFile()");
		}

		try {
			activatePluginClassLoader();
			implRegionObserver.postBulkLoadHFile(ctx, familyPaths, finalPaths);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postBulkLoadHFile()");
		}
	}

	@Override
	public StoreFileReader preStoreFileReaderOpen(ObserverContext<RegionCoprocessorEnvironment> ctx, FileSystem fs, Path p, FSDataInputStreamWrapper in, long size,
												  CacheConfig cacheConf, Reference r, StoreFileReader reader) throws IOException {
		final StoreFileReader ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preStoreFileReaderOpen()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.preStoreFileReaderOpen(ctx, fs, p, in, size, cacheConf, r, reader);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preStoreFileReaderOpen()");
		}

		return ret;
	}

	@Override
	public StoreFileReader postStoreFileReaderOpen(ObserverContext<RegionCoprocessorEnvironment> ctx, FileSystem fs,	Path p, FSDataInputStreamWrapper in, long size,
												   CacheConfig cacheConf, Reference r, StoreFileReader reader) throws IOException {
		final StoreFileReader ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postStoreFileReaderOpen()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postStoreFileReaderOpen(ctx, fs, p, in, size, cacheConf, r, reader);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postStoreFileReaderOpen()");
		}

		return ret;
	}

	@Override
	public Cell postMutationBeforeWAL(ObserverContext<RegionCoprocessorEnvironment> ctx, MutationType opType, Mutation mutation, Cell oldCell, Cell newCell) throws IOException {
		final Cell ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postMutationBeforeWAL()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postMutationBeforeWAL(ctx, opType, mutation, oldCell, newCell);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postMutationBeforeWAL()");
		}

		return ret;
	}

	@Override
	public DeleteTracker postInstantiateDeleteTracker( ObserverContext<RegionCoprocessorEnvironment> ctx, DeleteTracker delTracker) throws IOException {
		final DeleteTracker ret;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postInstantiateDeleteTracker()");
		}

		try {
			activatePluginClassLoader();
			ret = implRegionObserver.postInstantiateDeleteTracker(ctx, delTracker);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postInstantiateDeleteTracker()");
		}

		return ret;
	}

	@Override
	public void postCreateTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableDescriptor desc, RegionInfo[] regions) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCreateTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postCreateTable(ctx, desc, regions);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCreateTable()");
		}
	}

	@Override
	public void preCreateTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableDescriptor desc, RegionInfo[] regions) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preCreateTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preCreateTableAction(ctx, desc, regions);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preCreateTableHandler()");
		}
	}

	@Override
	public void postCompletedCreateTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableDescriptor desc, RegionInfo[] regions) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCreateTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postCompletedCreateTableAction(ctx, desc, regions);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCreateTableHandler()");
		}
	}

	@Override
	public void postDeleteTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postDeleteTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postDeleteTable(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postDeleteTable()");
		}
	}

	@Override
	public void preDeleteTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx,TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preDeleteTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preDeleteTableAction(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preDeleteTableHandler()");
		}
	}

	@Override
	public void postCompletedDeleteTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postDeleteTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postCompletedDeleteTableAction(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postDeleteTableHandler()");
		}
	}

	@Override
	public void preTruncateTable(ObserverContext<MasterCoprocessorEnvironment> ctx,	TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preTruncateTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preTruncateTable(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preTruncateTable()");
		}
	}

	@Override
	public void postTruncateTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postTruncateTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postTruncateTable(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postTruncateTable()");
		}
	}

	@Override
	public void preTruncateTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preTruncateTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preTruncateTableAction(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preTruncateTableHandler()");
		}
	}

	@Override
	public void postCompletedTruncateTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postTruncateTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postCompletedTruncateTableAction(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postTruncateTableHandler()");
		}
	}

	@Override
	public void postModifyTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, TableDescriptor htd) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postModifyTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postModifyTable(ctx, tableName, htd);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postModifyTable()");
		}
	}

	@Override
	public void preModifyTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx,TableName tableName, TableDescriptor htd) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preModifyTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preModifyTableAction(ctx, tableName, htd);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preModifyTableHandler()");
		}
	}

	@Override
	public void postCompletedModifyTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, TableDescriptor htd) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postModifyTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postCompletedModifyTableAction(ctx, tableName, htd);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postModifyTableHandler()");
		}
	}

	@Override
	public void postEnableTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postEnableTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postEnableTable(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postEnableTable()");
		}
	}

	@Override
	public void preEnableTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preEnableTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preEnableTableAction(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preEnableTableHandler()");
		}
	}

	@Override
	public void postCompletedEnableTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postEnableTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postCompletedEnableTableAction(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postEnableTableHandler()");
		}
	}

	@Override
	public void postDisableTable( ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postDisableTable()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postDisableTable(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postDisableTable()");
		}
	}

	@Override
	public void preDisableTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preDisableTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preDisableTableAction(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preDisableTableHandler()");
		}
	}

	@Override
	public void postCompletedDisableTableAction(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postDisableTableHandler()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postCompletedDisableTableAction(ctx, tableName);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postDisableTableHandler()");
		}
	}

	@Override
	public void postMove(ObserverContext<MasterCoprocessorEnvironment> ctx,	RegionInfo region, ServerName srcServer, ServerName destServer) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postMove()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postMove(ctx, region, srcServer, destServer);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postMove()");
		}
	}

	@Override
	public void preAbortProcedure(ObserverContext<MasterCoprocessorEnvironment> observerContext, long procId) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preAbortProcedure()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preAbortProcedure(observerContext, procId);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preAbortProcedure()");
		}
	}

	@Override
	public void postAbortProcedure(ObserverContext<MasterCoprocessorEnvironment> observerContext) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postAbortProcedure()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postAbortProcedure(observerContext);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postAbortProcedure()");
		}
	}

	@Override
	public void preGetProcedures(ObserverContext<MasterCoprocessorEnvironment> observerContext) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preListProcedures()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preGetProcedures(observerContext);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preListProcedures()");
		}
	}

	@Override
	public void postGetProcedures(ObserverContext<MasterCoprocessorEnvironment> observerContext) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postListProcedures()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postGetProcedures(observerContext);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postListProcedures()");
		}
	}

	@Override
	public void postAssign(ObserverContext<MasterCoprocessorEnvironment> ctx, RegionInfo regionInfo) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postAssign()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postAssign(ctx, regionInfo);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postAssign()");
		}
	}

	@Override
	public void postUnassign(ObserverContext<MasterCoprocessorEnvironment> ctx,	RegionInfo regionInfo, boolean force) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postUnassign()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postUnassign(ctx, regionInfo, force);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postUnassign()");
		}
	}

	@Override
	public void postRegionOffline(ObserverContext<MasterCoprocessorEnvironment> ctx, RegionInfo regionInfo) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postRegionOffline()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postRegionOffline(ctx, regionInfo);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postRegionOffline()");
		}
	}

	@Override
	public void postBalance(ObserverContext<MasterCoprocessorEnvironment> ctx, List<RegionPlan> plans) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postBalance()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postBalance(ctx, plans);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postBalance()");
		}
	}

	@Override
	public void postBalanceSwitch(ObserverContext<MasterCoprocessorEnvironment> ctx, boolean oldValue, boolean newValue) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postBalanceSwitch()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postBalanceSwitch(ctx, oldValue, newValue);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postBalanceSwitch()");
		}
	}

	@Override
	public void preMasterInitialization(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preMasterInitialization()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preMasterInitialization(ctx);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preMasterInitialization()");
		}
	}

	@Override
	public void postSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx,	SnapshotDescription snapshot, TableDescriptor hTableDescriptor) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postSnapshot(ctx, snapshot, hTableDescriptor );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postSnapshot()");
		}
	}

	@Override
	public void preListSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preListSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preListSnapshot(ctx, snapshot);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preListSnapshot()");
		}
	}

	@Override
	public void postListSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx,	SnapshotDescription snapshot) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postListSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postListSnapshot(ctx, snapshot);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postListSnapshot()");
		}
	}

	@Override
	public void postCloneSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot, TableDescriptor hTableDescriptor) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCloneSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postCloneSnapshot(ctx, snapshot, hTableDescriptor);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCloneSnapshot()");
		}
	}

	@Override
	public void postRestoreSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx,	SnapshotDescription snapshot, TableDescriptor hTableDescriptor) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postRestoreSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postRestoreSnapshot(ctx, snapshot, hTableDescriptor);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postRestoreSnapshot()");
		}
	}

	@Override
	public void postDeleteSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx,SnapshotDescription snapshot) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postDeleteSnapshot()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postDeleteSnapshot(ctx, snapshot);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postDeleteSnapshot()");
		}
	}

	@Override
	public void preGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx, List<TableName> tableNamesList, List<TableDescriptor> descriptors, String regex) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preGetTableDescriptors()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preGetTableDescriptors(ctx, tableNamesList, descriptors, regex );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preGetTableDescriptors()");
		}
	}

	@Override
	public void preGetTableNames(ObserverContext<MasterCoprocessorEnvironment> ctx,	List<TableDescriptor> descriptors, String regex) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preGetTableNames()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preGetTableNames(ctx, descriptors, regex );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preGetTableNames()");
		}
	}

	@Override
	public void postGetTableNames(ObserverContext<MasterCoprocessorEnvironment> ctx, List<TableDescriptor> descriptors, String regex) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postGetTableNames()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postGetTableNames(ctx, descriptors, regex );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postGetTableNames()");
		}
	}

	@Override
	public void postCreateNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx,	NamespaceDescriptor ns) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postCreateNamespace()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postCreateNamespace(ctx, ns );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postCreateNamespace()");
		}
	}

	@Override
	public void postDeleteNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, String namespace) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postDeleteNamespace()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postDeleteNamespace(ctx, namespace );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postDeleteNamespace()");
		}
	}

	@Override
	public void postModifyNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx,	NamespaceDescriptor ns) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postModifyNamespace()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postModifyNamespace(ctx, ns );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postModifyNamespace()");
		}
	}

	@Override
	public void preGetNamespaceDescriptor( ObserverContext<MasterCoprocessorEnvironment> ctx, String namespace) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preGetNamespaceDescriptor()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preGetNamespaceDescriptor(ctx, namespace );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preGetNamespaceDescriptor()");
		}
	}

	@Override
	public void postGetNamespaceDescriptor( ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postGetNamespaceDescriptor()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postGetNamespaceDescriptor(ctx, ns );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postGetNamespaceDescriptor()");
		}
	}

	@Override
	public void preListNamespaceDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx, List<NamespaceDescriptor> descriptors) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preListNamespaceDescriptors()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preListNamespaceDescriptors(ctx, descriptors );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preListNamespaceDescriptors()");
		}
	}

	@Override
	public void postListNamespaceDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx,	List<NamespaceDescriptor> descriptors) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postListNamespaceDescriptors()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postListNamespaceDescriptors(ctx, descriptors );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postListNamespaceDescriptors()");
		}
	}

	@Override
	public void preTableFlush(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.preTableFlush()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.preTableFlush(ctx, tableName );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.preTableFlush()");
		}
	}

	@Override
	public void postTableFlush(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postTableFlush()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postTableFlush(ctx, tableName );
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postTableFlush()");
		}
	}

	@Override
	public void postSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName, GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postSetUserQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postSetUserQuota(ctx, userName, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postSetUserQuota()");
		}
	}

	@Override
	public void postSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName,TableName tableName, GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postSetUserQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postSetUserQuota(ctx, userName, tableName, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postSetUserQuota()");
		}
	}

	@Override
	public void postSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName, String namespace, GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postSetUserQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postSetUserQuota(ctx, userName, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postSetUserQuota()");
		}
	}

	@Override
	public void postSetTableQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postSetTableQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postSetTableQuota(ctx, tableName, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postSetTableQuota()");
		}
	}

	@Override
	public void postSetNamespaceQuota(ObserverContext<MasterCoprocessorEnvironment> ctx,String namespace, GlobalQuotaSettings quotas) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postSetNamespaceQuota()");
		}

		try {
			activatePluginClassLoader();
			implMasterObserver.postSetNamespaceQuota(ctx, namespace, quotas);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postSetNamespaceQuota()");
		}
	}

	private void activatePluginClassLoader() {
		if(rangerPluginClassLoader != null) {
			rangerPluginClassLoader.activate();
		}
	}

	private void deactivatePluginClassLoader() {
		if(rangerPluginClassLoader != null) {
			rangerPluginClassLoader.deactivate();
		}
	}



	// TODO : need override annotations for all of the following methods

	public void preMoveServers(final ObserverContext<MasterCoprocessorEnvironment> ctx, Set<Address> servers, String targetGroup) throws IOException {}
	public void postMoveServers(ObserverContext<MasterCoprocessorEnvironment> ctx, Set<Address> servers, String targetGroup) throws IOException {}
	public void preMoveTables(final ObserverContext<MasterCoprocessorEnvironment> ctx, Set<TableName> tables, String targetGroup) throws IOException {}
	public void postMoveTables(final ObserverContext<MasterCoprocessorEnvironment> ctx, Set<TableName> tables, String targetGroup) throws IOException {}
	public void preRemoveRSGroup(final ObserverContext<MasterCoprocessorEnvironment> ctx, String name) throws IOException {}
	public void postRemoveRSGroup(final ObserverContext<MasterCoprocessorEnvironment> ctx, String name) throws IOException {}
	public void preBalanceRSGroup(final ObserverContext<MasterCoprocessorEnvironment> ctx, String groupName) throws IOException {}
	public void postBalanceRSGroup(final ObserverContext<MasterCoprocessorEnvironment> ctx, String groupName, boolean balancerRan) throws IOException {}
	public void preAddRSGroup(ObserverContext<MasterCoprocessorEnvironment> ctx, String name) throws IOException {}
	public void postAddRSGroup(ObserverContext<MasterCoprocessorEnvironment> ctx, String name) throws IOException {}

	public void postDispatchMerge(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1,
								  HRegionInfo arg2)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void postSetSplitOrMergeEnabled(ObserverContext<MasterCoprocessorEnvironment> arg0, boolean arg1,
										   MasterSwitchType arg2)
			throws IOException {
		// TODO Auto-generated method stub

	}
	public void preDispatchMerge(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1,
								 HRegionInfo arg2)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void preSetSplitOrMergeEnabled(ObserverContext<MasterCoprocessorEnvironment> arg0, boolean arg1,
											 MasterSwitchType arg2)
			throws IOException {
		// TODO Auto-generated method stub
	}
}
