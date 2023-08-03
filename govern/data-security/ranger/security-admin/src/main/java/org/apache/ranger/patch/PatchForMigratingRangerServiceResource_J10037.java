/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Consolidates Ranger policy details into a JSON string and stores it into a
 * column in x_policy table After running this patch Ranger policy can be
 * completely read/saved into x_policy table and some related Ref tables (which
 * maintain ID->String mapping for each policy).
 *
 */

package org.apache.ranger.patch;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceResource;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.store.RangerServiceResourceSignature;
import org.apache.ranger.service.RangerServiceResourceService;
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
public class PatchForMigratingRangerServiceResource_J10037 extends BaseLoader {

	private static final Logger logger = LoggerFactory.getLogger(PatchForMigratingRangerServiceResource_J10037.class);

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	@Qualifier(value = "transactionManager")
	PlatformTransactionManager txManager;

	@Autowired
	RangerServiceResourceService serviceResourceService;

	public static void main(String[] args) {
		logger.info("main() starts");
		try {
			PatchForMigratingRangerServiceResource_J10037 loader = (PatchForMigratingRangerServiceResource_J10037) CLIUtil
					.getBean(PatchForMigratingRangerServiceResource_J10037.class);

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
	public void execLoad() {
		logger.info("==> PatchForMigratingRangerServiceResource.execLoad()");

		try {
			updateRangerServiceResourceSignature();
		} catch (Exception e) {
			logger.error("Error while updateRangerServiceResourceSignature()", e);
			System.exit(1);
		}

		logger.info("<== PatchForMigratingRangerServiceResource.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info(" Updating Ranger Service Resource signature ");
	}

	private void updateRangerServiceResourceSignature() throws Exception {
		logger.info("==> updateRangerServiceResourceSignature() start ");

		List<XXService> allServices = daoMgr.getXXService().getAll();

		if (CollectionUtils.isNotEmpty(allServices)) {

			for (XXService xService : allServices) {
				logger.info("processing ranger service: " + xService);

				List<String> serviceResourceGuids = daoMgr.getXXServiceResource().findServiceResourceGuidsInServiceId(xService.getId());

				if (CollectionUtils.isNotEmpty(serviceResourceGuids)) {

					TransactionTemplate txTemplate = new TransactionTemplate(txManager);

					int chunkSize   = 1000; // hardcoded
					int numOfChunks = (serviceResourceGuids.size() / chunkSize) + 1;

					for (int chunkIndex = 0; chunkIndex < numOfChunks; chunkIndex++) {
						List<String> chunk = serviceResourceGuids.subList(chunkIndex * chunkSize, (chunkIndex == numOfChunks -1 ? serviceResourceGuids.size() : (chunkIndex + 1) * chunkSize));

						ServiceResourceUpdaterThread updaterThread = new ServiceResourceUpdaterThread(txTemplate, chunk);

						String errorMsg = runThread(updaterThread);

						if (StringUtils.isNotEmpty(errorMsg)) {
							throw new Exception(errorMsg);
						}
					}
				} else {
					logger.info("No Ranger service resource found for service : " + xService.getDisplayName());
				}
			}
		} else {
			logger.info("No Ranger service found");
		}

		logger.info("<== updateRangerServiceResourceSgnature() end");
	}

	private String runThread(ServiceResourceUpdaterThread updaterThread) throws Exception {
		updaterThread.setDaemon(true);
		updaterThread.start();
		updaterThread.join();
		return updaterThread.getErrorMsg();
	}

	private class ServiceResourceUpdaterThread extends Thread {
		final TransactionTemplate     txTemplate;
		final List<String>            entityGuids;
		String                        errorMsg;

		ServiceResourceUpdaterThread(TransactionTemplate txTemplate, final List<String> entityGuids) {
			this.txTemplate  = txTemplate;
			this.entityGuids = entityGuids;
			this.errorMsg    = null;
		}

		public String getErrorMsg() {
			return errorMsg;
		}

		@Override
		public void run() {
			txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

			errorMsg = txTemplate.execute(new TransactionCallback<String>() {
				@Override
				public String doInTransaction(TransactionStatus status) {
					String ret = null;
					try {
						if (CollectionUtils.isNotEmpty(entityGuids)) {
							for (String entityGuid : entityGuids) {
								XXServiceResource              entityObject = daoMgr.getXXServiceResource().findByGuid(entityGuid);
								RangerServiceResource          viewObject   = serviceResourceService.getPopulatedViewObject(entityObject);
								RangerServiceResourceSignature serializer   = new RangerServiceResourceSignature(viewObject);

								entityObject.setResourceSignature(serializer.getSignature());

								daoMgr.getXXServiceResource().update(entityObject);
							}
						}
					} catch (Throwable e) {
						logger.error("signature update  failed :[rangerServiceResource=" + entityGuids + "]", e);
						ret = e.toString();
					}
					return ret;
				}
			});
		}
	}

}
