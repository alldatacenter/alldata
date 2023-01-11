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
import org.apache.ranger.plugin.store.TagStore;

import org.apache.ranger.plugin.util.RangerServiceTagsDeltaUtil;
import org.apache.ranger.plugin.util.ServiceTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RangerServiceTagsCache {
	private static final Logger LOG = LoggerFactory.getLogger(RangerServiceTagsCache.class);

	private static final int MAX_WAIT_TIME_FOR_UPDATE = 10;

	private static volatile RangerServiceTagsCache sInstance = null;
	private final boolean useServiceTagsCache;
	private final int waitTimeInSeconds;

	private final Map<String, ServiceTagsWrapper> serviceTagsMap = new HashMap<>();

	public static RangerServiceTagsCache getInstance() {
		if (sInstance == null) {
			synchronized (RangerServiceTagsCache.class) {
				if (sInstance == null) {
					sInstance = new RangerServiceTagsCache();
				}
			}
		}
		return sInstance;
	}

	private RangerServiceTagsCache() {
		RangerAdminConfig config = RangerAdminConfig.getInstance();

		useServiceTagsCache = config.getBoolean("ranger.admin.tag.download.usecache", true);
		waitTimeInSeconds   = config.getInt("ranger.admin.tag.download.cache.max.waittime.for.update", MAX_WAIT_TIME_FOR_UPDATE);
	}

	public void dump() {

		if (useServiceTagsCache) {
			final Set<String> serviceNames;

			synchronized (this) {
				serviceNames = serviceTagsMap.keySet();
			}

			if (CollectionUtils.isNotEmpty(serviceNames)) {
				ServiceTagsWrapper cachedServiceTagsWrapper;

				for (String serviceName : serviceNames) {
					synchronized (this) {
						cachedServiceTagsWrapper = serviceTagsMap.get(serviceName);
					}
					LOG.debug("serviceName:" + serviceName + ", Cached-MetaData:" + cachedServiceTagsWrapper);
				}
			}
		}
	}

	public ServiceTags getServiceTags(String serviceName, Long serviceId, Long lastKnownVersion, boolean needsBackwardCompatibility, TagStore tagStore) throws Exception {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceTagsCache.getServiceTags(" + serviceName + ", " + serviceId + ", " + lastKnownVersion + ", " + needsBackwardCompatibility + ")");
		}

		ServiceTags ret = null;

		if (StringUtils.isNotBlank(serviceName) && serviceId != null) {

			if (LOG.isDebugEnabled()) {
				LOG.debug("useServiceTagsCache=" + useServiceTagsCache);
			}

			if (!useServiceTagsCache) {
				if (tagStore != null) {
					try {
						ret = tagStore.getServiceTags(serviceName, -1L);
					} catch (Exception exception) {
						LOG.error("getServiceTags(" + serviceName + "): failed to get latest tags from tag-store", exception);
					}
				} else {
					LOG.error("getServiceTags(" + serviceName + "): failed to get latest tags as tag-store is null!");
				}
			} else {
				ServiceTagsWrapper serviceTagsWrapper;

				synchronized (this) {
					serviceTagsWrapper = serviceTagsMap.get(serviceName);

					if (serviceTagsWrapper != null) {
						if (!serviceId.equals(serviceTagsWrapper.getServiceId())) {
							if (LOG.isDebugEnabled()) {
								LOG.debug("Service [" + serviceName + "] changed service-id from " + serviceTagsWrapper.getServiceId()
										+ " to " + serviceId);
								LOG.debug("Recreating serviceTagsWrapper for serviceName [" + serviceName + "]");
							}
							serviceTagsMap.remove(serviceName);
							serviceTagsWrapper = null;
						}
					}
					if (serviceTagsWrapper == null) {
						serviceTagsWrapper = new ServiceTagsWrapper(serviceId);
						serviceTagsMap.put(serviceName, serviceTagsWrapper);
					}
				}

				if (tagStore != null) {
					ret = serviceTagsWrapper.getLatestOrCached(serviceName, tagStore, lastKnownVersion, needsBackwardCompatibility);
				} else {
					LOG.error("getServiceTags(" + serviceName + "): failed to get latest tags as tag-store is null!");
					ret = serviceTagsWrapper.getServiceTags();
				}

			}
		} else {
			LOG.error("getServiceTags() failed to get tags as serviceName is null or blank and/or serviceId is null!");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceTagsCache.getServiceTags(" + serviceName + ", " + serviceId + ", " + lastKnownVersion + ", " + needsBackwardCompatibility + "): count=" + ((ret == null || ret.getTags() == null) ? 0 : ret.getTags().size()));
		}

		return ret;
	}

	private class ServiceTagsWrapper {
		final Long serviceId;
		ServiceTags serviceTags;
		Date updateTime = null;
		long longestDbLoadTimeInMs = -1;

		ServiceTagsDeltasCache deltaCache;

		class ServiceTagsDeltasCache {
			final long        		fromVersion;
			final ServiceTags 		serviceTagsDelta;

			ServiceTagsDeltasCache(final long fromVersion, ServiceTags serviceTagsDelta) {
				this.fromVersion         = fromVersion;
				this.serviceTagsDelta    = serviceTagsDelta;
			}
			ServiceTags getServiceTagsDeltaFromVersion(long fromVersion) {
				return this.fromVersion == fromVersion ? this.serviceTagsDelta : null;
			}
		}
		ReentrantLock lock = new ReentrantLock();

		ServiceTagsWrapper(Long serviceId) {
			this.serviceId = serviceId;
			serviceTags = null;
		}

		Long getServiceId() { return serviceId; }

		ServiceTags getServiceTags() {
			return serviceTags;
		}

		Date getUpdateTime() {
			return updateTime;
		}

		ServiceTags getLatestOrCached(String serviceName, TagStore tagStore, Long lastKnownVersion, boolean needsBackwardCompatibility) throws Exception {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> RangerServiceTagsCache.getLatestOrCached(lastKnownVersion=" + lastKnownVersion + ", " + needsBackwardCompatibility + ")");
			}
			ServiceTags	ret		   = null;
			boolean		lockResult = false;

			try {
				final boolean isCacheCompletelyLoaded;

				lockResult = lock.tryLock(waitTimeInSeconds, TimeUnit.SECONDS);
				if (lockResult) {

					isCacheCompletelyLoaded = getLatest(serviceName, tagStore);

					if (isCacheCompletelyLoaded) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("ServiceTags cache was completely loaded from database ");
						}
					}
					if (needsBackwardCompatibility || isCacheCompletelyLoaded
							|| lastKnownVersion == -1L || lastKnownVersion.equals(serviceTags.getTagVersion())) {
						// Looking for all tags, or Some disqualifying change encountered
						if (LOG.isDebugEnabled()) {
							LOG.debug("Need to return all cached ServiceTags: [needsBackwardCompatibility:" + needsBackwardCompatibility + ", isCacheCompletelyLoaded:" + isCacheCompletelyLoaded + ", lastKnownVersion:" + lastKnownVersion + ", serviceTagsVersion:" + serviceTags.getTagVersion() + "]");
						}
						ret = this.serviceTags;
					} else {
						boolean isDeltaCacheReinitialized = false;
						ServiceTags serviceTagsDelta = this.deltaCache != null ? this.deltaCache.getServiceTagsDeltaFromVersion(lastKnownVersion) : null;

						if (serviceTagsDelta == null) {
							serviceTagsDelta = tagStore.getServiceTagsDelta(serviceName, lastKnownVersion);
							isDeltaCacheReinitialized = true;
						}
						if (serviceTagsDelta != null) {
							if (LOG.isDebugEnabled()) {
								LOG.debug("Deltas were requested. Returning deltas from lastKnownVersion:[" + lastKnownVersion + "]");
							}
							if (isDeltaCacheReinitialized) {
								this.deltaCache = new ServiceTagsDeltasCache(lastKnownVersion, serviceTagsDelta);
							}
							ret = serviceTagsDelta;
						} else {
							LOG.warn("Deltas were requested, but could not get them!! lastKnownVersion:[" + lastKnownVersion + "]; Returning cached ServiceTags:[" + (serviceTags != null ? serviceTags.getTagVersion() : -1L) + "]");

							this.deltaCache = null;
							ret = this.serviceTags;
						}
					}
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Could not get lock in [" + waitTimeInSeconds + "] seconds, returning cached ServiceTags");
					}
					ret = this.serviceTags;
				}
			} catch (InterruptedException exception) {
				LOG.error("getLatestOrCached:lock got interrupted..", exception);
			} finally {
				if (lockResult) {
					lock.unlock();
				}
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RangerServiceTagsCache.getLatestOrCached(lastKnownVersion=" + lastKnownVersion + ", " + needsBackwardCompatibility + "): " + ret);
			}

			return ret;
		}

		boolean getLatest(String serviceName, TagStore tagStore) throws Exception {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> ServiceTagsWrapper.getLatest(" + serviceName + ")");
			}

			boolean isCacheCompletelyLoaded = false;

			final Long cachedServiceTagsVersion = serviceTags != null ? serviceTags.getTagVersion() : -1L;

			if (LOG.isDebugEnabled()) {
				LOG.debug("Found ServiceTags in-cache : " + (serviceTags != null));
			}

			Long tagVersionInDb = tagStore.getTagVersion(serviceName);

			if (serviceTags == null || tagVersionInDb == null || !tagVersionInDb.equals(cachedServiceTagsVersion)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("loading serviceTags from db ... cachedServiceTagsVersion=" + cachedServiceTagsVersion + ", tagVersionInDb=" + tagVersionInDb);
				}

				long startTimeMs = System.currentTimeMillis();

				ServiceTags serviceTagsFromDb = tagStore.getServiceTags(serviceName, cachedServiceTagsVersion);

				long dbLoadTime = System.currentTimeMillis() - startTimeMs;

				if (dbLoadTime > longestDbLoadTimeInMs) {
					longestDbLoadTimeInMs = dbLoadTime;
				}
				updateTime = new Date();

				if (serviceTagsFromDb != null) {
					if (serviceTags == null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Initializing ServiceTags cache for the first time");
						}
						serviceTags = serviceTagsFromDb;
						this.deltaCache = null;
						pruneUnusedAttributes();
						isCacheCompletelyLoaded = true;
					} else if (!serviceTagsFromDb.getIsDelta()) {
						// service-tags are loaded because of some disqualifying event
						if (LOG.isDebugEnabled()) {
							LOG.debug("Complete set of tag are loaded from database, because of some disqualifying event or because tag-delta is not supported");
						}
						serviceTags = serviceTagsFromDb;
						this.deltaCache = null;
						pruneUnusedAttributes();
						isCacheCompletelyLoaded = true;
					} else { // Previously cached service tags are still valid - no disqualifying change
						// Rebuild tags cache from original tags and deltas
						if (LOG.isDebugEnabled()) {
							LOG.debug("Retrieved tag-deltas from database. These will be applied on top of ServiceTags version:[" + cachedServiceTagsVersion + "], tag-deltas:[" + serviceTagsFromDb.getTagVersion() + "]");
						}
						RangerServiceTagsDeltaUtil.applyDelta(serviceTags, serviceTagsFromDb);
						this.deltaCache = new ServiceTagsDeltasCache(cachedServiceTagsVersion, serviceTagsFromDb);
					}
				} else {
					LOG.error("Could not get tags from database, from-version:[" + cachedServiceTagsVersion + ")");
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug("ServiceTags old-version:[" + cachedServiceTagsVersion + "], new-version:[" + serviceTags.getTagVersion() + "]");
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("ServiceTags Cache already has the latest version, version:[" + cachedServiceTagsVersion + "]");
				}
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("<== ServiceTagsWrapper.getLatest(" + serviceName + "): " + isCacheCompletelyLoaded);
			}

			return isCacheCompletelyLoaded;
		}

		private void pruneUnusedAttributes() {
			RangerServiceTagsDeltaUtil.pruneUnusedAttributes(this.serviceTags);
		}

		StringBuilder toString(StringBuilder sb) {
			sb.append("RangerServiceTagsWrapper={");

			sb.append("updateTime=").append(updateTime)
					.append(", longestDbLoadTimeInMs=").append(longestDbLoadTimeInMs)
					.append(", Service-Version:").append(serviceTags != null ? serviceTags.getTagVersion() : "null")
					.append(", Number-Of-Tags:").append(serviceTags != null ? serviceTags.getTags().size() : 0);

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

