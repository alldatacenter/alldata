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

package org.apache.ranger.patch.cliutil;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.patch.BaseLoader;
import org.apache.ranger.service.XPortalUserService;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class XXTrxLogUpdateUtil extends BaseLoader {
	private static final Logger logger = LoggerFactory
			.getLogger(XXTrxLogUpdateUtil.class);

	@Autowired
	XUserMgr xUserMgr;

	@Autowired
	XPortalUserService xPortalUserService;

	@Autowired
	RangerDaoManager daoManager;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			XXTrxLogUpdateUtil loader = (XXTrxLogUpdateUtil) CLIUtil
					.getBean(XXTrxLogUpdateUtil.class);

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
		logger.info("==> XTrxLogUpdate.execLoad() Start.");
		updateXTrxLog();
		logger.info("<== XTrxLogUpdate.execLoad() End.");
	}

	public void updateXTrxLog() {
		long maxXTrxLogID = daoManager.getXXTrxLog().getMaxIdOfXXTrxLog();
		if(maxXTrxLogID==0){
			return;
		}
		long maxRowsPerBatch=10000;
		//To ceil the actual division result i.e noOfBatches=maxXTrxLogID/maxRowsPerBatch
		long noOfBatches=(maxXTrxLogID+maxRowsPerBatch-1)/maxRowsPerBatch;
		long minRange=0;
		long maxRange=maxXTrxLogID<=maxRowsPerBatch ? maxXTrxLogID : maxRowsPerBatch;
		long startTimeInMS=0;
		long timeTaken=0;
		for(long index=1;index<=noOfBatches;index++){
			logger.info("Batch "+ index+" of total "+noOfBatches);
			startTimeInMS=System.currentTimeMillis();
			//minRange and maxRange both exclusive, if we add +1 in maxRange
			int rowsAffected=daoManager.getXXTrxLog().updateXTrxLog(minRange,maxRange+1,AppConstants.CLASS_TYPE_XA_USER,"Password",AppConstants.Masked_String);
			timeTaken=(System.currentTimeMillis()-startTimeInMS);
			logger.info(rowsAffected +" rows affected ("+timeTaken+" ms)");
			minRange=maxRange;
			maxRange=maxRange+maxRowsPerBatch;
		}
	}

	@Override
	public void printStats() {
	}

}
