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
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXTagResourceMap;
import org.springframework.stereotype.Service;

@Service
public class XXTagResourceMapDao extends BaseDao<XXTagResourceMap> {

	public XXTagResourceMapDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public XXTagResourceMap findByGuid(String resourceGuid) {
		if (StringUtil.isEmpty(resourceGuid)) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXTagResourceMap.findByGuid", tClass)
					.setParameter("guid", resourceGuid).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<XXTagResourceMap> findByResourceId(Long resourceId) {
		List<XXTagResourceMap> ret = null;

		if (resourceId == null) {
			ret = new ArrayList<>();
		} else {
			try {
				List<Object[]> rows = getEntityManager().createNamedQuery("XXTagResourceMap.findByResourceId", Object[].class)
						                                .setParameter("resourceId", resourceId).getResultList();

				ret = fromRows(rows);
			} catch (NoResultException e) {
				ret = new ArrayList<>();
			}
		}

		return ret;
	}

	public List<XXTagResourceMap> findByResourceGuid(String resourceGuid) {
		List<XXTagResourceMap> ret = null;

		if (resourceGuid == null) {
			ret = new ArrayList<>();
		} else {
			try {
				List<Object[]> rows = getEntityManager().createNamedQuery("XXTagResourceMap.findByResourceGuid", Object[].class)
						                                .setParameter("resourceGuid", resourceGuid).getResultList();

				ret = fromRows(rows);
			} catch (NoResultException e) {
				ret = new ArrayList<>();
			}
		}

		return ret;
	}

	public List<Long> findTagIdsForResourceId(Long resourceId) {
		try {
			return getEntityManager().createNamedQuery("XXTagResourceMap.getTagIdsForResourceId", Long.class)
					.setParameter("resourceId", resourceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<Long>();
		}
	}

	public List<XXTagResourceMap> findByTagId(Long tagId) {
		List<XXTagResourceMap> ret = null;

		if (tagId == null) {
			ret = new ArrayList<>();
		} else {
			try {
				List<Object[]>  rows = getEntityManager().createNamedQuery("XXTagResourceMap.findByTagId", Object[].class)
                                                         .setParameter("tagId", tagId).getResultList();

				ret = fromRows(rows);
			} catch (NoResultException e) {
				ret = new ArrayList<>();
			}
		}

		return ret;
	}

	public List<XXTagResourceMap> findByTagGuid(String tagGuid) {
		if (StringUtil.isEmpty(tagGuid)) {
			return new ArrayList<XXTagResourceMap>();
		}
		try {
			return getEntityManager().createNamedQuery("XXTagResourceMap.findByTagGuid", tClass)
					.setParameter("tagGuid", tagGuid).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXTagResourceMap>();
		}
	}

	public XXTagResourceMap findByTagAndResourceId(Long tagId, Long resourceId) {
		if (tagId == null || resourceId == null) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXTagResourceMap.findByTagAndResourceId", tClass)
					.setParameter("tagId", tagId)
					.setParameter("resourceId", resourceId).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public XXTagResourceMap findByTagAndResourceGuid(String tagGuid, String resourceGuid) {
		if (tagGuid == null || resourceGuid == null) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXTagResourceMap.findByTagAndResourceGuid", tClass)
					.setParameter("tagGuid", tagGuid)
					.setParameter("resourceGuid", resourceGuid).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<XXTagResourceMap> findByServiceId(Long serviceId) {
		if (serviceId == null) {
			return new ArrayList<XXTagResourceMap>();
		}
		try {
			return getEntityManager().createNamedQuery("XXTagResourceMap.findByServiceId", tClass)
					.setParameter("serviceId", serviceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXTagResourceMap>();
		}
	}

	private XXTagResourceMap fromRow(Object[] row) {
		XXTagResourceMap ret = new XXTagResourceMap();

		ret.setId((Long) row[0]);
		ret.setGuid((String) row[1]);
		ret.setTagId((Long) row[2]);
		ret.setResourceId((Long) row[3]);

		return ret;
	}

	private List<XXTagResourceMap> fromRows(List<Object[]> rows) {
		final List<XXTagResourceMap> ret;

		if (CollectionUtils.isNotEmpty(rows)) {
			ret = new ArrayList<>(rows.size());

			for (Object[] row : rows) {
				ret.add(fromRow(row));
			}
		} else {
			ret = new ArrayList<>();
		}

		return ret;
	}
}
