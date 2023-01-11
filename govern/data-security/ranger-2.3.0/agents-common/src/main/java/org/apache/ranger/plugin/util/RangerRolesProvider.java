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

package org.apache.ranger.plugin.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.admin.client.RangerAdminClient;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.HashSet;


public class RangerRolesProvider {
	private static final Logger LOG = LoggerFactory.getLogger(RangerRolesProvider.class);

	private static final Logger PERF_POLICYENGINE_INIT_LOG = RangerPerfTracer.getPerfLogger("policyengine.init");

	private final String            serviceType;
	private final String            serviceName;
	private final RangerAdminClient rangerAdmin;

	private final String            cacheFileName;
	private final String			cacheFileNamePrefix;
	private final String            cacheDir;
	private final Gson              gson;
	private final boolean           disableCacheIfServiceNotFound;

	private long	lastActivationTimeInMillis;
	private long    lastKnownRoleVersion = -1L;
	private boolean rangerUserGroupRolesSetInPlugin;
	private boolean serviceDefSetInPlugin;

	public RangerRolesProvider(String serviceType, String appId, String serviceName, RangerAdminClient rangerAdmin, String cacheDir, Configuration config) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerRolesProvider(serviceName=" + serviceName + ").RangerRolesProvider()");
		}

		this.serviceType = serviceType;
		this.serviceName = serviceName;
		this.rangerAdmin = rangerAdmin;


		if (StringUtils.isEmpty(appId)) {
			appId = serviceType;
		}

		cacheFileNamePrefix = "roles";
		String cacheFilename = String.format("%s_%s_%s.json", appId, serviceName, cacheFileNamePrefix);
		cacheFilename = cacheFilename.replace(File.separatorChar, '_');
		cacheFilename = cacheFilename.replace(File.pathSeparatorChar, '_');

		this.cacheFileName = cacheFilename;
		this.cacheDir = cacheDir;

		Gson gson = null;
		try {
			gson = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z").create();
		} catch (Throwable excp) {
			LOG.error("RangerRolesProvider(): failed to create GsonBuilder object", excp);
		}
		this.gson = gson;

		String propertyPrefix = "ranger.plugin." + serviceType;
		disableCacheIfServiceNotFound = config.getBoolean(propertyPrefix + ".disable.cache.if.servicenotfound", true);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerRolesProvider(serviceName=" + serviceName + ").RangerRolesProvider()");
		}
	}

	public long getLastActivationTimeInMillis() {
		return lastActivationTimeInMillis;
	}

	public void setLastActivationTimeInMillis(long lastActivationTimeInMillis) {
		this.lastActivationTimeInMillis = lastActivationTimeInMillis;
	}

	public void loadUserGroupRoles(RangerBasePlugin plugIn) {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerRolesProvider(serviceName= " + serviceName  + " serviceType= " + serviceType +").loadUserGroupRoles()");
		}

		RangerPerfTracer perf = null;

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYENGINE_INIT_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_POLICYENGINE_INIT_LOG, "RangerRolesProvider.loadUserGroupRoles(serviceName=" + serviceName + ")");
			long freeMemory = Runtime.getRuntime().freeMemory();
			long totalMemory = Runtime.getRuntime().totalMemory();
			PERF_POLICYENGINE_INIT_LOG.debug("In-Use memory: " + (totalMemory-freeMemory) + ", Free memory:" + freeMemory);
		}

		try {
			//load userGroupRoles from ranger admin
			RangerRoles roles = loadUserGroupRolesFromAdmin();

			if (roles == null) {
				//if userGroupRoles fetch from ranger Admin Fails, load from cache
				if (!rangerUserGroupRolesSetInPlugin) {
					roles = loadUserGroupRolesFromCache();
				}
			}

			if (PERF_POLICYENGINE_INIT_LOG.isDebugEnabled()) {
				long freeMemory = Runtime.getRuntime().freeMemory();
				long totalMemory = Runtime.getRuntime().totalMemory();
				PERF_POLICYENGINE_INIT_LOG.debug("In-Use memory: " + (totalMemory - freeMemory) + ", Free memory:" + freeMemory);
			}

			if (roles != null) {
				plugIn.setRoles(roles);
				rangerUserGroupRolesSetInPlugin = true;
				setLastActivationTimeInMillis(System.currentTimeMillis());
				lastKnownRoleVersion = roles.getRoleVersion();
			} else {
				if (!rangerUserGroupRolesSetInPlugin && !serviceDefSetInPlugin) {
					plugIn.setRoles(null);
					serviceDefSetInPlugin = true;
				}
			}
		} catch (RangerServiceNotFoundException snfe) {
			if (disableCacheIfServiceNotFound) {
				disableCache();
				plugIn.setRoles(null);
				setLastActivationTimeInMillis(System.currentTimeMillis());
				lastKnownRoleVersion = -1L;
				serviceDefSetInPlugin = true;
			}
		} catch (Exception excp) {
			LOG.error("Encountered unexpected exception, ignoring..", excp);
		}

		RangerPerfTracer.log(perf);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerRolesProvider(serviceName=" + serviceName + ").loadUserGroupRoles()");
		}
	}

	private RangerRoles loadUserGroupRolesFromAdmin() throws RangerServiceNotFoundException {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerRolesProvider(serviceName=" + serviceName + ").loadUserGroupRolesFromAdmin()");
		}

		RangerRoles roles;

		RangerPerfTracer perf = null;

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYENGINE_INIT_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_POLICYENGINE_INIT_LOG, "RangerRolesProvider.loadUserGroupRolesFromAdmin(serviceName=" + serviceName + ")");
		}

		try {
			roles = rangerAdmin.getRolesIfUpdated(lastKnownRoleVersion, lastActivationTimeInMillis);

			boolean isUpdated = roles != null;

			if(isUpdated) {
				long newVersion = roles.getRoleVersion() == null ? -1 : roles.getRoleVersion().longValue();
				saveToCache(roles);
				LOG.info("RangerRolesProvider(serviceName=" + serviceName + "): found updated version. lastKnownRoleVersion=" + lastKnownRoleVersion + "; newVersion=" + newVersion );
			} else {
				if(LOG.isDebugEnabled()) {
					LOG.debug("RangerRolesProvider(serviceName=" + serviceName + ").run(): no update found. lastKnownRoleVersion=" + lastKnownRoleVersion );
				}
			}
		} catch (RangerServiceNotFoundException snfe) {
			LOG.error("RangerRolesProvider(serviceName=" + serviceName + "): failed to find service. Will clean up local cache of roles (" + lastKnownRoleVersion + ")", snfe);
			throw snfe;
		} catch (Exception excp) {
			LOG.error("RangerRolesProvider(serviceName=" + serviceName + "): failed to refresh roles. Will continue to use last known version of roles (" + "lastKnowRoleVersion= " + lastKnownRoleVersion, excp);
			roles = null;
		}

		RangerPerfTracer.log(perf);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerRolesProvider(serviceName=" + serviceName + " serviceType= " + serviceType + " ).loadUserGroupRolesFromAdmin()");
		 }

		 return roles;
	}

	private RangerRoles loadUserGroupRolesFromCache() {

		RangerRoles roles = null;

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerRolesProvider(serviceName=" + serviceName + ").loadUserGroupRolesFromCache()");
		}

		File cacheFile = cacheDir == null ? null : new File(cacheDir + File.separator + cacheFileName);

		if (cacheFile != null && cacheFile.isFile() && cacheFile.canRead()) {
			Reader reader = null;

			RangerPerfTracer perf = null;

			if (RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYENGINE_INIT_LOG)) {
				perf = RangerPerfTracer.getPerfTracer(PERF_POLICYENGINE_INIT_LOG, "RangerRolesProvider.loadUserGroupRolesFromCache(serviceName=" + serviceName + ")");
			}

			try {
				reader = new FileReader(cacheFile);

				roles = gson.fromJson(reader, RangerRoles.class);

				if (roles != null) {
					if (!StringUtils.equals(serviceName, roles.getServiceName())) {
						LOG.warn("ignoring unexpected serviceName '" + roles.getServiceName() + "' in cache file '" + cacheFile.getAbsolutePath() + "'");

						roles.setServiceName(serviceName);
					}

					lastKnownRoleVersion = roles.getRoleVersion() == null ? -1 : roles.getRoleVersion().longValue();
				}
			} catch (Exception excp) {
				LOG.error("failed to load userGroupRoles from cache file " + cacheFile.getAbsolutePath(), excp);
			} finally {
				RangerPerfTracer.log(perf);

				if (reader != null) {
					try {
						reader.close();
					} catch (Exception excp) {
						LOG.error("error while closing opened cache file " + cacheFile.getAbsolutePath(), excp);
					}
				}
			}
		} else {
			roles = new RangerRoles();
			roles.setServiceName(serviceName);
			roles.setRoleVersion(-1L);
			roles.setRoleUpdateTime(new Date());
			roles.setRangerRoles(new HashSet<>());
			saveToCache(roles);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerRolesProvider(serviceName=" + serviceName + ").RangerRolesProvider()");
		}

		return roles;
	}

	public void saveToCache(RangerRoles roles) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerRolesProvider(serviceName=" + serviceName + ").saveToCache()");
		}

		if(roles != null) {
			File cacheFile = null;
			if (cacheDir != null) {
				// Create the cacheDir if it doesn't already exist
				File cacheDirTmp = new File(cacheDir);
				if (cacheDirTmp.exists()) {
					cacheFile =  new File(cacheDir + File.separator + cacheFileName);
				} else {
					try {
						cacheDirTmp.mkdirs();
						cacheFile =  new File(cacheDir + File.separator + cacheFileName);
					} catch (SecurityException ex) {
						LOG.error("Cannot create cache directory", ex);
					}
				}
			}

			if(cacheFile != null) {

				RangerPerfTracer perf = null;

				if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYENGINE_INIT_LOG)) {
					perf = RangerPerfTracer.getPerfTracer(PERF_POLICYENGINE_INIT_LOG, "RangerRolesProvider.saveToCache(serviceName=" + serviceName + ")");
				}

				Writer writer = null;

				try {
					writer = new FileWriter(cacheFile);

			        gson.toJson(roles, writer);
		        } catch (Exception excp) {
					LOG.error("failed to save roles to cache file '" + cacheFile.getAbsolutePath() + "'", excp);
		        } finally {
					if(writer != null) {
						try {
							writer.close();
						} catch(Exception excp) {
							LOG.error("error while closing opened cache file '" + cacheFile.getAbsolutePath() + "'", excp);
						}
					}
				}

				RangerPerfTracer.log(perf);
			}
		} else {
			LOG.info("roles is null. Nothing to save in cache");
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerRolesProvider.saveToCache(serviceName=" + serviceName + ")");
		}
	}

	private void disableCache() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerRolesProvider.disableCache(serviceName=" + serviceName + ")");
		}

		File cacheFile = cacheDir == null ? null : new File(cacheDir + File.separator + cacheFileName);

		if(cacheFile != null && cacheFile.isFile() && cacheFile.canRead()) {
			LOG.warn("Cleaning up local RangerRoles cache");
			String renamedCacheFile = cacheFile.getAbsolutePath() + "_" + System.currentTimeMillis();
			if (!cacheFile.renameTo(new File(renamedCacheFile))) {
				LOG.error("Failed to move " + cacheFile.getAbsolutePath() + " to " + renamedCacheFile);
			} else {
				LOG.warn("Moved " + cacheFile.getAbsolutePath() + " to " + renamedCacheFile);
			}
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("No local RangerRoles cache found. No need to disable it!");
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerRolesProvider.disableCache(serviceName=" + serviceName + ")");
		}
	}
}
