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

package org.apache.griffin.core.metric;

import org.apache.griffin.core.metric.model.MetricValue;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RestClient.class, RestClientBuilder.class})
@PowerMockIgnore("javax.management.*")
public class MetricStoreImplTest {

    private static final String INDEX = "griffin";
    private static final String TYPE = "accuracy";

    private static final String urlBase = String.format("/%s/%s", INDEX, TYPE);
    private static final String urlGet = urlBase.concat("/_search?filter_path=hits.hits._source");

    private RestClient restClientMock;

    @Before
    public void setup(){
        PowerMockito.mockStatic(RestClient.class);
        restClientMock = PowerMockito.mock(RestClient.class);
        RestClientBuilder restClientBuilderMock = PowerMockito.mock(RestClientBuilder.class);

        given(RestClient.builder(anyVararg())).willReturn(restClientBuilderMock);
        given(restClientBuilderMock.build()).willReturn(restClientMock);
    }

    @Test
    public void testBuildBasicAuthString()
            throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method m = MetricStoreImpl.class.getDeclaredMethod
                ("buildBasicAuthString", String.class,
                        String.class);
        m.setAccessible(true);
        String authStr = (String) m.invoke(null, "user", "password");
        assertTrue(authStr.equals("Basic dXNlcjpwYXNzd29yZA=="));
    }

    @Test
    public void testMetricGetting() throws IOException, URISyntaxException {
        //given
        Response responseMock = PowerMockito.mock(Response.class);
        HttpEntity httpEntityMock = PowerMockito.mock(HttpEntity.class);
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("metricvalue.json");
        Map<String, String> map = new HashMap<>();
        map.put("q", "metadata.applicationId:application_1549876136110_0018");

        Map<String, Object> value = new HashMap<String, Object>(){{
            put("total", 74);
            put("miss", 0);
            put("matched", 74);
            put("matchedFraction", 1);
        }};
        MetricValue expectedMetric = new MetricValue("de_demo_results_comparision",
                1549985089648L,
                Collections.singletonMap("applicationId", "application_1549876136110_0018"),
                value);


        given(restClientMock.performRequest(eq("GET"), eq(urlGet), eq(map), anyVararg())).willReturn(responseMock);
        given(responseMock.getEntity()).willReturn(httpEntityMock);
        given(httpEntityMock.getContent()).willReturn(is);

        //when
        MetricStoreImpl metricStore = new MetricStoreImpl("localhost", 0, "", "", "");
        MetricValue metric = metricStore.getMetric("application_1549876136110_0018");

        //then
        //PowerMockito.verifyStatic();
        assertEquals(expectedMetric, metric);
    }

}
