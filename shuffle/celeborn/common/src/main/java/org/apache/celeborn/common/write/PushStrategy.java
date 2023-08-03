/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.write;

import java.io.IOException;

import org.apache.celeborn.common.CelebornConf;

public abstract class PushStrategy {

  protected final CelebornConf conf;

  public static PushStrategy getStrategy(CelebornConf conf) {
    String strategyName = conf.clientPushLimitStrategy();
    switch (strategyName) {
      case "SIMPLE":
        return new SimplePushStrategy(conf);
      case "SLOWSTART":
        return new SlowStartPushStrategy(conf);
      default:
        throw new IllegalArgumentException("The strategy " + strategyName + " is not supported!");
    }
  }

  public PushStrategy(CelebornConf conf) {
    this.conf = conf;
  }

  /** Handle the response is successful. */
  public abstract void onSuccess(String hostAndPushPort);

  /** Handle the response is congested controlled. */
  public abstract void onCongestControl(String hostAndPushPort);

  public abstract void clear();

  /** Control the push speed to meet the requirement. */
  public abstract void limitPushSpeed(PushState pushState, String hostAndPushPort)
      throws IOException;

  public abstract int getCurrentMaxReqsInFlight(String hostAndPushPort);
}
