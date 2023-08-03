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
import org.apache.ranger.biz.RoleDBStore;
import org.apache.ranger.plugin.model.RangerRole;

import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.SearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RangerRoleCache {
	private static final Logger LOG = LoggerFactory.getLogger(RangerRoleCache.class);

	private static final int MAX_WAIT_TIME_FOR_UPDATE = 10;

	private static volatile RangerRoleCache sInstance = null;

	private final int           waitTimeInSeconds;
	private final ReentrantLock lock = new ReentrantLock();

	RangerRoleCacheWrapper roleCacheWrapper = null;

	public static RangerRoleCache getInstance() {
		if (sInstance == null) {
			synchronized (RangerRoleCache.class) {
				if (sInstance == null) {
					sInstance = new RangerRoleCache();
				}
			}
		}
		return sInstance;
	}

	private RangerRoleCache() {
		RangerAdminConfig config = RangerAdminConfig.getInstance();

		waitTimeInSeconds = config.getInt("ranger.admin.policy.download.cache.max.waittime.for.update", MAX_WAIT_TIME_FOR_UPDATE);
	}

	public RangerRoles getLatestRangerRoleOrCached(String serviceName, RoleDBStore roleDBStore, Long lastKnownRoleVersion, Long rangerRoleVersionInDB) throws Exception {
		final RangerRoles ret;

		if (lastKnownRoleVersion == null || !lastKnownRoleVersion.equals(rangerRoleVersionInDB)) {
			roleCacheWrapper = new RangerRoleCacheWrapper();
			ret              = roleCacheWrapper.getLatestRangerRoles(serviceName, roleDBStore, lastKnownRoleVersion, rangerRoleVersionInDB);
		} else {
			ret = null;
		}

		return ret;
	}

	private class RangerRoleCacheWrapper {
		RangerRoles roles;
		Long        rolesVersion;

		RangerRoleCacheWrapper() {
			this.roles        = null;
			this.rolesVersion = -1L;
		}

		public RangerRoles getRoles() {
			return this.roles;
		}

		public Long getRolesVersion() {
			return this.rolesVersion;
		}

		public RangerRoles getLatestRangerRoles(String serviceName, RoleDBStore roleDBStore, Long lastKnownRoleVersion, Long rolesVersionInDB) throws Exception {
			RangerRoles ret	       = null;
			boolean     lockResult = false;

			if (LOG.isDebugEnabled()) {
				LOG.debug("==> RangerRoleCache.getLatestRangerRoles(ServiceName= " + serviceName + " lastKnownRoleVersion= " + lastKnownRoleVersion + " rolesVersionInDB= " + rolesVersionInDB + ")");
			}

			try {
				lockResult = lock.tryLock(waitTimeInSeconds, TimeUnit.SECONDS);

				if (lockResult) {
					// We are getting all the Roles to be downloaded for now. Should do downloades for each service based on what roles are there in the policies.
					SearchFilter          searchFilter = null;
					final Set<RangerRole> rolesInDB    = new HashSet<>(roleDBStore.getRoles(searchFilter));

					Date updateTime = new Date();

					if (rolesInDB != null) {
						ret = new RangerRoles();

						ret.setRangerRoles(rolesInDB);
						ret.setRoleUpdateTime(updateTime);
						ret.setRoleVersion(rolesVersionInDB);

						rolesVersion = rolesVersionInDB;
						roles = ret;
					} else {
						LOG.error("Could not get Ranger Roles from database ...");
					}
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Could not get lock in [" + waitTimeInSeconds + "] seconds, returning cached RangerRoles");
					}
					ret = getRoles();
				}
			} catch (InterruptedException exception) {
				LOG.error("RangerRoleCache.getLatestRangerRoles:lock got interrupted..", exception);
			} finally {
				if (lockResult) {
					lock.unlock();
				}
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RangerRoleCache.getLatestRangerRoles(ServiceName= " + serviceName + " lastKnownRoleVersion= " + lastKnownRoleVersion + " rolesVersionInDB= " + rolesVersionInDB + " RangerRoles= " + ret + ")");
			}

			return ret;
		}
	}
}

