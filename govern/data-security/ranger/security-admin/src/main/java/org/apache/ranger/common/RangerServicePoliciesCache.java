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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.store.ServiceStore;

import org.apache.ranger.plugin.util.RangerPolicyDeltaUtil;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RangerServicePoliciesCache {
	private static final Logger LOG = LoggerFactory.getLogger(RangerServicePoliciesCache.class);

	private static final int MAX_WAIT_TIME_FOR_UPDATE = 10;

	public static volatile RangerServicePoliciesCache sInstance = null;

	private final int     waitTimeInSeconds;
	private final boolean dedupStrings;

	private final Map<String, ServicePoliciesWrapper> servicePoliciesMap = new HashMap<>();

	public static RangerServicePoliciesCache getInstance() {
		if (sInstance == null) {
			synchronized (RangerServicePoliciesCache.class) {
				if (sInstance == null) {
					sInstance = new RangerServicePoliciesCache();
				}
			}
		}
		return sInstance;
	}

	private RangerServicePoliciesCache() {
		RangerAdminConfig config = RangerAdminConfig.getInstance();

		waitTimeInSeconds = config.getInt("ranger.admin.policy.download.cache.max.waittime.for.update", MAX_WAIT_TIME_FOR_UPDATE);
		dedupStrings      = config.getBoolean("ranger.admin.policy.dedup.strings", Boolean.TRUE);
	}

	public void dump() {
		final Set<String> serviceNames;

		synchronized (this) {
			serviceNames = servicePoliciesMap.keySet();
		}

		if (CollectionUtils.isNotEmpty(serviceNames)) {

			for (String serviceName : serviceNames) {
				final ServicePoliciesWrapper cachedServicePoliciesWrapper;

				synchronized (this) {
					cachedServicePoliciesWrapper = servicePoliciesMap.get(serviceName);
				}
				LOG.debug("serviceName:" + serviceName + ", Cached-MetaData:" + cachedServicePoliciesWrapper);

			}
		}
	}

	public ServicePolicies getServicePolicies(String serviceName, Long serviceId, Long lastKnownVersion, boolean needsBackwardCompatibility, ServiceStore serviceStore) throws Exception {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServicePoliciesCache.getServicePolicies(" + serviceName + ", " + serviceId + ", " + lastKnownVersion + ", " + needsBackwardCompatibility + ")");
		}

		ServicePolicies ret = null;

		if (StringUtils.isNotBlank(serviceName) && serviceId != null) {

			ServicePoliciesWrapper servicePoliciesWrapper;

			synchronized (this) {
				servicePoliciesWrapper = servicePoliciesMap.get(serviceName);

				if (servicePoliciesWrapper != null) {
					if (!serviceId.equals(servicePoliciesWrapper.getServiceId())) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Service [" + serviceName + "] changed service-id from " + servicePoliciesWrapper.getServiceId()
									+ " to " + serviceId);
							LOG.debug("Recreating servicePoliciesWrapper for serviceName [" + serviceName + "]");
						}
						servicePoliciesMap.remove(serviceName);
						servicePoliciesWrapper = null;
					}
				}

				if (servicePoliciesWrapper == null) {
					servicePoliciesWrapper = new ServicePoliciesWrapper(serviceId);
					servicePoliciesMap.put(serviceName, servicePoliciesWrapper);
				}
			}

			if (serviceStore != null) {
				ret = servicePoliciesWrapper.getLatestOrCached(serviceName, serviceStore, lastKnownVersion, needsBackwardCompatibility);
			} else {
				LOG.error("getServicePolicies(" + serviceName + "): failed to get latest policies as service-store is null! Returning cached servicePolicies!");
				ret = servicePoliciesWrapper.getServicePolicies();
			}

		} else {
			LOG.error("getServicePolicies() failed to get policies as serviceName is null or blank and/or serviceId is null!");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServicePoliciesCache.getServicePolicies(" + serviceName + ", " + serviceId + ", " + lastKnownVersion + ", " + needsBackwardCompatibility + "): ret:[" + ret + "]");
		}

		return ret;
	}

    /**
     * Reset policy cache using serviceName if provided.
     * If serviceName is empty, reset everything.
     * @param serviceName
     * @return true if was able to reset policy cache, false otherwise
     */
    public boolean resetCache(final String serviceName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerServicePoliciesCache.resetCache({})", serviceName);
        }

        boolean ret = false;
        synchronized (this) {
            if (!servicePoliciesMap.isEmpty()) {
                if (StringUtils.isBlank(serviceName)) {
                    servicePoliciesMap.clear();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("RangerServicePoliciesCache.resetCache(): Removed policy caching for all services.");
                    }
                    ret = true;
                } else {
                    ServicePoliciesWrapper removedServicePoliciesWrapper = servicePoliciesMap.remove(serviceName.trim()); // returns null if key not found
                    ret = removedServicePoliciesWrapper != null;

                    if (ret) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("RangerServicePoliciesCache.resetCache(): Removed policy caching for [{}] service.", serviceName);
                        }
                    } else {
                        LOG.warn("RangerServicePoliciesCache.resetCache(): Caching for [{}] service not found, hence reset is skipped.", serviceName);
                    }
                }
            } else {
                LOG.warn("RangerServicePoliciesCache.resetCache(): Policy cache is already empty.");
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerServicePoliciesCache.resetCache(): ret={}", ret);
        }

        return ret;
    }

	private class ServicePoliciesWrapper {
		final Long          serviceId;
		ServicePolicies     servicePolicies;
		Date                updateTime            = null;
		long                longestDbLoadTimeInMs = -1;
		final ReentrantLock lock = new ReentrantLock();

		ServicePolicyDeltasCache deltaCache;

		class ServicePolicyDeltasCache {
			final long            fromVersion;
			final ServicePolicies servicePolicyDeltas;

			ServicePolicyDeltasCache(final long fromVersion, ServicePolicies servicePolicyDeltas) {
				this.fromVersion         = fromVersion;
				this.servicePolicyDeltas = servicePolicyDeltas;
			}
			ServicePolicies getServicePolicyDeltasFromVersion(long fromVersion) {
				return this.fromVersion == fromVersion ? this.servicePolicyDeltas : null;
			}
		}

		ServicePoliciesWrapper(Long serviceId) {
			this.serviceId = serviceId;
			servicePolicies = null;
		}

		Long getServiceId() { return serviceId; }

		ServicePolicies getServicePolicies() {
			return servicePolicies;
		}

		Date getUpdateTime() {
			return updateTime;
		}

		ServicePolicies getLatestOrCached(String serviceName, ServiceStore serviceStore, Long lastKnownVersion, boolean needsBackwardCompatibility) throws Exception {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> RangerServicePoliciesCache.getLatestOrCached(lastKnownVersion=" + lastKnownVersion + ", " + needsBackwardCompatibility + ")");
			}
			ServicePolicies ret        = null;
			boolean         lockResult = false;

			try {
				final boolean isCacheReloadedByDQEvent;

				lockResult = lock.tryLock(waitTimeInSeconds, TimeUnit.SECONDS);

				if (lockResult) {
					isCacheReloadedByDQEvent = getLatest(serviceName, serviceStore, lastKnownVersion);

					if (isCacheReloadedByDQEvent) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("ServicePolicies cache was completely loaded from database because of a disqualifying event - such as service-definition change!");
						}
					}

					if (needsBackwardCompatibility || isCacheReloadedByDQEvent
						|| lastKnownVersion == -1L || lastKnownVersion.equals(servicePolicies.getPolicyVersion())) {
						// Looking for all policies, or Some disqualifying change encountered
						if (LOG.isDebugEnabled()) {
							LOG.debug("All policies were requested, returning cached ServicePolicies");
						}
						ret = this.servicePolicies;
					} else {
						boolean         isDeltaCacheReinitialized = false;
						ServicePolicies servicePoliciesForDeltas  = this.deltaCache != null ? this.deltaCache.getServicePolicyDeltasFromVersion(lastKnownVersion) : null;

						if (servicePoliciesForDeltas == null) {
							servicePoliciesForDeltas  = serviceStore.getServicePolicyDeltas(serviceName, lastKnownVersion);
							isDeltaCacheReinitialized = true;
						}
						if (servicePoliciesForDeltas != null && servicePoliciesForDeltas.getPolicyDeltas() != null) {
							if (LOG.isDebugEnabled()) {
								LOG.debug("Deltas were requested. Returning deltas from lastKnownVersion:[" + lastKnownVersion + "]");
							}
							if (isDeltaCacheReinitialized) {
								this.deltaCache = new ServicePolicyDeltasCache(lastKnownVersion, servicePoliciesForDeltas);
							}
							ret = servicePoliciesForDeltas;
						} else {
							LOG.warn("Deltas were requested for service:[" + serviceName + "], but could not get them!! lastKnownVersion:[" + lastKnownVersion + "]; Returning cached ServicePolicies:[" + (servicePolicies != null ? servicePolicies.getPolicyVersion() : -1L) + "]");

							this.deltaCache = null;
							ret = this.servicePolicies;
						}
					}
				} else {
					LOG.error("Could not get lock in [" + waitTimeInSeconds + "] seconds, returning cached ServicePolicies and wait Queue Length:[" +lock.getQueueLength() + "], servicePolicies version:[" + (servicePolicies != null ? servicePolicies.getPolicyVersion() : -1L) + "]");
					ret = this.servicePolicies;
				}
			} catch (InterruptedException exception) {
				LOG.error("getLatestOrCached:lock got interrupted..", exception);
			} finally {
				if (lockResult) {
					lock.unlock();
				}
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("RangerServicePoliciesCache.getLatestOrCached - Returns ServicePolicies:[" + ret +"]");
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RangerServicePoliciesCache.getLatestOrCached(lastKnownVersion=" + lastKnownVersion + ", " + needsBackwardCompatibility + ") : " + ret);
			}
			return ret;
		}

		boolean getLatest(String serviceName, ServiceStore serviceStore, Long lastKnownVersion) throws Exception {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> ServicePoliciesWrapper.getLatest(serviceName=" + serviceName + ", lastKnownVersion=" + lastKnownVersion + ")");
			}

			final Long servicePolicyVersionInDb     = serviceStore.getServicePolicyVersion(serviceName);
			final Long cachedServicePoliciesVersion = servicePolicies != null ? servicePolicies.getPolicyVersion() : -1L;

			if (LOG.isDebugEnabled()) {
				LOG.debug("ServicePolicies version in cache[" + cachedServicePoliciesVersion + "], ServicePolicies version in database[" + servicePolicyVersionInDb + "]");
			}

			boolean isCacheReloadedByDQEvent = false;

			if (servicePolicyVersionInDb == null || !servicePolicyVersionInDb.equals(cachedServicePoliciesVersion)) {

				if (LOG.isDebugEnabled()) {
					LOG.debug("loading servicePolicies from database");
				}

				final long            startTimeMs           = System.currentTimeMillis();
				final ServicePolicies servicePoliciesFromDb = serviceStore.getServicePolicyDeltasOrPolicies(serviceName, cachedServicePoliciesVersion);
				final long            dbLoadTime            = System.currentTimeMillis() - startTimeMs;

				if (dbLoadTime > longestDbLoadTimeInMs) {
					longestDbLoadTimeInMs = dbLoadTime;
				}
				updateTime = new Date();

				if (servicePoliciesFromDb != null) {
					if (dedupStrings) {
						servicePoliciesFromDb.dedupStrings();
					}

					if (LOG.isDebugEnabled()) {
						LOG.debug("Successfully loaded ServicePolicies from database: ServicePolicies:[" + servicePoliciesFromDb + "]");
					}
					if (servicePolicies == null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Initializing ServicePolicies cache for the first time");
						}
						servicePolicies = servicePoliciesFromDb;
						pruneUnusedAttributes();
					} else if (servicePoliciesFromDb.getPolicyDeltas() == null) {
						// service-policies are loaded because service/service-def changed
						if (LOG.isDebugEnabled()) {
							LOG.debug("Complete set of policies are loaded from database, because of some disqualifying event");
						}
						servicePolicies = servicePoliciesFromDb;
						pruneUnusedAttributes();
						isCacheReloadedByDQEvent = true;
					} else { // Previously cached service policies are still valid - no service/service-def change
						// Rebuild policies cache from original policies and deltas
						if (LOG.isDebugEnabled()) {
							LOG.debug("Retrieved policy-deltas from database. These will be applied on top of ServicePolicy version:[" + cachedServicePoliciesVersion +"], policy-deltas:[" + servicePoliciesFromDb.getPolicyDeltas() + "]");
						}
						servicePolicies.setPolicyVersion(servicePoliciesFromDb.getPolicyVersion());

						final List<RangerPolicy> policies = servicePolicies.getPolicies() == null ? new ArrayList<>() : servicePolicies.getPolicies();
						final List<RangerPolicy> newPolicies = RangerPolicyDeltaUtil.applyDeltas(policies, servicePoliciesFromDb.getPolicyDeltas(), servicePolicies.getServiceDef().getName());

						servicePolicies.setPolicies(newPolicies);

						checkCacheSanity(serviceName, serviceStore, false);

						// Rebuild tag-policies from original tag-policies and deltas
						if (servicePoliciesFromDb.getTagPolicies() != null) {
							String tagServiceName = servicePoliciesFromDb.getTagPolicies().getServiceName();
							if (LOG.isDebugEnabled()) {
								LOG.debug("This service has associated tag service:[" + tagServiceName + "]. Will compute tagPolicies from corresponding policy-deltas");
							}

							final List<RangerPolicy> tagPolicies = (servicePolicies.getTagPolicies() == null || CollectionUtils.isEmpty(servicePolicies.getTagPolicies().getPolicies())) ? new ArrayList<>() : servicePolicies.getTagPolicies().getPolicies();
							final List<RangerPolicy> newTagPolicies = RangerPolicyDeltaUtil.applyDeltas(tagPolicies, servicePoliciesFromDb.getPolicyDeltas(), servicePoliciesFromDb.getTagPolicies().getServiceDef().getName());

							servicePolicies.getTagPolicies().setPolicies(newTagPolicies);
							servicePolicies.getTagPolicies().setPolicyVersion(servicePoliciesFromDb.getTagPolicies().getPolicyVersion());

							checkCacheSanity(servicePoliciesFromDb.getTagPolicies().getServiceName(), serviceStore, true);

						} else {
							if (LOG.isDebugEnabled()) {
								LOG.debug("This service has no associated tag service");
							}
						}
					}
					this.deltaCache = null;
				} else {
					LOG.error("Could not get policies from database, from-version:[" + cachedServicePoliciesVersion + ")");
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug("ServicePolicies old-version:[" + cachedServicePoliciesVersion + "], new-version:[" + servicePolicies.getPolicyVersion() + "]");
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("ServicePolicies Cache already has the latest version, version:[" + servicePolicies.getPolicyVersion() + "]");
				}
			}

			if (LOG.isTraceEnabled()) {
				LOG.trace("Latest Cached ServicePolicies:[" + servicePolicies +"]");
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("<== ServicePoliciesWrapper.getLatest(serviceName=" + serviceName + ", lastKnownVersion=" + lastKnownVersion + ") : " + isCacheReloadedByDQEvent);
			}
			return isCacheReloadedByDQEvent;
		}

		private void checkCacheSanity(String serviceName, ServiceStore serviceStore, boolean isTagService) {
			final boolean result;
			Long dbPolicyVersion = serviceStore.getServicePolicyVersion(serviceName);
			Long cachedPolicyVersion = isTagService ? servicePolicies.getTagPolicies().getPolicyVersion() : servicePolicies.getPolicyVersion();

			result = Objects.equals(dbPolicyVersion, cachedPolicyVersion);

			if (!result && cachedPolicyVersion != null && dbPolicyVersion != null && cachedPolicyVersion < dbPolicyVersion) {
				LOG.info("checkCacheSanity(serviceName=" + serviceName + "): policy cache has a different version than one in the database. However, changes from " + cachedPolicyVersion + " to " + dbPolicyVersion + " will be downloaded in the next download. policyVersionInDB=" + dbPolicyVersion + ", policyVersionInCache=" + cachedPolicyVersion);
			}
		}

		private void pruneUnusedAttributes() {
			if (servicePolicies != null) {
				pruneUnusedPolicyAttributes(servicePolicies.getPolicies());
				if (servicePolicies.getTagPolicies() != null) {
					pruneUnusedPolicyAttributes(servicePolicies.getTagPolicies().getPolicies());
				}
			}
		}

		private void pruneUnusedPolicyAttributes(List<RangerPolicy> policies) {

			// Null out attributes not required by plug-ins
			if (CollectionUtils.isNotEmpty(policies)) {
				for (RangerPolicy policy : policies) {
					policy.setCreatedBy(null);
					policy.setCreateTime(null);
					policy.setUpdatedBy(null);
					policy.setUpdateTime(null);
					// policy.setGuid(null); /* this is used by import policy */
					// policy.setName(null); /* this is used by GUI in policy list page */
					// policy.setDescription(null); /* this is used by export policy */
					policy.setResourceSignature(null);
					policy.setOptions(null);
				}
			}
		}

		StringBuilder toString(StringBuilder sb) {
			sb.append("RangerServicePoliciesWrapper={");

			sb.append("updateTime=").append(updateTime)
					.append(", longestDbLoadTimeInMs=").append(longestDbLoadTimeInMs)
					.append(", Service-Version:").append(servicePolicies != null ? servicePolicies.getPolicyVersion() : "null")
					.append(", Number-Of-Policies:").append(servicePolicies != null && servicePolicies.getPolicies() != null ? servicePolicies.getPolicies().size() : 0)
					.append(", Number-Of-Policy-Deltas:").append(servicePolicies != null && servicePolicies.getPolicyDeltas() != null ? servicePolicies.getPolicyDeltas().size() : 0);

			sb.append("} ");

			return sb;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();

			toString(sb);

			return sb.toString();
		}
	}
}

