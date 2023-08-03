/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXDataHist;
import org.apache.ranger.common.DateUtil;
import org.springframework.stereotype.Service;

@Service
public class XXDataHistDao extends BaseDao<XXDataHist> {

	public XXDataHistDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public XXDataHist findLatestByObjectClassTypeAndObjectId(Integer classType, Long objectId) {
		if(classType == null || objectId == null) {
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXDataHist.findLatestByObjectClassTypeAndObjectId", tClass)
					.setParameter("classType", classType)
					.setParameter("objectId", objectId)
					.setMaxResults(1).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	
	public XXDataHist findObjByEventTimeClassTypeAndId(String eventTime, int classType, Long objId) {
		if (eventTime == null || objId == null) {
			return null;
		}
		Date date=DateUtil.stringToDate(eventTime,"yyyy-MM-dd'T'HH:mm:ss'Z'");
		if(date==null){
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXDataHist.findLatestByObjectClassTypeAndObjectIdAndEventTime", tClass)
					.setParameter("classType", classType)
					.setParameter("objectId", objId)
					.setParameter("createTime", date)
					.setMaxResults(1).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Integer> getVersionListOfObject(Long objId, int classType) {
		if (objId == null) {
			return new ArrayList<Integer>();
		}
		try {
			return getEntityManager().createNamedQuery("XXDataHist.getVersionListOfObject")
					.setParameter("objId", objId).setParameter("classType", classType).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<Integer>();
		}
	}

	public XXDataHist findObjectByVersionNumber(Long objId, int classType, int versionNo) {
		if (objId == null) {
			return null;
		}
		try {
			return getEntityManager().createNamedQuery("XXDataHist.findObjectByVersionNumber", tClass)
					.setParameter("objId", objId).setParameter("classType", classType)
					.setParameter("version", versionNo).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

}
