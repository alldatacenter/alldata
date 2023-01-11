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

package org.apache.ranger.common;

import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.plugin.model.GroupInfo;
import org.apache.ranger.plugin.model.UserInfo;
import org.apache.ranger.plugin.util.RangerUserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RangerUserStoreCache {
	private static final Logger LOG = LoggerFactory.getLogger(RangerUserStoreCache.class);

	private static final int MAX_WAIT_TIME_FOR_UPDATE = 10;

	public static volatile RangerUserStoreCache 	sInstance = null;
	private final int 								waitTimeInSeconds;
	private final ReentrantLock 					lock = new ReentrantLock();
	private RangerUserStore 						rangerUserStore;

	public static RangerUserStoreCache getInstance() {
		if (sInstance == null) {
			synchronized (RangerUserStoreCache.class) {
				if (sInstance == null) {
					sInstance = new RangerUserStoreCache();
				}
			}
		}
		return sInstance;
	}

	private RangerUserStoreCache() {
		RangerAdminConfig config = RangerAdminConfig.getInstance();
		waitTimeInSeconds = config.getInt("ranger.admin.userstore.download.cache.max.waittime.for.update", MAX_WAIT_TIME_FOR_UPDATE);
		this.rangerUserStore = new RangerUserStore();
	}

	public RangerUserStore getRangerUserStore() {
		return this.rangerUserStore;
	}

	public RangerUserStore getLatestRangerUserStoreOrCached(XUserMgr xUserMgr, Long lastKnownUserStoreVersion, Long rangerUserStoreVersionInDB) throws Exception {
		RangerUserStore ret = null;

		if (lastKnownUserStoreVersion == null || !lastKnownUserStoreVersion.equals(rangerUserStoreVersionInDB)) {
			ret = getLatestRangerUserStore(xUserMgr, lastKnownUserStoreVersion, rangerUserStoreVersionInDB);
		} else if (lastKnownUserStoreVersion.equals(rangerUserStoreVersionInDB)) {
			ret = null;
		} else {
			ret = getRangerUserStore();
		}

		return ret;
	}

	public RangerUserStore getLatestRangerUserStore(XUserMgr xUserMgr, Long lastKnownUserStoreVersion, Long rangerUserStoreVersionInDB) throws Exception {
		RangerUserStore ret	 = null;
		boolean         lockResult   = false;
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerUserStoreCache.getLatestRangerUserStore(lastKnownUserStoreVersion= " + lastKnownUserStoreVersion + " rangerUserStoreVersionInDB= " + rangerUserStoreVersionInDB + ")");
		}

		try {
			lockResult = lock.tryLock(waitTimeInSeconds, TimeUnit.SECONDS);

			if (lockResult) {
				final Set<UserInfo> rangerUsersInDB = xUserMgr.getUsers();
				final Set<GroupInfo> rangerGroupsInDB = xUserMgr.getGroups();
				final Map<String, Set<String>> userGroups = xUserMgr.getUserGroups();
				if (LOG.isDebugEnabled()) {
					LOG.debug("No. of users from DB = " + rangerUsersInDB.size() + " and no. of groups from DB = " + rangerGroupsInDB.size());
					LOG.debug("No. of userGroupMappings = " + userGroups.size());
				}

				ret = new RangerUserStore(rangerUserStoreVersionInDB, rangerUsersInDB, rangerGroupsInDB, userGroups);
				rangerUserStore = ret;
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Could not get lock in [" + waitTimeInSeconds + "] seconds, returning cached RangerUserStore");
				}
				ret = getRangerUserStore();
			}
		} catch (InterruptedException exception) {
			LOG.error("RangerUserStoreCache.getLatestRangerUserStore:lock got interrupted..", exception);
		} finally {
			if (lockResult) {
				lock.unlock();
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerUserStoreCache.getLatestRangerUserStore(lastKnownUserStoreVersion= " + lastKnownUserStoreVersion + " rangerUserStoreVersionInDB= " + rangerUserStoreVersionInDB + " RangerUserStore= " + ret + ")");
		}
		return ret;
	}
}

