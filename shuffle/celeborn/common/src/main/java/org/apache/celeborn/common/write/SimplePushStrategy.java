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

/** A Simple strategy that control the push speed by a solid configure, pushMaxReqsInFlight. */
public class SimplePushStrategy extends PushStrategy {

  private final int maxInFlightPerWorker;

  public SimplePushStrategy(CelebornConf conf) {
    super(conf);
    this.maxInFlightPerWorker = conf.clientPushMaxReqsInFlightPerWorker();
  }

  @Override
  public void onSuccess(String hostAndPushPort) {
    // No op
  }

  @Override
  public void onCongestControl(String hostAndPushPort) {
    // No op
  }

  @Override
  public void clear() {
    // No op
  }

  @Override
  public void limitPushSpeed(PushState pushState, String hostAndPushPort) throws IOException {
    // No op
  }

  @Override
  public int getCurrentMaxReqsInFlight(String hostAndPushPort) {
    return maxInFlightPerWorker;
  }
}
