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

import javax.persistence.NoResultException;

import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class XXAssetDao extends BaseDao<XXAsset> {
	private static final Logger logger = LoggerFactory.getLogger(XXAssetDao.class);

    public XXAssetDao( RangerDaoManagerBase  daoManager ) {
		super(daoManager);
    }

    public XXAsset findByAssetName(String name){
		if (daoManager.getStringUtil().isEmpty(name)) {
			logger.debug("name is empty");
			return null;
		}
		try {
			return getEntityManager()
					.createNamedQuery("XXAsset.findByAssetName", XXAsset.class)
					.setParameter("name", name.trim())
					.setParameter("status",RangerCommonEnums.STATUS_DELETED)
					.getSingleResult();
		} catch (NoResultException e) {
			// ignore
		}
		return null;
    }

}

