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
package org.apache.atlas.web.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.atlas.AtlasBaseClient;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.web.model.DebugMetrics;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class DebugMetricsIT extends BaseResourceIT {

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void checkMetricCountIncrement() {

        // Get the metrics
        AtlasBaseClient.API metricsAPI = new AtlasBaseClient.API(AtlasBaseClient.BASE_URI + "admin/debug/metrics", HttpMethod.GET, Response.Status.OK);
        try {
            String metricsJson = atlasClientV1.callAPI(metricsAPI, String.class, null);
            HashMap<String, DebugMetrics> currentMetrics = AtlasJson.fromJson(metricsJson, new TypeReference<HashMap<String, DebugMetrics>>() {});
            DebugMetrics currentCreateOrUpdateDTO = currentMetrics.get("EntityREST_createOrUpdate");
            long currentCreateOrUpdateCount = 0;
            if(currentCreateOrUpdateDTO != null) {
                currentCreateOrUpdateCount = currentCreateOrUpdateDTO.getNumops();
            }

            // hit the api
            AtlasEntity atlasEntity = createEntity(DATABASE_TYPE_BUILTIN, randomString());
            atlasClientV2.createEntity(new AtlasEntity.AtlasEntityWithExtInfo(atlasEntity));

            atlasEntity = createEntity(DATABASE_TYPE_BUILTIN, randomString());
            atlasClientV2.createEntity(new AtlasEntity.AtlasEntityWithExtInfo(atlasEntity));

            // get the metrics again
            Thread.sleep(30000); // The metrics take some time to update
            metricsJson = atlasClientV1.callAPI(metricsAPI, String.class, null);
            HashMap<String, DebugMetrics> newMetrics = AtlasJson.fromJson(metricsJson, new TypeReference<HashMap<String, DebugMetrics>>() {});
            DebugMetrics newCreateOrUpdateDTO = newMetrics.get("EntityREST_createOrUpdate");

            // check if the metric count has increased
            long newCreateOrUpdateCount = 0;
            if(newCreateOrUpdateDTO != null) {
                newCreateOrUpdateCount = newCreateOrUpdateDTO.getNumops();
            }
            assertEquals(newCreateOrUpdateCount, (currentCreateOrUpdateCount + 2), "Count didn't increase after making API call");
        } catch (Exception e) {
            fail("Caught exception while running the test: " + e.getMessage(), e);
        }
    }
}
