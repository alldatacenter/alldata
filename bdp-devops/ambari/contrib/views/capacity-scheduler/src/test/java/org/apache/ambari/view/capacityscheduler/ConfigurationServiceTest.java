/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.capacityscheduler;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.easymock.EasyMock;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;


public class ConfigurationServiceTest {
    private ViewContext context;
    private HttpHeaders httpHeaders;
    private UriInfo uriInfo;
    private Cluster ambariCluster;
    private Map<String, String> properties;
    private ConfigurationService configurationService;

    public static final String BASE_URI = "http://localhost:8084/myapp/";


    @Before
    public void setUp() throws Exception {
        context = createNiceMock(ViewContext.class);
        httpHeaders = createNiceMock(HttpHeaders.class);
        ambariCluster = createNiceMock(Cluster.class);

        EasyMock.expect(ambariCluster.getConfigurationValue("ranger-yarn-plugin-properties", "ranger-yarn-plugin-enabled")).andReturn("Yes").anyTimes();
        EasyMock.expect(context.getCluster()).andReturn(ambariCluster).anyTimes();
        EasyMock.expect(context.getProperties()).andReturn(properties).anyTimes();
        EasyMock.replay(context);
        EasyMock.replay(ambariCluster);
        System.out.println("context.getProperties() : " + context.getProperties());
        configurationService = new ConfigurationService(context);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRightConfigurationValue() {
        Response response = configurationService.getConfigurationValue("ranger-yarn-plugin-properties", "ranger-yarn-plugin-enabled");
        JSONObject jsonObject = (JSONObject) response.getEntity();
        JSONArray arr = (JSONArray) jsonObject.get("configs");
        Assert.assertEquals(arr.size(), 1);
        JSONObject obj = (JSONObject) arr.get(0);

        Assert.assertEquals(obj.get("siteName"), "ranger-yarn-plugin-properties");
        Assert.assertEquals(obj.get("configName"), "ranger-yarn-plugin-enabled");
        Assert.assertEquals(obj.get("configValue"), "Yes"); // because I set it myself.
    }

    @Test
    public void testExceptionOnWrongConfigurationValue() {
        Response response = configurationService.getConfigurationValue("random-site", "random-key");
        JSONObject jsonObject = (JSONObject) response.getEntity();
        JSONArray arr = (JSONArray) jsonObject.get("configs");
        Assert.assertEquals(arr.size(), 1);
        JSONObject obj = (JSONObject) arr.get(0);

        Assert.assertEquals(obj.get("siteName"), "random-site");
        Assert.assertEquals(obj.get("configName"), "random-key");
        Assert.assertEquals(obj.get("configValue"), null);
    }
}
