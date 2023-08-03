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

package com.netease.arctic.server.dashboard.controller;

import com.alibaba.fastjson.JSONObject;
import com.netease.arctic.server.ArcticManagementConf;
import com.netease.arctic.server.dashboard.response.OkResponse;
import com.netease.arctic.server.utils.Configurations;
import io.javalin.http.Context;

import java.io.Serializable;

/**
 * login controller.

 */
public class LoginController {

  private final String adminUser;
  private final String adminPassword;

  public LoginController(Configurations serviceConfig) {
    adminUser = serviceConfig.get(ArcticManagementConf.ADMIN_USERNAME);
    adminPassword = serviceConfig.get(ArcticManagementConf.ADMIN_PASSWORD);
  }

  /**
   * getRuntime current user.
   */
  public void getCurrent(Context ctx) {
    SessionInfo user = ctx.sessionAttribute("user");
    ctx.json(OkResponse.of(user));
  }

  /**
   * handle login post request.
   **/
  public void login(Context ctx) {
    // ok
    JSONObject postBody = ctx.bodyAsClass(JSONObject.class);
    if (adminUser.equals(postBody.get("user")) &&
        (adminPassword.equals(postBody.get("password")))) {
      ctx.sessionAttribute(
          "user", new SessionInfo(adminUser, System.currentTimeMillis() + ""));
      ctx.json(OkResponse.of("success"));
    } else {
      throw new RuntimeException("bad user " + postBody.get("user") + " or password!");
    }
  }

  static class SessionInfo implements Serializable {
    String userName;
    String loginTime;

    public SessionInfo(String username, String loginTime) {
      this.userName = username;
      this.loginTime = loginTime;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public String getLoginTime() {
      return loginTime;
    }

    public void setLoginTime(String loginTime) {
      this.loginTime = loginTime;
    }
  }
}
