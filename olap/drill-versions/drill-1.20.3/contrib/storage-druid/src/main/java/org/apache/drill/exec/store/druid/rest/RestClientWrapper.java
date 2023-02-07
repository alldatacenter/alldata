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
package org.apache.drill.exec.store.druid.rest;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

public class RestClientWrapper implements RestClient {
  // OkHttp client is designed to be shared across threads.
  private final OkHttpClient httpClient = new OkHttpClient();

  public Response get(String url) throws IOException {
    Request get = new Request.Builder()
      .url(url)
      .addHeader("Content-Type", "application/json")
      .build();

    return httpClient.newCall(get).execute();
  }

  public Response post(String url, String body) throws IOException {
    RequestBody postBody = RequestBody.create(body.getBytes(StandardCharsets.UTF_8));

    Request post = new Request.Builder()
      .url(url)
      .addHeader("Content-Type", "application/json")
      .post(postBody)
      .build();

    return httpClient.newCall(post).execute();
  }
}
