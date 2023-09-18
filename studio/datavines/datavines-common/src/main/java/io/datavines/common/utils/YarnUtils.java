/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.utils;

import io.datavines.common.utils.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.datavines.common.CommonConstants;
import io.datavines.common.enums.ExecutionStatus;

public class YarnUtils {

    private static final Logger logger = LoggerFactory.getLogger(YarnUtils.class);

    private static final String YARN_MODE_NONE = "none";
    private static final String YARN_MODE_HA = "ha";
    private static final String YARN_MODE_STANDALONE = "standalone";

    private static final String YARN_MODE_KEY = "yarn.mode";
    private static final String YARN_RESOURCE_MANAGER_HA_IDS_KEY = "yarn.resource.manager.ha.ids";
    private static final String YARN_APPLICATION_STATUS_ADDRESS_KEY = "yarn.application.status.address";

    private static final String YARN_RESOURCE_MANAGER_HTTP_ADDRESS_PORT_KEY = "yarn.resource.manager.http.address.port";

    private static final String YARN_MODE = CommonPropertyUtils.getString(YARN_MODE_KEY, YARN_MODE_NONE);

    private static final String YARN_RESOURCE_MANAGER_HA_IDS =
            CommonPropertyUtils.getString(YARN_RESOURCE_MANAGER_HA_IDS_KEY,"");

    private static final String YARN_APPLICATION_STATUS_ADDRESS =
            CommonPropertyUtils.getString(YARN_APPLICATION_STATUS_ADDRESS_KEY, "");

    private static final String HADOOP_RM_STATE_ACTIVE = "ACTIVE";
    private static final String HADOOP_RM_STATE_STANDBY = "STANDBY";


    private static boolean yarnEnabled = false;

    /**
     * get application url
     * if rmHaIds contains xx, it signs not use resource manager
     * otherwise:
     * if rmHaIds is empty, single resource manager enabled
     * if rmHaIds not empty: resource manager HA enabled
     *
     * @param applicationId application id
     * @return url of application
     */
    public static String getApplicationUrl(String applicationId) {

        String appUrl = "";
        //not use resource manager
        if (YARN_MODE_NONE.equals(YARN_MODE)){
            yarnEnabled = false;
            logger.warn("should not step here");
            return appUrl;
        } else if (YARN_MODE_HA.equals(YARN_MODE)) {
            //resource manager HA enabled
            appUrl = getAppAddress(YARN_APPLICATION_STATUS_ADDRESS, YARN_RESOURCE_MANAGER_HA_IDS);
            yarnEnabled = true;
        } else if (YARN_MODE_STANDALONE.equals(YARN_MODE)) {
            //single resource manager enabled
            appUrl = YARN_APPLICATION_STATUS_ADDRESS;
            yarnEnabled = true;
        }

        if (StringUtils.isEmpty(appUrl)) {
            return appUrl;
        }

        appUrl = String.format(appUrl, CommonPropertyUtils.getInt(
                YARN_RESOURCE_MANAGER_HTTP_ADDRESS_PORT_KEY, 8088),applicationId);
        logger.info("application url : {}", appUrl);
        return appUrl;
    }

    /**
     * getAppAddress
     *
     * @param appAddress app address
     * @param rmHa       resource manager ha
     * @return app address
     */
    public static String getAppAddress(String appAddress, String rmHa) {

        //get active ResourceManager
        String activeResourceManager = getActiveResourceManagerName(rmHa);
        if (StringUtils.isEmpty(activeResourceManager)) {
            return null;
        }
        //http://ds1:8088/ws/v1/cluster/apps/%s
        String[] split1 = appAddress.split(CommonConstants.DOUBLE_SLASH);

        if (split1.length != 2) {
            return null;
        }

        String start = split1[0] + CommonConstants.DOUBLE_SLASH;
        String[] split2 = split1[1].split(CommonConstants.COLON);

        if (split2.length != 2) {
            return null;
        }

        String end = CommonConstants.COLON + split2[1];

        return start + activeResourceManager + end;
    }

    public static String getActiveResourceManagerName(String rmIds) {

        String[] rmIdArr = rmIds.split(CommonConstants.COMMA);
        int activeResourceManagerPort = CommonPropertyUtils.getInt(
                YARN_RESOURCE_MANAGER_HTTP_ADDRESS_PORT_KEY, 8088);
        String yarnUrl = "http://%s:" + activeResourceManagerPort + "/ws/v1/cluster/info";

        String state = null;
        try {

            state = getResourceManagerState(String.format(yarnUrl, rmIdArr[0]));

            if (HADOOP_RM_STATE_ACTIVE.equals(state)) {
                return rmIdArr[0];
            } else if (HADOOP_RM_STATE_STANDBY.equals(state)) {
                state = getResourceManagerState(String.format(yarnUrl, rmIdArr[1]));
                if (HADOOP_RM_STATE_ACTIVE.equals(state)) {
                    return rmIdArr[1];
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            state = getResourceManagerState(String.format(yarnUrl, rmIdArr[1]));
            if (HADOOP_RM_STATE_ACTIVE.equals(state)) {
                return rmIdArr[0];
            }
        }

        return null;
    }

    public static String getResourceManagerState(String url) {

        String retStr = HttpUtils.get(url);
        if (StringUtils.isEmpty(retStr)) {
            return null;
        }

        JsonNode node = JSONUtils.parseNode(retStr);
        if (node != null) {
            JsonNode clusterInfo = JSONUtils.findNode(node,"clusterInfo");
            if (clusterInfo != null) {
                return JSONUtils.findValue(clusterInfo, "haState");
            }
        }

        return null;
    }

    public static String getYarnAppId(String user, String tags) {
        try {
            String applicationUrl = getApplicationUrl(null)
                    .split("/apps")[0]
                    + "/apps?"
                    + "user="+user
                    + "&applicationTags="+tags
                    + "&limit=1";
            logger.info("get yarn application id url : {}", applicationUrl);
            String responseContent = HttpUtils.get(applicationUrl);
            JsonNode node = JSONUtils.parseNode(responseContent);
            if (node != null) {
                return node.findValue("apps").findValue("app").findValue("id").textValue();
            } else {
                return null;
            }

        } catch (Exception e) {
            logger.warn("yarn application get applicationId error: {}", e.getMessage());
            return null;
        }
    }

    public static boolean isYarnEnabled(){
        return yarnEnabled;
    }

    /**
     * get the state of an application
     *
     * @param applicationId application id
     * @return the return may be null or there may be other parse exceptions
     */
    public static ExecutionStatus getApplicationStatus(String applicationId){
        if (StringUtils.isEmpty(applicationId)) {
            return null;
        }

        String applicationUrl = getApplicationUrl(applicationId);
        String responseContent = HttpUtils.get(applicationUrl);
        String result = "";
        JsonNode node = JSONUtils.parseNode(responseContent);
        if (node != null) {
            JsonNode app = JSONUtils.findNode(node,"app");
            if (app != null) {
                result =  JSONUtils.findValue(app, "finalStatus");
            }
        }

        if (StringUtils.isEmpty(result)) {
            return ExecutionStatus.RUNNING_EXECUTION;
        }

        switch (result) {
            case CommonConstants.ACCEPTED:
                return ExecutionStatus.SUBMITTED_SUCCESS;
            case CommonConstants.SUCCEEDED:
                return ExecutionStatus.SUCCESS;
            case CommonConstants.NEW:
            case CommonConstants.NEW_SAVING:
            case CommonConstants.SUBMITTED:
            case CommonConstants.FAILED:
                return ExecutionStatus.FAILURE;
            case CommonConstants.KILLED:
                return ExecutionStatus.KILL;
            case CommonConstants.RUNNING:
            default:
                return ExecutionStatus.RUNNING_EXECUTION;
        }
    }

    public static boolean isSuccessOfYarnState(String appId) {
        boolean result = true;
        try {
            while(Stopper.isRunning()){
                ExecutionStatus applicationStatus = YarnUtils.getApplicationStatus(appId);
                if (applicationStatus != null) {
                    logger.info("appId:{}, final state:{}",appId,applicationStatus.name());
                    if (applicationStatus.equals(ExecutionStatus.FAILURE) ||
                            applicationStatus.equals(ExecutionStatus.KILL)) {
                        return false;
                    }

                    if (applicationStatus.equals(ExecutionStatus.SUCCESS)){
                        break;
                    }
                }

                Thread.sleep(CommonConstants.SLEEP_TIME_MILLIS);
            }
        } catch (Exception e) {
            logger.error(String.format("yarn applications: %s  status failed ", appId),e);
            result = false;
        }
        return result;
    }


}
