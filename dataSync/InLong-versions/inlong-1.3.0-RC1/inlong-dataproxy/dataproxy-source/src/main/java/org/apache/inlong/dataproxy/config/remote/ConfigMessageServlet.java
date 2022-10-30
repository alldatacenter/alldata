/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.config.remote;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.http.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http
 */
public class ConfigMessageServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigMessageServlet.class);
    private static final ConfigManager configManager = ConfigManager.getInstance();

    private final Gson gson = new Gson();

    public ConfigMessageServlet() {
    }

    @Override
    protected void doGet(
            HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    private boolean handleTopicConfig(RequestContent requestContent) {
        Map<String, String> groupIdToTopic = new HashMap<String, String>();
        for (Map<String, String> item : requestContent.getContent()) {
            groupIdToTopic.put(item.get("inlongGroupId"), item.get("topic"));
        }
        if ("add".equals(requestContent.getOperationType())) {
            return configManager.addTopicProperties(groupIdToTopic);
        } else if ("delete".equals(requestContent.getOperationType())) {
            return configManager.deleteTopicProperties(groupIdToTopic);
        }
        return false;
    }

    private boolean handleMxConfig(RequestContent requestContent) {
        Map<String, String> groupIdToMValue = new HashMap<String, String>();
        for (Map<String, String> item : requestContent.getContent()) {
            groupIdToMValue.put(item.get("inlongGroupId"), item.get("m"));
        }
        if ("add".equals(requestContent.getOperationType())) {
            return configManager.addMxProperties(groupIdToMValue);
        } else if ("delete".equals(requestContent.getOperationType())) {
            return configManager.deleteMxProperties(groupIdToMValue);
        }
        return false;
    }

    private void responseToJson(HttpServletResponse response,
                                ResponseResult result) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String jsonStr = gson.toJson(result);
        PrintWriter out = response.getWriter();
        out.print(jsonStr);
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ResponseResult result = new ResponseResult(StatusCode.SERVICE_ERR, "");
        BufferedReader reader = null;
        try {
            reader = req.getReader();
            boolean isSuccess = false;
            RequestContent requestContent = gson.fromJson(IOUtils.toString(reader),
                    RequestContent.class);
            if (requestContent.getRequestType() != null
                    && requestContent.getOperationType() != null) {
                if ("topic".equals(requestContent.getRequestType())) {
                    isSuccess = handleTopicConfig(requestContent);
                } else if ("mx".equals(requestContent.getRequestType())) {
                    isSuccess = handleMxConfig(requestContent);
                }
            } else {
                result.setMessage("request format is not valid");
            }

            if (isSuccess) {
                result.setCode(StatusCode.SUCCESS);
            } else {
                result.setMessage("cannot operate config update, please check it");
            }

        } catch (Exception ex) {
            LOG.error("error while do post", ex);
            result.setMessage(ex.getMessage());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        responseToJson(resp, result);
    }

}
