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

import org.apache.drill.yarn.core.YarnClientException;
import org.apache.drill.yarn.core.YarnRMClient;
import org.apache.hadoop.yarn.api.records.ApplicationId;

public class KillCommand extends ClientCommand {

  @Override
  public void run() throws ClientException {
    ApplicationId appId = checkAppId();
    if (appId == null) {
      System.exit(-1);
    }
    YarnRMClient client = new YarnRMClient(appId);
    try {
      client.killApplication();
    } catch (YarnClientException e) {
      throw new ClientException(e);
    }
    System.out.println("Kill request sent, waiting for shut-down.");
    try {
      client.waitForCompletion();
    } catch (YarnClientException e) {
      throw new ClientException(
          "Wait for completion failed for app id: " + appId.toString(), e);
    }
    System.out.println("Application completed: " + appId.toString());
  }

}
