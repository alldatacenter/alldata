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

import com.netease.arctic.server.dashboard.model.VersionInfo;
import com.netease.arctic.server.dashboard.response.OkResponse;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/** optimize controller.
 * @Description: getRuntime version and commitTime
 */
public class VersionController {
  private static final Logger LOG = LoggerFactory.getLogger(VersionController.class);

  /**
   * getRuntime versionInfo.
   */
  public void getVersionInfo(Context ctx) {
    Properties prop = new Properties();
    InputStream is = VersionController.class.getClassLoader().getResourceAsStream("arctic/git.properties");
    String version = "UNKNOWN";
    String commitTime = "UNKNOWN";
    if (is != null) {
      try {
        prop.load(is);
        version = prop.getProperty("git.build.version");
        commitTime = prop.getProperty("git.commit.time");
      } catch (Exception e) {
        LOG.warn("Failed to find git.properties.");
      }
    }

    VersionInfo versionInfo = new VersionInfo();
    versionInfo.setVersion(version);
    versionInfo.setCommitTime(commitTime);
    ctx.json(OkResponse.of(versionInfo));
  }
}
