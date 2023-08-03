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

import com.google.gson.Gson;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXGlobalState;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PatchForXGlobalState_J10036 extends BaseLoader {
	private static final Logger logger = LoggerFactory
			.getLogger(PatchForXGlobalState_J10036.class);

	@Autowired
	RangerDaoManager daoManager;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			PatchForXGlobalState_J10036 loader = (PatchForXGlobalState_J10036) CLIUtil
					.getBean(PatchForXGlobalState_J10036.class);

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
		logger.info("==> ServiceVersionInfoPatch.execLoad()");
		updateRangerRoleVersionToVersion();
		logger.info("<== ServiceVersionInfoPatch.execLoad()");
	}

	public void updateRangerRoleVersionToVersion() {
		XXGlobalState globalState     = daoManager.getXXGlobalState().findByStateName("RangerRole");
		if (globalState != null) {
			logger.info("Updating globalstate with id = " + globalState.getId());
			Map<String, String> appDataVersionJson = new Gson().fromJson(globalState.getAppData(), Map.class);
			if (MapUtils.isNotEmpty(appDataVersionJson)) {
				logger.info("Updating globalstate appdata version for = " + appDataVersionJson);
				String roleVersion = appDataVersionJson.get("RangerRoleVersion");
				if (StringUtils.isNotEmpty(roleVersion)) {
					appDataVersionJson.put("Version", roleVersion);
					appDataVersionJson.remove("RangerRoleVersion");
					globalState.setAppData(new Gson().toJson(appDataVersionJson));
					daoManager.getXXGlobalState().update(globalState);
				}
			}
		}
	}

	@Override
	public void printStats() {
	}

}
