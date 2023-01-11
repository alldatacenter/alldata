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



import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.db.RangerTransactionSynchronizationAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RangerDaoManager extends RangerDaoManagerBase {
	private static final Logger logger = LoggerFactory.getLogger(RangerDaoManager.class);

	@PersistenceContext(unitName = "defaultPU")
	private EntityManager em;

	@PersistenceContext(unitName = "loggingPU")
	private EntityManager loggingEM;

	@Autowired
	StringUtil stringUtil;

	@Autowired
	RangerTransactionSynchronizationAdapter transactionSynchronizationAdapter;

	@Override
	public EntityManager getEntityManager() {
		return em;
	}

	public EntityManager getEntityManager(String persistenceContextUnit) {
		if(logger.isDebugEnabled()) {
			logger.debug("RangerDaoManager.getEntityManager(" + persistenceContextUnit + ")");
		}

		if ("loggingPU".equalsIgnoreCase(persistenceContextUnit)) {
			return loggingEM;
		}
		return getEntityManager();
	}

	
	/**
	 * @return the stringUtil
	 */
	public StringUtil getStringUtil() {
		return stringUtil;
	}

	public RangerTransactionSynchronizationAdapter getRangerTransactionSynchronizationAdapter() {
		return transactionSynchronizationAdapter;
	}

}
