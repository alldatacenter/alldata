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
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXServiceResource;
import org.springframework.stereotype.Service;

@Service
public class XXServiceResourceDao extends BaseDao<XXServiceResource> {

	public XXServiceResourceDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public XXServiceResource findByGuid(String guid) {
		if (StringUtil.isEmpty(guid)) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceResource.findByGuid", tClass)
					.setParameter("guid", guid).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<XXServiceResource> findByServiceId(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<XXServiceResource>();
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceResource.findByServiceId", tClass)
					.setParameter("serviceId", serviceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXServiceResource>();
		}
	}

	public XXServiceResource findByServiceAndResourceSignature(Long serviceId, String resourceSignature) {
		if (StringUtils.isBlank(resourceSignature)) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceResource.findByServiceAndResourceSignature", tClass)
					.setParameter("serviceId", serviceId).setParameter("resourceSignature", resourceSignature)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<XXServiceResource> findTaggedResourcesInServiceId(Long serviceId) {
	    List<XXServiceResource> ret = new ArrayList<>();
		if (serviceId != null) {
            List<Object[]> rows = null;
            try {
                rows = getEntityManager().createNamedQuery("XXServiceResource.findTaggedResourcesInServiceId", Object[].class)
                        .setParameter("serviceId", serviceId).getResultList();
            } catch (NoResultException e) {
                // Nothing
            }
            if (CollectionUtils.isNotEmpty(rows)) {
                for (Object[] row : rows) {
                    XXServiceResource xxServiceResource = new XXServiceResource();
                    xxServiceResource.setId((Long) row[0]);
                    xxServiceResource.setGuid((String) row[1]);
                    xxServiceResource.setVersion((Long) row[2]);
                    xxServiceResource.setIsEnabled((Boolean) row[3]);
                    xxServiceResource.setResourceSignature((String) row[4]);
                    xxServiceResource.setServiceId((Long) row[5]);
                    xxServiceResource.setServiceResourceElements((String) row[6]);
                    xxServiceResource.setTags((String) row[7]);

                    ret.add(xxServiceResource);
                }
            }
        }
        return ret;
	}

	public long countTaggedResourcesInServiceId(Long serviceId) {
		if (serviceId == null) {
			return -1;
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceResource.countTaggedResourcesInServiceId", Long.class)
					.setParameter("serviceId", serviceId).getSingleResult();
		} catch (NoResultException e) {
			return -1;
		}
	}

	public List<XXServiceResource> findForServicePlugin(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<XXServiceResource>();
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceResource.findForServicePlugin", tClass)
					.setParameter("serviceId", serviceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXServiceResource>();
		}
	}

	public List<String> findServiceResourceGuidsInServiceId(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<String>();
		}
		try {
			return getEntityManager().createNamedQuery("XXServiceResource.findServiceResourceGuidsInServiceId", String.class)
					.setParameter("serviceId", serviceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<String>();
		}
	}
}
