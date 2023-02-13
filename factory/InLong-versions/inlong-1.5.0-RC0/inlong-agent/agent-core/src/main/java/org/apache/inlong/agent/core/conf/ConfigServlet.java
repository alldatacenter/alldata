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

package org.apache.inlong.agent.core.conf;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handle config http request
 */
public class ConfigServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigServlet.class);

    private static final String CONTENT_TYPE = "application/json";
    private static final String CHARSET_TYPE = "UTF-8";
    private static final String JOB_TYPE = "job";
    private static final String AGENT_TYPE = "agent";
    private static final String TRIGGER_TYPE = "trigger";
    private static final String OP_TYPE = "op";
    private static final String ADD_OP = "add";
    private static final String DELETE_OP = "delete";
    private final Gson gson = new Gson();
    private final ConfigJetty configHandler;

    public ConfigServlet(ConfigJetty configHandler) {
        this.configHandler = configHandler;
    }

    /**
     * write http response
     *
     * @param response HttpServletResponse
     * @param result ResponseResult
     * @throws IOException
     */
    public void responseToJson(HttpServletResponse response,
            ResponseResult result) throws IOException {
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(CHARSET_TYPE);
        String jsonStr = gson.toJson(result);
        PrintWriter out = response.getWriter();
        out.print(jsonStr);
        out.flush();
    }

    /**
     * handle path of "/config/job"
     *
     * @param jobProfileStr job profile string
     */
    private void handleJob(String jobProfileStr) {
        JobProfile jobProfile = JobProfile.parseJsonStr(jobProfileStr);
        String op = jobProfile.get(OP_TYPE);
        if (ADD_OP.equals(op)) {
            configHandler.storeJobConf(jobProfile);
        } else if (DELETE_OP.equals(op)) {
            configHandler.deleteJobConf(jobProfile);
        }
    }

    private void handleAgent(String agentProfileStr) {
        // TODO: handle agent
    }

    /**
     * handle post requests.
     *
     * @param req request
     * @param resp response
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        ResponseResult responseResult = new ResponseResult(0, "");

        try (BufferedReader reader = req.getReader()) {
            String configJsonStr = IOUtils.toString(reader);
            LOGGER.info("Getting request {}", configJsonStr);
            // path is "/config/job"
            if (pathInfo.endsWith(JOB_TYPE)) {
                handleJob(configJsonStr);
            } else if (pathInfo.endsWith(AGENT_TYPE)) {
                handleAgent(configJsonStr);
            } else {
                responseResult.setCode(-1).setMessage("child path is not correct");
            }
        } catch (Exception ex) {
            LOGGER.error("error while handle post", ex);
            responseResult.setCode(-1).setMessage(ex.getMessage());
        }
        responseToJson(resp, responseResult);
    }
}
