/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.yarn.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

public class SimpleRestClient {
  public String send(String baseUrl, String resource, boolean isPost)
      throws ClientException {
    String url = baseUrl;
    if (!url.endsWith("/")) {
      url += "/";
    }
    url += resource;
    try {
      HttpClient client = new DefaultHttpClient();
      HttpRequestBase request;
      if (isPost) {
        request = new HttpPost(url);
      } else {
        request = new HttpGet(url);
      }

      HttpResponse response = client.execute(request);
      BufferedReader rd = new BufferedReader(
          new InputStreamReader(response.getEntity().getContent()));
      StringBuilder buf = new StringBuilder();
      String line = null;
      while ((line = rd.readLine()) != null) {
        buf.append(line);
      }
      return buf.toString().trim();
    } catch (ClientProtocolException e) {
      throw new ClientException("Internal REST error", e);
    } catch (IllegalStateException e) {
      throw new ClientException("Internal REST error", e);
    } catch (IOException e) {
      throw new ClientException("REST request failed: " + url, e);
    }
  }
}
