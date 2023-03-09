/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.util;

import static org.apache.griffin.core.job.entity.LivySessionStates.State.DEAD;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.LivySessionStates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class YarnNetUtil {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(YarnNetUtil.class);
    private static RestTemplate restTemplate = new RestTemplate();

    /**
     * delete app task scheduling by yarn.
     *
     * @param url   prefix part of whole url
     * @param appId application id
     */
    public static void delete(String url, String appId) {
        try {
            if (appId != null) {
                LOGGER.info("{} will delete by yarn", appId);
                restTemplate.put(url + "ws/v1/cluster/apps/"
                        + appId + "/state",
                    "{\"state\": \"KILLED\"}");
            }
        } catch (HttpClientErrorException e) {
            LOGGER.warn("client error {} from yarn: {}",
                e.getMessage(), e.getResponseBodyAsString());
        } catch (Exception e) {
            LOGGER.error("delete exception happens by yarn. {}", e);
        }
    }

    /**
     * update app task scheduling by yarn.
     *
     * @param url      prefix part of whole url
     * @param instance job instance
     * @return
     */
    public static boolean update(String url, JobInstanceBean instance) {
        try {
            url += "/ws/v1/cluster/apps/" + instance.getAppId();
            String result = restTemplate.getForObject(url, String.class);
            JsonObject state = parse(result);
            if (state != null) {
                instance.setState(LivySessionStates.toLivyState(state));
            }
            return true;
        } catch (HttpClientErrorException e) {
            LOGGER.warn("client error {} from yarn: {}",
                e.getMessage(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // in sync with Livy behavior, see com.cloudera.livy.utils.SparkYarnApp
                instance.setState(DEAD);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("update exception happens by yarn. {}", e);
        }
        return false;
    }

    /**
     * parse json string and get app json object.
     *
     * @param json json string
     * @return
     */
    public static JsonObject parse(String json) {
        if (StringUtils.isEmpty(json)) {
            LOGGER.warn("Input string is empty.");
            return null;
        }
        JsonParser parser = new JsonParser();
        return parser.parse(json).getAsJsonObject().getAsJsonObject("app");
    }
}

