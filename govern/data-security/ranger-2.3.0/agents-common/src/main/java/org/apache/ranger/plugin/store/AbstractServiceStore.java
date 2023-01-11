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

package org.apache.ranger.plugin.store;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.plugin.model.RangerBaseModelObject;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.services.tag.RangerServiceTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public abstract class AbstractServiceStore implements ServiceStore {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractServiceStore.class);

	public static final String COMPONENT_ACCESSTYPE_SEPARATOR = ":";

	public static final String AUTOPROPAGATE_ROWFILTERDEF_TO_TAG_PROP = "ranger.servicedef.autopropagate.rowfilterdef.to.tag";

	public static final boolean AUTOPROPAGATE_ROWFILTERDEF_TO_TAG_PROP_DEFAULT = false;

	private static final int MAX_ACCESS_TYPES_IN_SERVICE_DEF = 1000;

	private final RangerAdminConfig config;

	// when a service-def is updated, the updated service-def should be made available to plugins
	//   this is achieved by incrementing policyVersion of all its services
	protected abstract void updateServicesForServiceDefUpdate(RangerServiceDef serviceDef) throws Exception;

	protected AbstractServiceStore() {
		this.config = RangerAdminConfig.getInstance();
	}

	@Override
	public void updateTagServiceDefForAccessTypes() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDefDBStore.updateTagServiceDefForAccessTypes()");
		}
		List<RangerServiceDef> allServiceDefs = getServiceDefs(new SearchFilter());
		for (RangerServiceDef serviceDef : allServiceDefs) {
			updateTagServiceDefForUpdatingAccessTypes(serviceDef);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDefDBStore.updateTagServiceDefForAccessTypes()");
		}
	}

	@Override
	public PList<RangerServiceDef> getPaginatedServiceDefs(SearchFilter filter) throws Exception {
		List<RangerServiceDef> resultList = getServiceDefs(filter);

		return CollectionUtils.isEmpty(resultList) ? new PList<RangerServiceDef>() : new PList<RangerServiceDef>(resultList, 0, resultList.size(),
				(long) resultList.size(), resultList.size(), filter.getSortType(), filter.getSortBy());
	}

	@Override
	public PList<RangerService> getPaginatedServices(SearchFilter filter) throws Exception {
		List<RangerService> resultList = getServices(filter);

		return CollectionUtils.isEmpty(resultList) ? new PList<RangerService>() : new PList<RangerService>(resultList, 0, resultList.size(), (long) resultList.size(),
				resultList.size(), filter.getSortType(), filter.getSortBy());
	}

	@Override
	public PList<RangerPolicy> getPaginatedPolicies(SearchFilter filter) throws Exception {
		List<RangerPolicy> resultList = getPolicies(filter);

		return CollectionUtils.isEmpty(resultList) ? new PList<RangerPolicy>() : new PList<RangerPolicy>(resultList, 0, resultList.size(), (long) resultList.size(),
				resultList.size(), filter.getSortType(), filter.getSortBy());
	}

	@Override
	public PList<RangerPolicy> getPaginatedServicePolicies(Long serviceId, SearchFilter filter) throws Exception {
		List<RangerPolicy> resultList = getServicePolicies(serviceId, filter);

		return CollectionUtils.isEmpty(resultList) ? new PList<RangerPolicy>() : new PList<RangerPolicy>(resultList, 0, resultList.size(), (long) resultList.size(),
				resultList.size(), filter.getSortType(), filter.getSortBy());
	}

	@Override
	public PList<RangerPolicy> getPaginatedServicePolicies(String serviceName, SearchFilter filter) throws Exception {
		List<RangerPolicy> resultList = getServicePolicies(serviceName, filter);

		return CollectionUtils.isEmpty(resultList) ? new PList<RangerPolicy>() : new PList<RangerPolicy>(resultList, 0, resultList.size(), (long) resultList.size(),
				resultList.size(), filter.getSortType(), filter.getSortBy());
	}

	@Override
	public Long getServicePolicyVersion(String serviceName) {
		RangerService service = null;
		try {
			service = getServiceByName(serviceName);
		} catch (Exception exception) {
			LOG.error("Failed to get service object for service:" + serviceName);
		}
		return service != null ? service.getPolicyVersion() : null;
	}

	protected void postCreate(RangerBaseModelObject obj) throws Exception {
		if (obj instanceof RangerServiceDef) {
			updateTagServiceDefForUpdatingAccessTypes((RangerServiceDef) obj);
		}
	}

	protected void postUpdate(RangerBaseModelObject obj) throws Exception {
		if (obj instanceof RangerServiceDef) {
			RangerServiceDef serviceDef = (RangerServiceDef) obj;

			updateTagServiceDefForUpdatingAccessTypes(serviceDef);
			updateServicesForServiceDefUpdate(serviceDef);
		}
	}

	protected void postDelete(RangerBaseModelObject obj) throws Exception {
		if (obj instanceof RangerServiceDef) {
			updateTagServiceDefForDeletingAccessTypes(((RangerServiceDef) obj).getName());
		}
	}

	public static long getNextVersion(Long currentVersion) {
		return currentVersion == null ? 1L : currentVersion + 1;
	}

	private RangerServiceDef.RangerAccessTypeDef findAccessTypeDef(long itemId, List<RangerServiceDef.RangerAccessTypeDef> accessTypeDefs) {
		RangerServiceDef.RangerAccessTypeDef ret = null;

		for (RangerServiceDef.RangerAccessTypeDef accessTypeDef : accessTypeDefs) {
			if (itemId == accessTypeDef.getItemId()) {
				ret = accessTypeDef;
				break;
			}
		}
		return ret;
	}

	private boolean updateTagAccessTypeDef(RangerServiceDef.RangerAccessTypeDef tagAccessType, RangerServiceDef.RangerAccessTypeDef svcAccessType, String prefix) {

		boolean isUpdated = false;

		if (!Objects.equals(tagAccessType.getName().substring(prefix.length()), svcAccessType.getName())) {
			isUpdated = true;
		} else if (!Objects.equals(tagAccessType.getLabel(), svcAccessType.getLabel())) {
			isUpdated = true;
		} else if (!Objects.equals(tagAccessType.getRbKeyLabel(), svcAccessType.getRbKeyLabel())) {
			isUpdated = true;
		} else {
			Collection<String> tagImpliedGrants = tagAccessType.getImpliedGrants();
			Collection<String> svcImpliedGrants = svcAccessType.getImpliedGrants();

			int tagImpliedGrantsLen = tagImpliedGrants == null ? 0 : tagImpliedGrants.size();
			int svcImpliedGrantsLen = svcImpliedGrants == null ? 0 : svcImpliedGrants.size();

			if (tagImpliedGrantsLen != svcImpliedGrantsLen) {
				isUpdated = true;
			} else if (tagImpliedGrantsLen > 0) {
				for (String svcImpliedGrant : svcImpliedGrants) {
					if (!tagImpliedGrants.contains(prefix + svcImpliedGrant)) {
						isUpdated = true;
						break;
					}
				}
			}
		}

		if (isUpdated) {
			tagAccessType.setName(prefix + svcAccessType.getName());
			tagAccessType.setLabel(svcAccessType.getLabel());
			tagAccessType.setRbKeyLabel(svcAccessType.getRbKeyLabel());

			tagAccessType.setImpliedGrants(new HashSet<String>());
			if (CollectionUtils.isNotEmpty(svcAccessType.getImpliedGrants())) {
				for (String svcImpliedGrant : svcAccessType.getImpliedGrants()) {
					tagAccessType.getImpliedGrants().add(prefix + svcImpliedGrant);
				}
			}
		}
		return isUpdated;
	}

	private boolean updateTagAccessTypeDefs(List<RangerServiceDef.RangerAccessTypeDef> svcDefAccessTypes, List<RangerServiceDef.RangerAccessTypeDef> tagDefAccessTypes,
										 long itemIdOffset, String prefix) {

		List<RangerServiceDef.RangerAccessTypeDef> toAdd    = new ArrayList<>();
		List<RangerServiceDef.RangerAccessTypeDef> toUpdate = new ArrayList<>();
		List<RangerServiceDef.RangerAccessTypeDef> toDelete = new ArrayList<>();

		for (RangerServiceDef.RangerAccessTypeDef svcAccessType : svcDefAccessTypes) {
			long tagAccessTypeItemId = svcAccessType.getItemId() + itemIdOffset;

			RangerServiceDef.RangerAccessTypeDef tagAccessType = findAccessTypeDef(tagAccessTypeItemId, tagDefAccessTypes);

			if (tagAccessType == null) {
				tagAccessType = new RangerServiceDef.RangerAccessTypeDef();

				tagAccessType.setItemId(tagAccessTypeItemId);
				tagAccessType.setName(prefix + svcAccessType.getName());
				tagAccessType.setLabel(svcAccessType.getLabel());
				tagAccessType.setRbKeyLabel(svcAccessType.getRbKeyLabel());

				tagAccessType.setImpliedGrants(new HashSet<String>());
				if (CollectionUtils.isNotEmpty(svcAccessType.getImpliedGrants())) {
					for (String svcImpliedGrant : svcAccessType.getImpliedGrants()) {
						tagAccessType.getImpliedGrants().add(prefix + svcImpliedGrant);
					}
				}

				toAdd.add(tagAccessType);
			}
		}


		for (RangerServiceDef.RangerAccessTypeDef tagAccessType : tagDefAccessTypes) {
			if (tagAccessType.getName().startsWith(prefix)) {
				long svcAccessTypeItemId = tagAccessType.getItemId() - itemIdOffset;

				RangerServiceDef.RangerAccessTypeDef svcAccessType = findAccessTypeDef(svcAccessTypeItemId, svcDefAccessTypes);

				if (svcAccessType == null) { // accessType has been deleted in service
					toDelete.add(tagAccessType);
				} else if (updateTagAccessTypeDef(tagAccessType, svcAccessType, prefix)) {
					toUpdate.add(tagAccessType);
				}
			}
		}

		boolean updateNeeded = false;

		if (CollectionUtils.isNotEmpty(toAdd) || CollectionUtils.isNotEmpty(toUpdate) || CollectionUtils.isNotEmpty(toDelete)) {
			if (LOG.isDebugEnabled()) {
				for (RangerServiceDef.RangerAccessTypeDef accessTypeDef : toDelete) {
					LOG.debug("accessTypeDef-to-delete:[" + accessTypeDef + "]");
				}

				for (RangerServiceDef.RangerAccessTypeDef accessTypeDef : toUpdate) {
					LOG.debug("accessTypeDef-to-update:[" + accessTypeDef + "]");
				}
				for (RangerServiceDef.RangerAccessTypeDef accessTypeDef : toAdd) {
					LOG.debug("accessTypeDef-to-add:[" + accessTypeDef + "]");
				}
			}

			tagDefAccessTypes.addAll(toAdd);
			tagDefAccessTypes.removeAll(toDelete);

			updateNeeded = true;
		}
		return updateNeeded;
	}

	private void updateTagServiceDefForUpdatingAccessTypes(RangerServiceDef serviceDef) throws Exception {
		if (StringUtils.equals(serviceDef.getName(), EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
			return;
		}

		if (EmbeddedServiceDefsUtil.instance().getTagServiceDefId() == -1) {
			LOG.info("AbstractServiceStore.updateTagServiceDefForUpdatingAccessTypes(" + serviceDef.getName() + "): tag service-def does not exist");
		}

		RangerServiceDef tagServiceDef;
		try {
			tagServiceDef = this.getServiceDef(EmbeddedServiceDefsUtil.instance().getTagServiceDefId());
		} catch (Exception e) {
			LOG.error("AbstractServiceStore.updateTagServiceDefForUpdatingAccessTypes" + serviceDef.getName() + "): could not find TAG ServiceDef.. ", e);
			throw e;
		}

		if (tagServiceDef == null) {
			LOG.error("AbstractServiceStore.updateTagServiceDefForUpdatingAccessTypes(" + serviceDef.getName() + "): could not find TAG ServiceDef.. ");

			return;
		}

		String serviceDefName = serviceDef.getName();
		String prefix = serviceDefName + COMPONENT_ACCESSTYPE_SEPARATOR;

		List<RangerServiceDef.RangerAccessTypeDef> svcDefAccessTypes = serviceDef.getAccessTypes();
		List<RangerServiceDef.RangerAccessTypeDef> tagDefAccessTypes = tagServiceDef.getAccessTypes();

		long itemIdOffset = serviceDef.getId() * (MAX_ACCESS_TYPES_IN_SERVICE_DEF + 1);

		boolean updateNeeded = updateTagAccessTypeDefs(svcDefAccessTypes, tagDefAccessTypes, itemIdOffset, prefix);

		if (updateTagServiceDefForUpdatingDataMaskDef(tagServiceDef, serviceDef, itemIdOffset, prefix)) {
			updateNeeded = true;
		}

		if (updateTagServiceDefForUpdatingRowFilterDef(tagServiceDef, serviceDef, itemIdOffset, prefix)) {
			updateNeeded = true;
		}

		boolean resourceUpdated = updateResourceInTagServiceDef(tagServiceDef);

		updateNeeded = updateNeeded || resourceUpdated;

		if (updateNeeded) {
			try {
				updateServiceDef(tagServiceDef);
				LOG.info("AbstractServiceStore.updateTagServiceDefForUpdatingAccessTypes -- updated TAG service def with " + serviceDefName + " access types");
			} catch (Exception e) {
				LOG.error("AbstractServiceStore.updateTagServiceDefForUpdatingAccessTypes -- Failed to update TAG ServiceDef.. ", e);
				throw e;
			}
		}
	}

	private void updateTagServiceDefForDeletingAccessTypes(String serviceDefName) throws Exception {
		if (EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME.equals(serviceDefName)) {
			return;
		}

		RangerServiceDef tagServiceDef;
		try {
			tagServiceDef = this.getServiceDef(EmbeddedServiceDefsUtil.instance().getTagServiceDefId());
		} catch (Exception e) {
			LOG.error("AbstractServiceStore.updateTagServiceDefForDeletingAccessTypes(" + serviceDefName + "): could not find TAG ServiceDef.. ", e);
			throw e;
		}

		if (tagServiceDef == null) {
			LOG.error("AbstractServiceStore.updateTagServiceDefForDeletingAccessTypes(" + serviceDefName + "): could not find TAG ServiceDef.. ");

			return;
		}

		List<RangerServiceDef.RangerAccessTypeDef> accessTypes = new ArrayList<>();

		for (RangerServiceDef.RangerAccessTypeDef accessType : tagServiceDef.getAccessTypes()) {
			if (accessType.getName().startsWith(serviceDefName + COMPONENT_ACCESSTYPE_SEPARATOR)) {
				accessTypes.add(accessType);
			}
		}

		tagServiceDef.getAccessTypes().removeAll(accessTypes);

		updateTagServiceDefForDeletingDataMaskDef(tagServiceDef, serviceDefName);

		updateTagServiceDefForDeletingRowFilterDef(tagServiceDef, serviceDefName);

		updateResourceInTagServiceDef(tagServiceDef);

		try {
			updateServiceDef(tagServiceDef);
			LOG.info("AbstractServiceStore.updateTagServiceDefForDeletingAccessTypes -- updated TAG service def with " + serviceDefName + " access types");
		} catch (Exception e) {
			LOG.error("AbstractServiceStore.updateTagServiceDefForDeletingAccessTypes -- Failed to update TAG ServiceDef.. ", e);
			throw e;
		}
	}

	private boolean updateTagServiceDefForUpdatingDataMaskDef(RangerServiceDef tagServiceDef, RangerServiceDef serviceDef, long itemIdOffset, String prefix) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AbstractServiceStore.updateTagServiceDefForUpdatingDataMaskDef(" + serviceDef.getName() + ")");
		}
		boolean ret = false;

		RangerServiceDef.RangerDataMaskDef svcDataMaskDef = serviceDef.getDataMaskDef();
		RangerServiceDef.RangerDataMaskDef tagDataMaskDef = tagServiceDef.getDataMaskDef();

		List<RangerServiceDef.RangerDataMaskTypeDef> svcDefMaskTypes = svcDataMaskDef.getMaskTypes();
		List<RangerServiceDef.RangerDataMaskTypeDef> tagDefMaskTypes = tagDataMaskDef.getMaskTypes();

		List<RangerServiceDef.RangerAccessTypeDef> svcDefAccessTypes = svcDataMaskDef.getAccessTypes();
		List<RangerServiceDef.RangerAccessTypeDef> tagDefAccessTypes = tagDataMaskDef.getAccessTypes();

		List<RangerServiceDef.RangerDataMaskTypeDef> maskTypesToAdd = new ArrayList<>();
		List<RangerServiceDef.RangerDataMaskTypeDef> maskTypesToUpdate = new ArrayList<>();
		List<RangerServiceDef.RangerDataMaskTypeDef> maskTypesToDelete = new ArrayList<>();

		for (RangerServiceDef.RangerDataMaskTypeDef svcMaskType : svcDefMaskTypes) {
			long tagMaskTypeItemId = itemIdOffset + svcMaskType.getItemId();
			RangerServiceDef.RangerDataMaskTypeDef foundTagMaskType = null;
			for (RangerServiceDef.RangerDataMaskTypeDef tagMaskType : tagDefMaskTypes) {
				if (tagMaskType.getItemId().equals(tagMaskTypeItemId)) {
					foundTagMaskType = tagMaskType;
					break;
				}
			}
			if (foundTagMaskType == null) {
				RangerServiceDef.RangerDataMaskTypeDef tagMaskType = new RangerServiceDef.RangerDataMaskTypeDef(svcMaskType);
				tagMaskType.setName(prefix + svcMaskType.getName());
				tagMaskType.setItemId(itemIdOffset + svcMaskType.getItemId());
				tagMaskType.setLabel(svcMaskType.getLabel());
				tagMaskType.setRbKeyLabel(svcMaskType.getRbKeyLabel());
				maskTypesToAdd.add(tagMaskType);
			}
		}

		for (RangerServiceDef.RangerDataMaskTypeDef tagMaskType : tagDefMaskTypes) {
			if (StringUtils.startsWith(tagMaskType.getName(), prefix)) {

				RangerServiceDef.RangerDataMaskTypeDef foundSvcMaskType = null;
				for (RangerServiceDef.RangerDataMaskTypeDef svcMaskType : svcDefMaskTypes) {
					long tagMaskTypeItemId = itemIdOffset + svcMaskType.getItemId();
					if (tagMaskType.getItemId().equals(tagMaskTypeItemId)) {
						foundSvcMaskType = svcMaskType;
						break;
					}
				}
				if (foundSvcMaskType == null) {
					maskTypesToDelete.add(tagMaskType);
					continue;
				}

				RangerServiceDef.RangerDataMaskTypeDef checkTagMaskType = new RangerServiceDef.RangerDataMaskTypeDef(foundSvcMaskType);

				checkTagMaskType.setName(prefix + foundSvcMaskType.getName());
				checkTagMaskType.setItemId(itemIdOffset + foundSvcMaskType.getItemId());

				if (!checkTagMaskType.equals(tagMaskType)) {
					tagMaskType.setLabel(checkTagMaskType.getLabel());
					tagMaskType.setDescription(checkTagMaskType.getDescription());
					tagMaskType.setTransformer(checkTagMaskType.getTransformer());
					tagMaskType.setDataMaskOptions(checkTagMaskType.getDataMaskOptions());
					tagMaskType.setRbKeyLabel(checkTagMaskType.getRbKeyLabel());
					tagMaskType.setRbKeyDescription(checkTagMaskType.getRbKeyDescription());
					maskTypesToUpdate.add(tagMaskType);
				}
			}
		}

		if (CollectionUtils.isNotEmpty(maskTypesToAdd) || CollectionUtils.isNotEmpty(maskTypesToUpdate) || CollectionUtils.isNotEmpty(maskTypesToDelete)) {
			ret = true;

			if (LOG.isDebugEnabled()) {
				for (RangerServiceDef.RangerDataMaskTypeDef maskTypeDef : maskTypesToDelete) {
					LOG.debug("maskTypeDef-to-delete:[" + maskTypeDef + "]");
				}

				for (RangerServiceDef.RangerDataMaskTypeDef maskTypeDef : maskTypesToUpdate) {
					LOG.debug("maskTypeDef-to-update:[" + maskTypeDef + "]");
				}

				for (RangerServiceDef.RangerDataMaskTypeDef maskTypeDef : maskTypesToAdd) {
					LOG.debug("maskTypeDef-to-add:[" + maskTypeDef + "]");
				}
			}

			tagDefMaskTypes.removeAll(maskTypesToDelete);
			tagDefMaskTypes.addAll(maskTypesToAdd);

			tagDataMaskDef.setMaskTypes(tagDefMaskTypes);
		}

		boolean tagMaskDefAccessTypesUpdated = updateTagAccessTypeDefs(svcDefAccessTypes, tagDefAccessTypes, itemIdOffset, prefix);

		if (tagMaskDefAccessTypesUpdated) {
			tagDataMaskDef.setAccessTypes(tagDefAccessTypes);
			ret = true;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AbstractServiceStore.updateTagServiceDefForUpdatingDataMaskDef(" + serviceDef.getName() + ") : " + ret);
		}

		return ret;
	}

	private void updateTagServiceDefForDeletingDataMaskDef(RangerServiceDef tagServiceDef, String serviceDefName) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AbstractServiceStore.updateTagServiceDefForDeletingDataMaskDef(" + serviceDefName + ")");
		}
		RangerServiceDef.RangerDataMaskDef tagDataMaskDef = tagServiceDef.getDataMaskDef();

		if (tagDataMaskDef == null) {
			return;
		}

		String prefix = serviceDefName + COMPONENT_ACCESSTYPE_SEPARATOR;

		List<RangerServiceDef.RangerAccessTypeDef> accessTypes = new ArrayList<>();

		for (RangerServiceDef.RangerAccessTypeDef accessType : tagDataMaskDef.getAccessTypes()) {
			if (accessType.getName().startsWith(prefix)) {
				accessTypes.add(accessType);
			}
		}
		List<RangerServiceDef.RangerDataMaskTypeDef> maskTypes = new ArrayList<>();
		for (RangerServiceDef.RangerDataMaskTypeDef maskType : tagDataMaskDef.getMaskTypes()) {
			if (maskType.getName().startsWith(prefix)) {
				maskTypes.add(maskType);
			}
		}
		tagDataMaskDef.getAccessTypes().removeAll(accessTypes);
		tagDataMaskDef.getMaskTypes().removeAll(maskTypes);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AbstractServiceStore.updateTagServiceDefForDeletingDataMaskDef(" + serviceDefName + ")");
		}
	}

	private boolean updateTagServiceDefForUpdatingRowFilterDef(RangerServiceDef tagServiceDef, RangerServiceDef serviceDef, long itemIdOffset, String prefix) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AbstractServiceStore.updateTagServiceDefForUpdatingRowFilterDef(" + serviceDef.getName() + ")");
		}
		boolean ret = false;

		boolean autopropagateRowfilterdefToTag = config.getBoolean(AUTOPROPAGATE_ROWFILTERDEF_TO_TAG_PROP, AUTOPROPAGATE_ROWFILTERDEF_TO_TAG_PROP_DEFAULT);

		if (autopropagateRowfilterdefToTag) {
			RangerServiceDef.RangerRowFilterDef svcRowFilterDef = serviceDef.getRowFilterDef();
			RangerServiceDef.RangerRowFilterDef tagRowFilterDef = tagServiceDef.getRowFilterDef();

			List<RangerServiceDef.RangerAccessTypeDef> svcDefAccessTypes = svcRowFilterDef.getAccessTypes();
			List<RangerServiceDef.RangerAccessTypeDef> tagDefAccessTypes = tagRowFilterDef.getAccessTypes();

			boolean tagRowFilterAccessTypesUpdated = updateTagAccessTypeDefs(svcDefAccessTypes, tagDefAccessTypes, itemIdOffset, prefix);

			if (tagRowFilterAccessTypesUpdated) {
				tagRowFilterDef.setAccessTypes(tagDefAccessTypes);
				ret = true;
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AbstractServiceStore.updateTagServiceDefForUpdatingRowFilterDef(" + serviceDef.getName() + ") : " + ret);
		}

		return ret;
	}

	private void updateTagServiceDefForDeletingRowFilterDef(RangerServiceDef tagServiceDef, String serviceDefName) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AbstractServiceStore.updateTagServiceDefForDeletingRowFilterDef(" + serviceDefName + ")");
		}
		RangerServiceDef.RangerRowFilterDef tagRowFilterDef = tagServiceDef.getRowFilterDef();

		if (tagRowFilterDef == null) {
			return;
		}

		String prefix = serviceDefName + COMPONENT_ACCESSTYPE_SEPARATOR;

		List<RangerServiceDef.RangerAccessTypeDef> accessTypes = new ArrayList<>();

		for (RangerServiceDef.RangerAccessTypeDef accessType : tagRowFilterDef.getAccessTypes()) {
			if (accessType.getName().startsWith(prefix)) {
				accessTypes.add(accessType);
			}
		}

		tagRowFilterDef.getAccessTypes().removeAll(accessTypes);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AbstractServiceStore.updateTagServiceDefForDeletingRowFilterDef(" + serviceDefName + ")");
		}
	}

	private boolean updateResourceInTagServiceDef(RangerServiceDef tagServiceDef) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AbstractServiceStore.updateResourceInTagServiceDef(" + tagServiceDef + ")");
		}
		boolean ret = false;

		final RangerServiceDef.RangerResourceDef accessPolicyTagResource = getResourceDefForTagResource(tagServiceDef.getResources());

		final List<RangerServiceDef.RangerResourceDef> resources = new ArrayList<>();

		if (accessPolicyTagResource == null) {
			LOG.warn("Resource with name :[" + RangerServiceTag.TAG_RESOURCE_NAME + "] not found in  tag-service-definition!!");
		} else {
			resources.add(accessPolicyTagResource);
		}

		RangerServiceDef.RangerDataMaskDef dataMaskDef = tagServiceDef.getDataMaskDef();

		if (dataMaskDef != null) {
			if (CollectionUtils.isNotEmpty(dataMaskDef.getAccessTypes())) {
				if (CollectionUtils.isEmpty(dataMaskDef.getResources())) {
					dataMaskDef.setResources(resources);
					ret = true;
				}
			} else {
				if (CollectionUtils.isNotEmpty(dataMaskDef.getResources())) {
					dataMaskDef.setResources(null);
					ret = true;
				}
			}
		}

		RangerServiceDef.RangerRowFilterDef rowFilterDef = tagServiceDef.getRowFilterDef();

		if (rowFilterDef != null) {
			boolean autopropagateRowfilterdefToTag = config.getBoolean(AUTOPROPAGATE_ROWFILTERDEF_TO_TAG_PROP, AUTOPROPAGATE_ROWFILTERDEF_TO_TAG_PROP_DEFAULT);
			if (autopropagateRowfilterdefToTag) {
				if (CollectionUtils.isNotEmpty(rowFilterDef.getAccessTypes())) {
					if (CollectionUtils.isEmpty(rowFilterDef.getResources())) {
						rowFilterDef.setResources(resources);
						ret = true;
					}
				} else {
					if (CollectionUtils.isNotEmpty(rowFilterDef.getResources())) {
						rowFilterDef.setResources(null);
						ret = true;
					}
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AbstractServiceStore.updateResourceInTagServiceDef(" + tagServiceDef + ") : " + ret);
		}
		return ret;
	}

	private RangerServiceDef.RangerResourceDef getResourceDefForTagResource(List<RangerServiceDef.RangerResourceDef> resourceDefs) {
		RangerServiceDef.RangerResourceDef ret = null;

		if (CollectionUtils.isNotEmpty(resourceDefs)) {
			for (RangerServiceDef.RangerResourceDef resourceDef : resourceDefs) {
				if (resourceDef.getName().equals(RangerServiceTag.TAG_RESOURCE_NAME)) {
					ret = resourceDef;
					break;
				}
			}
		}
		return ret;
	}
}
