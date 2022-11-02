/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.funtest.server.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * Simple test to get the list of stacks. Does not touch the DB.
 */
@Ignore
public class GetStacksTest extends ServerTestBase {
  /**
   * Waits for the ambari server to startup and then checks it's
   * status by querying /api/v1/stacks (does not touch the DB)
   */
  @Test
  public void testServerStatus() throws IOException {
    /**
     * Query the ambari server for the list of stacks.
     * A successful GET returns the list of stacks.
     * We should get a json like:
     * {
     *   "href" : "http://localhost:9995/api/v1/stacks",
     *   "items" : [
     *   {
     *     "href" : "http://localhost:9995/api/v1/stacks/HDP",
     *     "Stacks" : {
     *     "stack_name" : "HDP"
     *     }
     *   }
     *  ]
     * }
     */

    /**
     * Test URL for GETting the status of the ambari server
     */
    String stacksPath = "/api/v1/stacks";
    String stacksUrl = String.format(SERVER_URL_FORMAT, serverPort) + stacksPath;
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet httpGet = new HttpGet(stacksUrl);
    httpGet.addHeader("Authorization", getBasicAdminAuthentication());
    httpGet.addHeader("X-Requested-By", "ambari");

    try {
      HttpResponse httpResponse = httpClient.execute(httpGet);
      int statusCode = httpResponse.getStatusLine().getStatusCode();

      assertEquals(HttpStatus.SC_OK, statusCode); // HTTP status code 200

      HttpEntity entity = httpResponse.getEntity();
      String responseBody = entity != null ? EntityUtils.toString(entity) : null;

      assertTrue(responseBody != null); // Make sure response body is valid

      JsonElement jsonElement = new JsonParser().parse(new JsonReader(new StringReader(responseBody)));

      assertTrue (jsonElement != null); // Response was a JSON string

      JsonObject jsonObject = jsonElement.getAsJsonObject();

      assertTrue (jsonObject.has("items"));  // Should have "items" entry

      JsonArray stacksArray = jsonObject.get("items").getAsJsonArray();

      assertTrue (stacksArray.size() > 0); // Should have at least one stack

    } finally {
      httpClient.close();
    }
  }
}
