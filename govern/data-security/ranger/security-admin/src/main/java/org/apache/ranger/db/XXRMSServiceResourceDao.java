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

package org.apache.ranger.db;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.NoResultException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXRMSServiceResource;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.store.StoredServiceResource;
import org.apache.ranger.plugin.util.JsonUtilsV2;
import org.springframework.stereotype.Service;

@Service
public class XXRMSServiceResourceDao extends BaseDao<XXRMSServiceResource> {

	private static RangerDaoManagerBase _daoManager = null;

	public XXRMSServiceResourceDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
		_daoManager = daoManager;
	}

	public XXRMSServiceResource findByGuid(String guid) {
		if (StringUtil.isEmpty(guid)) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXRMSServiceResource.findByGuid", tClass)
					.setParameter("guid", guid).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<RangerServiceResource> findByServiceId(Long serviceId) {
		List<RangerServiceResource> ret = new ArrayList<>();

		if (serviceId != null) {
			List<Object[]> rows = null;
			try {
				rows = getEntityManager()
						.createNamedQuery("XXRMSServiceResource.findByServiceId", Object[].class)
						.setParameter("serviceId", serviceId).getResultList();
			} catch (NoResultException e) {
				// Nothing
			}

			if (CollectionUtils.isNotEmpty(rows)) {
				for (Object[] row : rows) {
					XXRMSServiceResource xxServiceResource = new XXRMSServiceResource();
					xxServiceResource.setId((Long) row[0]);
					xxServiceResource.setGuid((String) row[1]);
					xxServiceResource.setVersion((Long) row[2]);
					xxServiceResource.setIsEnabled((Boolean) row[3]);
					xxServiceResource.setResourceSignature((String) row[4]);
					xxServiceResource.setServiceId((Long) row[5]);
					xxServiceResource.setServiceResourceElements((String) row[6]);
					ret.add(XXRMSServiceResourceDao.populateViewBean(xxServiceResource));
				}
			}
		}
		return ret;
	}

	public XXRMSServiceResource findByServiceAndResourceSignature(Long serviceId, String resourceSignature) {
		if (StringUtils.isBlank(resourceSignature)) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXRMSServiceResource.findByServiceAndResourceSignature", tClass)
					.setParameter("serviceId", serviceId).setParameter("resourceSignature", resourceSignature)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public RangerServiceResource getServiceResourceByServiceAndResourceSignature(String serviceName, String resourceSignature) {
		RangerServiceResource ret = null;

		if (StringUtils.isNotBlank(resourceSignature)) {
			Long serviceId = daoManager.getXXService().findIdByName(serviceName);

			if (serviceId != null) {
				try {
					XXRMSServiceResource xxServiceResource = getEntityManager().createNamedQuery("XXRMSServiceResource.findByServiceAndResourceSignature", tClass)
							.setParameter("serviceId", serviceId).setParameter("resourceSignature", resourceSignature)
							.getSingleResult();
					ret = populateViewBean(xxServiceResource);

				} catch (NoResultException e) {
					return null;
				}
			}
		}

		return ret;
	}

	public static RangerServiceResource populateViewBean(XXRMSServiceResource xxServiceResource) {

		RangerServiceResource ret = null;

		XXService service = _daoManager == null ? null : _daoManager.getXXService().getById(xxServiceResource.getServiceId());

		if (service != null) {
			ret = new RangerServiceResource();
			ret.setId(xxServiceResource.getId());
			ret.setCreateTime(xxServiceResource.getCreateTime());
			ret.setUpdateTime(xxServiceResource.getUpdateTime());
			ret.setGuid(xxServiceResource.getGuid());
			ret.setResourceSignature(xxServiceResource.getResourceSignature());

			ret.setServiceName(service.getName());

			if (StringUtils.isNotEmpty(xxServiceResource.getServiceResourceElements())) {
				try {
					StoredServiceResource storedServiceResource = JsonUtilsV2.jsonToObj(xxServiceResource.getServiceResourceElements(), StoredServiceResource.class);
					ret.setResourceElements(storedServiceResource.getResourceElements());
					ret.setOwnerUser(storedServiceResource.getOwnerName());
					ret.setAdditionalInfo(storedServiceResource.getAdditionalInfo());
				} catch (Exception e){
					ret = null;
				}
			} else {
				ret = null;
			}
		}

		return ret;
	}

	public XXRMSServiceResource populateEntityBean(RangerServiceResource serviceResource) {

		XXRMSServiceResource ret = new XXRMSServiceResource();

		ret.setId(serviceResource.getId());
		ret.setCreateTime(serviceResource.getCreateTime() != null ? serviceResource.getCreateTime() : DateUtil.getUTCDate());
		ret.setUpdateTime(serviceResource.getUpdateTime() != null ? serviceResource.getUpdateTime() : DateUtil.getUTCDate());
		ret.setAddedByUserId(0L);
		ret.setUpdatedByUserId(0L);

		String guid = (StringUtils.isEmpty(serviceResource.getGuid())) ?  new GUIDUtil().genGUID() : serviceResource.getGuid();

		ret.setGuid(guid);
		ret.setVersion(serviceResource.getVersion());
		ret.setIsEnabled(serviceResource.getIsEnabled());
		ret.setResourceSignature(serviceResource.getResourceSignature());

		Long serviceId = daoManager.getXXService().findIdByName(serviceResource.getServiceName());

		if (serviceId != null) {
			ret.setServiceId(serviceId);

			StoredServiceResource storedServiceResource = new StoredServiceResource(serviceResource.getResourceElements(), serviceResource.getOwnerUser(), serviceResource.getAdditionalInfo());
			try {
				String serviceResourceString = JsonUtilsV2.objToJson(storedServiceResource);
				ret.setServiceResourceElements(serviceResourceString);
			} catch (Exception e) {
				ret = null;
			}

		} else {
			ret = null;
		}

		return ret;
	}

	public RangerServiceResource createServiceResource(RangerServiceResource viewObject) {
		XXRMSServiceResource dbObject = populateEntityBean(viewObject);
		if (dbObject != null) {
				dbObject = daoManager.getXXRMSServiceResource().create(dbObject);
			if (dbObject != null) {
				return populateViewBean(dbObject);
			}
		}
		return null;
	}

	public void deleteById(Long serviceResourceId) {
		getEntityManager()
				.createNamedQuery("XXRMSServiceResource.deleteById")
				.setParameter("resourceId", serviceResourceId)
				.executeUpdate();
	}

	public List<RangerServiceResource> findByLlServiceId(long llServiceId) {
		return findByServiceId(llServiceId);
	}

	public List<RangerServiceResource> getLlResourceIdForHlResourceId(long hlResourceId, long lastKnownVersion) {
		List<RangerServiceResource> ret = new ArrayList<>();
		try {
			List<XXRMSServiceResource> list = getEntityManager().createNamedQuery("XXRMSServiceResource.getLlResourceIdForHlResourceId", tClass)
					.setParameter("hlResourceId", hlResourceId)
					.setParameter("lastKnownVersion", lastKnownVersion)
					.getResultList();
			if (CollectionUtils.isNotEmpty(list)) {
				//ret = list.stream().map(XXRMSServiceResourceDao::populateViewBean).collect(Collectors.toList());
				for (XXRMSServiceResource entityBean : list) {
					RangerServiceResource viewBean = populateViewBean(entityBean);
					ret.add(viewBean);
				}
			}
		} catch (NoResultException e) {
		}
		return ret;
	}

	public void purge(long serviceId) {

		getEntityManager().createNamedQuery("XXRMSNotification.deleteByServiceId")
				.setParameter("serviceId", serviceId)
				.executeUpdate();

		getEntityManager().createNamedQuery("XXRMSResourceMapping.deleteByServiceId")
				.setParameter("serviceId", serviceId)
				.executeUpdate();

		getEntityManager().createNamedQuery("XXRMSServiceResource.deleteByServiceId")
				.setParameter("serviceId", serviceId)
				.executeUpdate();

	}
}
