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

package com.netease.arctic.server.dashboard.model;

public class ApiTokens {
  Integer id;
  String apikey;
  String secret;
  String applyTime;

  public ApiTokens() {
  }

  public ApiTokens(String apiKey, String secret) {
    this.apikey = apiKey;
    this.secret = secret;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getApikey() {
    return apikey;
  }

  public void setApikey(String apikey) {
    this.apikey = apikey;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getApplyTime() {
    return applyTime;
  }

  public void setApplyTime(String applyTime) {
    this.applyTime = applyTime;
  }
}
