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

package org.apache.ranger.patch;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PatchForExternalUserStatusUpdate_J10056 extends BaseLoader {

	private static final Logger logger = LoggerFactory.getLogger(PatchForExternalUserStatusUpdate_J10056.class);

	@Autowired
	private RangerDaoManager daoManager;

	@Autowired
	@Qualifier(value = "transactionManager")
	PlatformTransactionManager txManager;

	public static void main(String[] args) {
		try {
			PatchForExternalUserStatusUpdate_J10056 loader = (PatchForExternalUserStatusUpdate_J10056) CLIUtil
					.getBean(PatchForExternalUserStatusUpdate_J10056.class);
			loader.init();
			while (loader.isMoreToProcess()) {
				loader.load();
			}
			logger.info("Load complete. Exiting!!!");
			System.exit(0);
		} catch (Exception e) {
			logger.error("Error loading", e);
			System.exit(1);
		}
	}

	@Override
	public void init() throws Exception {
		// Do Nothing
	}

	@Override
	public void printStats() {
		// TODO Auto-generated method stub
	}

	@Override
	public void execLoad() {
		updateExternalUserStatus();
	}

	private void updateExternalUserStatus() {
		XXPortalUserDao dao = this.daoManager.getXXPortalUser();
		List<XXPortalUser> xXPortalUsers = dao.findByUserSourceAndStatus(RangerCommonEnums.USER_EXTERNAL,RangerCommonEnums.ACT_STATUS_DISABLED);

		if(CollectionUtils.isNotEmpty(xXPortalUsers)) {
			for (XXPortalUser xxPortalUser : xXPortalUsers) {
				if (xxPortalUser != null) {
					xxPortalUser.setStatus(RangerCommonEnums.ACT_STATUS_ACTIVE);
					TransactionTemplate txTemplate = new TransactionTemplate(txManager);
					txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
					try {
						txTemplate.execute(new TransactionCallback<Object>() {
							@Override
							public Object doInTransaction(TransactionStatus status) {
								dao.update(xxPortalUser, true);
								return null;
							}
						});
					} catch (Throwable ex) {
						logger.error("updateExternalUserStatus(): Failed to update DB for user: " + xxPortalUser.getLoginId() + " ", ex);
						throw new RuntimeException(ex);
					}
				}
			}
		}
	}
}
