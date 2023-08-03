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


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class XXResourceDao extends BaseDao<XXResource> {
	private static final Logger logger = LoggerFactory.getLogger(XXResourceDao.class);

	public XXResourceDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public XXResource findByResourceName(String name) {
		if (daoManager.getStringUtil().isEmpty(name)) {
			logger.debug("name is empty");
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXResource.findByResourceName",
							XXResource.class).setParameter("name", name.trim())
					.getSingleResult();
		} catch (NoResultException e) {
			// ignore
		}
		return null;
	}	

	public List<XXResource> findUpdatedResourcesByAssetId(
			Long assetId, Date lastUpdated) {
		if (assetId != null) {
			try {
				return getEntityManager()
						.createNamedQuery("XXResource.findUpdatedResourcesByAssetId",
								XXResource.class)
						.setParameter("assetId", assetId)
						.setParameter("lastUpdated", lastUpdated)
						.getResultList();
			} catch (NoResultException e) {
				logger.debug(e.getMessage());
			}
		} else {
			logger.debug("AssetId not provided.");
			return new ArrayList<XXResource>();
		}
		return null;
	}
	
	public List<XXResource> findByAssetId(Long assetId) {
		List<XXResource> xResourceList = null;
		if (assetId != null) {
			try {
				xResourceList = getEntityManager()
						.createNamedQuery("XXResource.findByAssetId", XXResource.class)
						.setParameter("assetId", assetId)
						.getResultList();
			} catch (NoResultException e) {
				// ignore
				logger.debug(e.getMessage());
			}
			if(xResourceList == null) {
				xResourceList = new ArrayList<XXResource>();
			}
		} else {
			logger.debug("AssetId not provided.");
			xResourceList = new ArrayList<XXResource>();
		}
		return xResourceList;
	}
	
	public List<XXResource> findByAssetType(Integer assetType) {
		List<XXResource> xResourceList = null;
		if (assetType != null) {
			try {
				xResourceList = getEntityManager()
						.createNamedQuery("XXResource.findByAssetType", XXResource.class)
						.setParameter("assetType", assetType)
						.getResultList();
			} catch (NoResultException e) {
				// ignore
				logger.debug(e.getMessage());
			}
			if(xResourceList == null) {
				xResourceList = new ArrayList<XXResource>();
			}
		} else {
			logger.debug("AssetType not provided.");
			xResourceList = new ArrayList<XXResource>();
		}
		return xResourceList;
	}
	
	public Timestamp getMaxUpdateTimeForAssetName(String assetName) {
		if (assetName == null) {
			return null;
		}
		try {		
			 Date date=(Date)getEntityManager()
			.createNamedQuery("XXResource.getMaxUpdateTimeForAssetName")
			.setParameter("assetName", assetName)
			.getSingleResult();
			 if(date!=null){
				 Timestamp timestamp=new Timestamp(date.getTime());	
				 return timestamp;
			 }else{
				 return null;
			 }		
		} catch (NoResultException e) {
			// ignore
		}
		return null;
	}

	public List<XXResource> findUpdatedResourcesByAssetName(
			String assetName, Date lastUpdated) {
		if (assetName != null) {
			try {
				return getEntityManager()
						.createNamedQuery(
								"XXResource.findUpdatedResourcesByAssetName",
								XXResource.class)
						.setParameter("assetName", assetName)
						.setParameter("lastUpdated", lastUpdated)
						.getResultList();
			} catch (NoResultException e) {
				logger.debug(e.getMessage());
			}
		} else {
			logger.debug("Asset name not provided.");
			return new ArrayList<XXResource>();
		}
		return null;
	}

	public List<XXResource> findByResourceNameAndAssetIdAndRecursiveFlag(
			String name,Long assetId,int isRecursive ) {
		if (daoManager.getStringUtil().isEmpty(name)) {
			logger.debug("name is empty");
			return null;
		}
		if (assetId==null) {
			logger.debug("assetId is null");
			return null;
		}
		try {
			String resourceName = name.trim();
			resourceName = "%"+resourceName+"%";
			return getEntityManager()
					.createNamedQuery(
							"XXResource.findByResourceNameAndAssetIdAndRecursiveFlag",
							XXResource.class).setParameter("name", resourceName)							
					.setParameter("assetId", assetId)
					.setParameter("isRecursive", isRecursive)
					.getResultList();
		} catch (NoResultException e) {
			// ignore
		}
		return null;
	}

	public List<XXResource> findByResourceNameAndAssetIdAndResourceType(String name,Long assetId,int resourceType) {
		if (daoManager.getStringUtil().isEmpty(name)) {
			logger.debug("name is empty");
			return null;
		}
		if (assetId==null) {
			logger.debug("assetId is null");
			return null;
		}
		try {
			String resourceName = name.trim();
			resourceName = "%"+resourceName+"%";
			return getEntityManager()
					.createNamedQuery(
							"XXResource.findByResourceNameAndAssetIdAndResourceType",
							XXResource.class).setParameter("name", resourceName)							
					.setParameter("assetId", assetId)
					.setParameter("resourceType", resourceType)
					.getResultList();
		} catch (NoResultException e) {
			// ignore
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public List<XXResource> findByAssetIdAndResourceTypes(Long assetId,
			List<Integer> resourceType) {
		if (assetId == null) {
			logger.debug("assetId is null");
			return null;
		}
		try {
			StringBuilder query = new StringBuilder(
					"SELECT obj FROM XXResource obj WHERE obj.assetId="
							+ assetId);
			String whereClause = makeWhereCaluseForResourceType(resourceType);
			if (!whereClause.trim().isEmpty()) {
				query.append(" and ( " + whereClause + " )");
			}
			return getEntityManager().createQuery(query.toString())
					.getResultList();
		} catch (NoResultException e) {
			// ignore
		}
		return null;
	}

	private String makeWhereCaluseForResourceType(List<Integer> resourceTypes) {
		StringBuilder whereClause = new StringBuilder();
		if (resourceTypes != null && resourceTypes.size() != 0) {

			for (int i = 0; i < resourceTypes.size() - 1; i++) {
				whereClause.append("obj.resourceType=" + resourceTypes.get(i)
						+ " OR ");
			}
			whereClause.append("obj.resourceType="
					+ resourceTypes.get(resourceTypes.size() - 1));
		}
		return whereClause.toString();
	}
	
	public List<XXResource> findByAssetIdAndResourceStatus(Long assetId, int resourceStatus) {
		List<XXResource> xResourceList = null;
		if (assetId != null) {
			try {
				xResourceList = getEntityManager()
						.createNamedQuery("XXResource.findByAssetIdAndResourceStatus", XXResource.class)
						.setParameter("assetId", assetId)
						.setParameter("resourceStatus", resourceStatus)
						.getResultList();
			} catch (NoResultException e) {
				// ignore
				logger.debug(e.getMessage());
			}
			if(xResourceList == null) {
				xResourceList = new ArrayList<XXResource>();
			}
		} else {
			logger.debug("AssetId not provided.");
			xResourceList = new ArrayList<XXResource>();
		}
		return xResourceList;
	}
}
