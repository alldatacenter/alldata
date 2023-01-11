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

import com.google.gson.Gson;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXGlobalState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class XXGlobalStateDao extends BaseDao<XXGlobalState> {
    private static final Logger logger = LoggerFactory.getLogger(XXGlobalStateDao.class);

    final static String APP_DATA_ENTRY_VERSION = "Version";

    public void onGlobalStateChange(String stateName) throws Exception {

        if (StringUtils.isBlank(stateName)) {
            logger.error("Invalid name for state:[" + stateName +"]");
            throw new Exception("Invalid name for state:[" + stateName +"]");
        } else {
            try {
                XXGlobalState globalState = findByStateName(stateName);
                if (globalState == null) {
                    globalState = new XXGlobalState();
                    globalState.setStateName(stateName);
                    create(globalState);
                } else {
                    Date date = DateUtil.getUTCDate();
                    if (date == null) {
                        date = new Date();
                    }
                    globalState.setAppData(date.toString());

                    update(globalState);
                }
            } catch (Exception exception) {
                logger.error("Cannot create/update GlobalState for state:[" + stateName + "]", exception);
                throw exception;
            }
        }
    }

	public void onGlobalAppDataChange(String stateName) throws Exception {

		if (StringUtils.isBlank(stateName)) {
			logger.error("Invalid name for state:[" + stateName + "]");
			throw new Exception("Invalid name for state:[" + stateName + "]");
		} else {
			try {
				XXGlobalState globalState = findByStateName(stateName);
				if (globalState == null) {
					createGlobalStateForAppDataVersion(stateName);
				} else {
					updateGlobalStateForAppDataVersion(globalState, stateName);
				}
			} catch (OptimisticLockException | org.eclipse.persistence.exceptions.OptimisticLockException ole) {
				logger.warn("One or more objects cannot be updated because it has changed or been deleted since it was last read. Unable to update GlobalState for state:[" + stateName + "] continuing...");
			} catch (Exception exception) {
				logger.warn("Cannot create/update GlobalState for state:[" + stateName + "] continuing...");
			}
		}
	}

    public Long getAppDataVersion(String stateName) {
        Long ret = null;
        try {
            XXGlobalState       globalState     = findByStateName(stateName);
            if (globalState != null) {
                Map<String, String> appDataVersionJson = new Gson().fromJson(globalState.getAppData(), Map.class);
                if (MapUtils.isNotEmpty(appDataVersionJson)) {
                    ret = Long.valueOf(appDataVersionJson.get(APP_DATA_ENTRY_VERSION));
                } else {
                    ret = 1L;
                }
            }
        } catch (Exception exception) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to find the version for " + stateName + " in Ranger Database", exception);
            }
        }
        return ret;
    }

    /**
     * Default Constructor
     */
    public XXGlobalStateDao(RangerDaoManagerBase daoManager) {
        super(daoManager);
    }
    public XXGlobalState findByStateId(Long stateId) {
        if (stateId == null) {
            return null;
        }
        try {
            XXGlobalState xxGlobalState = getEntityManager()
                    .createNamedQuery("XXGlobalState.findByStateId", tClass)
                    .setParameter("stateId", stateId)
                    .getSingleResult();
            return xxGlobalState;
        } catch (NoResultException e) {
            return null;
        }
    }
    public XXGlobalState findByStateName(String stateName) {
        if (StringUtils.isBlank(stateName)) {
            return null;
        }
        try {
            XXGlobalState xxGlobalState = getEntityManager()
                    .createNamedQuery("XXGlobalState.findByStateName", tClass)
                    .setParameter("stateName", stateName)
                    .getSingleResult();
            return xxGlobalState;
        } catch (NoResultException e) {
            return null;
        }
    }

    private void createGlobalStateForAppDataVersion(String stateName) {
        XXGlobalState globalState = new XXGlobalState();
        globalState.setStateName(stateName);
        Map<String,String> appDataVersion = new HashMap<>();
        appDataVersion.put(APP_DATA_ENTRY_VERSION,new String(Long.toString(1L)));
        globalState.setAppData(new Gson().toJson(appDataVersion));
        create(globalState);
    }

    private void updateGlobalStateForAppDataVersion(XXGlobalState globalState, String stateName) {
        Map<String,String> appDataVersionJson = new Gson().fromJson(globalState.getAppData(),Map.class);
        if (MapUtils.isNotEmpty(appDataVersionJson)) {
            Long appDataVersion = Long.valueOf(appDataVersionJson.get(APP_DATA_ENTRY_VERSION)) + 1L;
            appDataVersionJson.put(APP_DATA_ENTRY_VERSION, new String(Long.toString(appDataVersion)));
            globalState.setAppData(new Gson().toJson(appDataVersionJson));
            update(globalState);
        } else {
            //if not present create Global State for state name Version.
            createGlobalStateForAppDataVersion(stateName);
        }
    }
}

