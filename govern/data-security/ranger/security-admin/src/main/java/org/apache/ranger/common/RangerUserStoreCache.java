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

	private final int             waitTimeInSeconds;
	private final boolean         dedupStrings;
	private final ReentrantLock   lock = new ReentrantLock();
	private       RangerUserStore rangerUserStore;

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

		this.waitTimeInSeconds = config.getInt("ranger.admin.userstore.download.cache.max.waittime.for.update", MAX_WAIT_TIME_FOR_UPDATE);
		this.dedupStrings      = config.getBoolean("ranger.admin.userstore.dedup.strings", Boolean.TRUE);
		this.rangerUserStore   = new RangerUserStore();
	}

	public RangerUserStore getRangerUserStore() {
		return this.rangerUserStore;
	}

	public RangerUserStore getLatestRangerUserStoreOrCached(XUserMgr xUserMgr) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerUserStoreCache.getLatestRangerUserStoreOrCached()");
		}

		RangerUserStore ret        = null;
		boolean         lockResult = false;

		try {
			lockResult = lock.tryLock(waitTimeInSeconds, TimeUnit.SECONDS);

			if (lockResult) {
				Long cachedUserStoreVersion = rangerUserStore.getUserStoreVersion();
				Long dbUserStoreVersion     = xUserMgr.getUserStoreVersion();

				if (!Objects.equals(cachedUserStoreVersion, dbUserStoreVersion)) {
					LOG.info("RangerUserStoreCache refreshing from version " + cachedUserStoreVersion + " to " + dbUserStoreVersion);

					final Set<UserInfo>            rangerUsersInDB  = xUserMgr.getUsers();
					final Set<GroupInfo>           rangerGroupsInDB = xUserMgr.getGroups();
					final Map<String, Set<String>> userGroups       = xUserMgr.getUserGroups();

					if (LOG.isDebugEnabled()) {
						LOG.debug("No. of users from DB = " + rangerUsersInDB.size() + " and no. of groups from DB = " + rangerGroupsInDB.size());
						LOG.debug("No. of userGroupMappings = " + userGroups.size());
					}

					RangerUserStore rangerUserStore = new RangerUserStore(dbUserStoreVersion, rangerUsersInDB, rangerGroupsInDB, userGroups);

					if (dedupStrings) {
						rangerUserStore.dedupStrings();
					}

					this.rangerUserStore = rangerUserStore;

					LOG.info("RangerUserStoreCache refreshed from version " + cachedUserStoreVersion + " to " + dbUserStoreVersion + ": users=" + rangerUsersInDB.size() + ", groups=" + rangerGroupsInDB.size() + ", userGroupMappings=" + userGroups.size());
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Could not get lock in [" + waitTimeInSeconds + "] seconds, returning cached RangerUserStore");
				}
			}
		} catch (InterruptedException exception) {
			LOG.error("RangerUserStoreCache.getLatestRangerUserStoreOrCached:lock got interrupted..", exception);
		} finally {
			ret = rangerUserStore;

			if (lockResult) {
				lock.unlock();
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerUserStoreCache.getLatestRangerUserStoreOrCached(): ret=" + ret);
		}

		return ret;
	}
}

