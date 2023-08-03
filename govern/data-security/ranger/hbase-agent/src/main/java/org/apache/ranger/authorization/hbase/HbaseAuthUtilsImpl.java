/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.authorization.hbase;

import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.security.access.Permission.Action;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HbaseAuthUtilsImpl implements HbaseAuthUtils {

	private static final Logger LOG = LoggerFactory.getLogger(HbaseAuthUtilsImpl.class.getName());
	@Override
	public String getAccess(Action action) {
		switch(action) {
			case READ:
				return ACCESS_TYPE_READ;
			case WRITE:
				return ACCESS_TYPE_WRITE;
			case CREATE:
				return ACCESS_TYPE_CREATE;
			case ADMIN:
				return ACCESS_TYPE_ADMIN;
			case EXEC:
				return ACCESS_TYPE_EXECUTE;
			default:
				return action.name().toLowerCase();
		}
	}

	@Override
	public boolean isReadAccess(String access) {
		return getAccess(Action.READ).equals(access);
	}

	@Override
	public boolean isWriteAccess(String access) {
		return getAccess(Action.WRITE).equals(access);
	}

	@Override
	public boolean isExecuteAccess(String access) {
		return getAccess(Action.EXEC).equals(access);
	}

	@Override
	public String getTable(RegionCoprocessorEnvironment regionServerEnv) {
		RegionInfo hri = regionServerEnv.getRegion().getRegionInfo();
		byte[] tableName = hri.getTable().getName();
		String tableNameStr = Bytes.toString(tableName);
		if (LOG.isDebugEnabled()) {
			String message = String.format("getTable: Returning tablename[%s]", tableNameStr);
			LOG.debug(message);
		}
		return tableNameStr;
	}

	@Override
	public String getActionName(String access) {
		switch(access) {
			case ACCESS_TYPE_READ:
				return Action.READ.name();
			case ACCESS_TYPE_WRITE:
				return Action.WRITE.name();
			case ACCESS_TYPE_CREATE:
				return Action.CREATE.name();
			case ACCESS_TYPE_ADMIN:
				return Action.ADMIN.name();
			case ACCESS_TYPE_EXECUTE:
				return Action.EXEC.name();
			default:
				return access.toUpperCase();
		}
	}
}
