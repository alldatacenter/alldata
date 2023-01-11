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

package org.apache.ranger.biz;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.*;
import org.apache.ranger.plugin.model.*;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.service.RangerServiceResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class RangerTagDBRetriever {
	private static final Logger LOG = LoggerFactory.getLogger(RangerTagDBRetriever.class);
	private static final Logger PERF_LOG = RangerPerfTracer.getPerfLogger("db.RangerTagDBRetriever");

	public static final Type subsumedDataType = new TypeToken<List<RangerTagDef.RangerTagAttributeDef>>() {}.getType();

	public static final Gson gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
			.create();

	private final RangerDaoManager daoMgr;
	private final LookupCache lookupCache;

	private List<RangerServiceResource> serviceResources;
	private Map<Long, RangerTagDef> tagDefs;

	RangerTagDBRetriever(final RangerDaoManager daoMgr, final PlatformTransactionManager txManager, final XXService xService) {

		this.daoMgr = daoMgr;

		final TransactionTemplate txTemplate;

		if (txManager != null) {
			txTemplate = new TransactionTemplate(txManager);
			txTemplate.setReadOnly(true);
		} else {
			txTemplate = null;
		}
		this.lookupCache = new LookupCache();


		if (this.daoMgr != null && xService != null) {

			RangerPerfTracer perf = null;

			if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
				perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerTagDBRetriever.RangerTagDBRetriever(serviceName=" + xService.getName() + ")");
			}

			if (txTemplate == null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Load Tags in the same thread and using an existing transaction");
				}
				if (!initializeTagCache(xService)) {
					LOG.error("Failed to get tags for service:[" + xService.getName() + "] in the same thread and using an existing transaction");
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Load Tags in a separate thread and using a new transaction");
				}

				TagLoaderThread t = new TagLoaderThread(txTemplate, xService);
				t.setDaemon(true);
				t.start();
				try {
					t.join();
				} catch (InterruptedException ie) {
					LOG.error("Failed to get Tags in a separate thread and using a new transaction", ie);
				}
			}

			RangerPerfTracer.log(perf);

		}
	}

	List<RangerServiceResource> getServiceResources() {
		return serviceResources;
	}

	Map<Long, RangerTagDef> getTagDefs() {
		return tagDefs;
	}

	Map<Long, RangerTag> getTags() {

		Map<Long, RangerTag> ret = new HashMap<>();

		if (CollectionUtils.isNotEmpty(serviceResources)) {
			for (RangerServiceResource serviceResource : serviceResources) {
				List<RangerTag> tags = lookupCache.serviceResourceToTags.get(serviceResource.getId());
				if (CollectionUtils.isNotEmpty(tags)) {
					for (RangerTag tag : tags) {
						ret.put(tag.getId(), tag);
					}
				}
			}
		}

		return ret;
	}

	Map<Long, List<Long>> getResourceToTagIds() {
		Map<Long, List<Long>> ret = new HashMap<>();

		if (CollectionUtils.isNotEmpty(serviceResources)) {
			for (RangerServiceResource serviceResource : serviceResources) {
				List<RangerTag> tags = lookupCache.serviceResourceToTags.get(serviceResource.getId());
				if (CollectionUtils.isNotEmpty(tags)) {
					List<Long> tagIds = new ArrayList<>();
					ret.put(serviceResource.getId(), tagIds);
					for (RangerTag tag : tags) {
						tagIds.add(tag.getId());
					}
				}
			}
		}
		return ret;
	}

	private boolean initializeTagCache(XXService xService) {
		boolean ret;
		try {
			serviceResources = new TagRetrieverServiceResourceContext(xService).getAllServiceResources();
			tagDefs          = new TagRetrieverTagDefContext(xService).getAllTagDefs();

			ret = true;
		} catch (Exception ex) {
			LOG.error("Failed to get tags for service:[" + xService.getName() + "]", ex);
			serviceResources    = null;
			tagDefs             = null;
			ret = false;
		}
		return ret;
	}

	private class LookupCache {
		final Map<Long, String> userScreenNames = new HashMap<>();
		final Map<Long, List<RangerTag>> serviceResourceToTags = new HashMap<>();

		String getUserScreenName(Long userId) {
			String ret = null;

			if (userId != null) {
				ret = userScreenNames.get(userId);

				if (ret == null) {
					XXPortalUser user = daoMgr.getXXPortalUser().getById(userId);

					if (user != null) {
						ret = user.getPublicScreenName();

						if (StringUtil.isEmpty(ret)) {
							ret = user.getFirstName();

							if (StringUtil.isEmpty(ret)) {
								ret = user.getLoginId();
							} else {
								if (!StringUtil.isEmpty(user.getLastName())) {
									ret += (" " + user.getLastName());
								}
							}
						}

						if (ret != null) {
							userScreenNames.put(userId, ret);
						}
					}
				}
			}

			return ret;
		}

	}

	private class TagLoaderThread extends Thread {
		final TransactionTemplate txTemplate;
		final XXService           xService;

		TagLoaderThread(TransactionTemplate txTemplate, final XXService xService) {
			this.txTemplate = txTemplate;
			this.xService   = xService;
		}

		@Override
		public void run() {
			try {
				txTemplate.setReadOnly(true);
				Boolean result = txTemplate.execute(new TransactionCallback<Boolean>() {
					@Override
					public Boolean doInTransaction(TransactionStatus status) {
						boolean ret = initializeTagCache(xService);
						if (!ret) {
							status.setRollbackOnly();
							LOG.error("Failed to get tags for service:[" + xService.getName() + "] in a new transaction");
						}
						return ret;
					}
				});
				 if (LOG.isDebugEnabled()) {
				 	LOG.debug("transaction result:[" + result +"]");
				 }
			} catch (Throwable ex) {
				LOG.error("Failed to get tags for service:[" + xService.getName() + "] in a new transaction", ex);
			}
		}
	}

	private class TagRetrieverServiceResourceContext {

		final XXService service;
		final ListIterator<XXServiceResource> iterServiceResource;

		TagRetrieverServiceResourceContext(XXService xService) {
			Long serviceId = xService == null ? null : xService.getId();
			this.service = xService;

			List<XXServiceResource> xServiceResources = daoMgr.getXXServiceResource().findTaggedResourcesInServiceId(serviceId);

			this.iterServiceResource = xServiceResources.listIterator();

		}

		List<RangerServiceResource> getAllServiceResources() {
			List<RangerServiceResource> ret = new ArrayList<>();

			while (iterServiceResource.hasNext()) {
				RangerServiceResource serviceResource = getNextServiceResource();

				if (serviceResource != null) {
					ret.add(serviceResource);
				}
			}
			return ret;
		}

		RangerServiceResource getNextServiceResource() {
			RangerServiceResource ret = null;

			if (iterServiceResource.hasNext()) {
				XXServiceResource xServiceResource = iterServiceResource.next();

				iterServiceResource.remove();

				if (xServiceResource != null && StringUtils.isNotEmpty(xServiceResource.getTags())) {
					ret = new RangerServiceResource();

					ret.setId(xServiceResource.getId());
					ret.setGuid(xServiceResource.getGuid());
					ret.setIsEnabled(xServiceResource.getIsEnabled());
					ret.setCreatedBy(lookupCache.getUserScreenName(xServiceResource.getAddedByUserId()));
					ret.setUpdatedBy(lookupCache.getUserScreenName(xServiceResource.getUpdatedByUserId()));
					ret.setCreateTime(xServiceResource.getCreateTime());
					ret.setUpdateTime(xServiceResource.getUpdateTime());
					ret.setVersion(xServiceResource.getVersion());
					ret.setResourceSignature(xServiceResource.getResourceSignature());

					Map<String, RangerPolicy.RangerPolicyResource> serviceResourceElements = gsonBuilder.fromJson(xServiceResource.getServiceResourceElements(), RangerServiceResourceService.subsumedDataType);
					ret.setResourceElements(serviceResourceElements);

					List<RangerTag> tags = gsonBuilder.fromJson(xServiceResource.getTags(), RangerServiceResourceService.duplicatedDataType);
					lookupCache.serviceResourceToTags.put(xServiceResource.getId(), tags);
				}
			}

			return ret;
		}

	}

	private class TagRetrieverTagDefContext {

		final XXService service;
		final ListIterator<XXTagDef> iterTagDef;

		TagRetrieverTagDefContext(XXService xService) {
			Long serviceId = xService == null ? null : xService.getId();

			List<XXTagDef> xTagDefs = daoMgr.getXXTagDef().findByServiceId(serviceId);

			this.service = xService;
			this.iterTagDef = xTagDefs.listIterator();
		}

		Map<Long, RangerTagDef> getAllTagDefs() {
			Map<Long, RangerTagDef> ret = new HashMap<>();

			while (iterTagDef.hasNext()) {
				RangerTagDef tagDef = getNextTagDef();

				if (tagDef != null) {
					ret.put(tagDef.getId(), tagDef);
				}
			}
			return ret;
		}

		RangerTagDef getNextTagDef() {
			RangerTagDef ret = null;

			if (iterTagDef.hasNext()) {
				XXTagDef xTagDef = iterTagDef.next();

				iterTagDef.remove();

				if (xTagDef != null) {
					ret = new RangerTagDef();

					ret.setId(xTagDef.getId());
					ret.setGuid(xTagDef.getGuid());
					ret.setIsEnabled(xTagDef.getIsEnabled());
					ret.setCreatedBy(lookupCache.getUserScreenName(xTagDef.getAddedByUserId()));
					ret.setUpdatedBy(lookupCache.getUserScreenName(xTagDef.getUpdatedByUserId()));
					ret.setCreateTime(xTagDef.getCreateTime());
					ret.setUpdateTime(xTagDef.getUpdateTime());
					ret.setVersion(xTagDef.getVersion());
					ret.setName(xTagDef.getName());
					ret.setSource(xTagDef.getSource());
					List<RangerTagDef.RangerTagAttributeDef> attributeDefs = gsonBuilder.fromJson(xTagDef.getTagAttrDefs(), RangerTagDBRetriever.subsumedDataType);
					ret.setAttributeDefs(attributeDefs);
				}
			}

			return ret;
		}

	}

}
