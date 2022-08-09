/**
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

package org.apache.ambari.view.pig.templeton.client;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.utils.MisconfigurationFormattedException;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempletonApiFactory {
  private final static Logger LOG =
      LoggerFactory.getLogger(TempletonApiFactory.class);

  private ViewContext context;
  private AmbariApi ambariApi;

  public TempletonApiFactory(ViewContext context) {
    this.context = context;
    this.ambariApi = new AmbariApi(context);
  }

  public TempletonApi connectToTempletonApi() {
    String webhcatUrl = ambariApi.getServices().getWebHCatURL();
    return new TempletonApi(webhcatUrl, getTempletonUser(context), context);
  }

  /**
   * Extension point to use different usernames in templeton
   * requests instead of logged in user
   * @return username in templeton
   */
  private String getTempletonUser(ViewContext context) {
    String username = context.getProperties().get("webhcat.username");
    if (username == null || username.compareTo("null") == 0 || username.compareTo("") == 0) {
      username = context.getUsername();
    }
    return username;
  }
}
