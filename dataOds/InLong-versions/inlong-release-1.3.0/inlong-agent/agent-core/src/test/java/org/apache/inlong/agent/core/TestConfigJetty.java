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

package org.apache.inlong.agent.core;

import static org.apache.inlong.agent.constant.AgentConstants.AGENT_CONF_PARENT;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_ENABLE_HTTP;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_HTTP_PORT;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_AGENT_HTTP_PORT;

import com.google.gson.Gson;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.core.conf.ResponseResult;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestConfigJetty {

    private static CloseableHttpClient httpClient;
    private static AgentConfiguration configuration;
    private static final Gson gson = new Gson();
    private static AgentManager manager;
    private static AgentBaseTestsHelper helper;

    @BeforeClass
    public static void setup() {
        configuration = AgentConfiguration.getAgentConf();
        configuration.setBoolean(AGENT_ENABLE_HTTP, true);
        helper = new AgentBaseTestsHelper(TestConfigJetty.class.getName()).setupAgentHome();
        manager = new AgentManager();
        RequestConfig requestConfig = RequestConfig.custom().build();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        httpClient = httpClientBuilder.build();
    }

    @Test
    public void testJobConfig() throws Exception {
        int webPort = configuration.getInt(AGENT_HTTP_PORT, DEFAULT_AGENT_HTTP_PORT);
        String url = "http://localhost:" + webPort + "/config/";
        HttpPost httpPost = new HttpPost(url);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        String returnStr = EntityUtils.toString(response.getEntity());
        ResponseResult responseResult = gson.fromJson(returnStr, ResponseResult.class);
        Assert.assertEquals(-1, responseResult.getCode());
        Assert.assertEquals("child path is not correct", responseResult.getMessage());

        url = "http://localhost:" + webPort + "/config/job";
        httpPost = new HttpPost(url);

        String jsonStr = "{\n"
                + "  \"op\": 0,\n"
                + "  \"job\": {\n"
                + "    \"fileJob\": {\n"
                + "    \"trigger\": \"org.apache.inlong.agent.plugin.trigger.DirectoryTrigger\",\n"
                + "    \"file\": {\n"
                + "      \"max\": {\n"
                + "        \"wait\": 1\n"
                + "      }\n"
                + "    },\n"
                + "    \"dir\": {\n"
                + "      \"path\": \"\",\n"
                + "      \"pattern\": \"/test.[0-9]\"\n"
                + "    }\n"
                + "    },\n"
                + "    \"deliveryTime\": \"12313123\",\n"
                + "    \"id\": 1,\n"
                + "    \"name\": \"fileAgentTest\",\n"
                + "    \"source\": \"org.apache.inlong.agent.plugin.sources.TextFileSource\",\n"
                + "    \"sink\": \"org.apache.inlong.agent.plugin.sinks.MockSink\",\n"
                + "    \"channel\": \"org.apache.inlong.agent.plugin.channel.MemoryChannel\",\n"
                + "    \"pattern\": \"test\",\n"
                + "  \"op\": 0\n"
                + "  }\n"
                + "}";
        StringEntity entity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);

        response = httpClient.execute(httpPost);
        returnStr = EntityUtils.toString(response.getEntity());
        responseResult = gson.fromJson(returnStr, ResponseResult.class);
        Assert.assertEquals(0, responseResult.getCode());
    }

    @Test
    public void testAgentConfig() throws Exception {
        int webPort = configuration.getInt(AGENT_HTTP_PORT, DEFAULT_AGENT_HTTP_PORT);
        configuration.set(AGENT_CONF_PARENT, helper.getTestRootDir().toString());
        String url = "http://localhost:" + webPort + "/config/agent";
        HttpPost httpPost = new HttpPost(url);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        String returnStr = EntityUtils.toString(response.getEntity());
        ResponseResult responseResult = gson.fromJson(returnStr, ResponseResult.class);
        Assert.assertEquals(0, responseResult.getCode());
    }

    @AfterClass
    public static void teardown() throws Exception {
        httpClient.close();
        manager.stop();
        helper.teardownAgentHome();
    }
}
