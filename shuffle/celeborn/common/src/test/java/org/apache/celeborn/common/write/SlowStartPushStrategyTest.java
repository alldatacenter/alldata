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

import org.junit.Assert;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;

public class SlowStartPushStrategyTest {

  private final CelebornConf conf = new CelebornConf();

  @Test
  public void testSleepTime() {
    conf.set(CelebornConf.CLIENT_PUSH_MAX_REQS_IN_FLIGHT_PERWORKER().key(), "32");
    conf.set(CelebornConf.CLIENT_PUSH_LIMIT_STRATEGY().key(), "slowstart");
    conf.set(CelebornConf.CLIENT_PUSH_SLOW_START_MAX_SLEEP_TIME().key(), "3s");
    SlowStartPushStrategy strategy = (SlowStartPushStrategy) PushStrategy.getStrategy(conf);
    String dummyHostPort = "test:9087";
    SlowStartPushStrategy.CongestControlContext context =
        strategy.getCongestControlContextByAddress(dummyHostPort);

    // If the currentReq is 0, not throw error
    strategy.getSleepTime(context);

    // If the currentReq is 1, should sleep 440 ms
    Assert.assertEquals(440, strategy.getSleepTime(context));

    // If the currentReq is 8, should sleep 20 ms
    for (int i = 0; i < 7; i++) {
      strategy.onSuccess(dummyHostPort);
    }
    Assert.assertEquals(20, strategy.getSleepTime(context));

    // If the currentReq is 16, should sleep 0 ms
    for (int i = 0; i < 8; i++) {
      strategy.onSuccess(dummyHostPort);
    }
    Assert.assertEquals(0, strategy.getSleepTime(context));

    // Congest the requests, the currentReq reduced to 8, should sleep 20 ms
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(20, strategy.getSleepTime(context));

    // Congest the requests, the currentReq reduced to 4, should sleep 20 ms
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(260, strategy.getSleepTime(context));
    // Congest the requests, the currentReq reduced to 2, should sleep 20 ms
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(380, strategy.getSleepTime(context));
    // Congest the requests, the currentReq reduced to 1, should sleep 20 ms
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(440, strategy.getSleepTime(context));
    // Keep congest the requests even the currentReq reduced to 1, will increase the sleep time 1s
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(1440, strategy.getSleepTime(context));
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(2440, strategy.getSleepTime(context));

    // Cannot exceed the max sleep time
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(3000, strategy.getSleepTime(context));

    // If start to return success, the currentReq is increased to 2, should sleep 380 ms
    strategy.onSuccess(dummyHostPort);
    Assert.assertEquals(380, strategy.getSleepTime(context));
  }

  @Test
  public void testCongestStrategy() {
    conf.set(CelebornConf.CLIENT_PUSH_MAX_REQS_IN_FLIGHT_PERWORKER().key(), "5");
    conf.set(CelebornConf.CLIENT_PUSH_LIMIT_STRATEGY().key(), "slowstart");
    conf.set(CelebornConf.CLIENT_PUSH_SLOW_START_MAX_SLEEP_TIME().key(), "4s");
    SlowStartPushStrategy strategy = (SlowStartPushStrategy) PushStrategy.getStrategy(conf);
    String dummyHostPort = "test:9087";
    // Slow start, should exponentially increase the currentReq
    strategy.onSuccess(dummyHostPort);
    Assert.assertEquals(2, strategy.getCurrentMaxReqsInFlight(dummyHostPort));
    strategy.onSuccess(dummyHostPort);
    strategy.onSuccess(dummyHostPort);
    Assert.assertEquals(4, strategy.getCurrentMaxReqsInFlight(dummyHostPort));
    strategy.onSuccess(dummyHostPort);
    strategy.onSuccess(dummyHostPort);
    strategy.onSuccess(dummyHostPort);
    strategy.onSuccess(dummyHostPort);

    // Will linearly increase the currentReq if meet the maxReqsInFlight
    Assert.assertEquals(5, strategy.getCurrentMaxReqsInFlight(dummyHostPort));
    strategy.onSuccess(dummyHostPort);
    strategy.onSuccess(dummyHostPort);
    strategy.onSuccess(dummyHostPort);
    strategy.onSuccess(dummyHostPort);
    strategy.onSuccess(dummyHostPort);
    Assert.assertEquals(6, strategy.getCurrentMaxReqsInFlight(dummyHostPort));

    // Congest controlled, should half the currentReq
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(3, strategy.getCurrentMaxReqsInFlight(dummyHostPort));
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(1, strategy.getCurrentMaxReqsInFlight(dummyHostPort));

    // Cannot lower than 1
    strategy.onCongestControl(dummyHostPort);
    Assert.assertEquals(1, strategy.getCurrentMaxReqsInFlight(dummyHostPort));
  }

  @Test
  public void testMultiHosts() {
    conf.set(CelebornConf.CLIENT_PUSH_MAX_REQS_IN_FLIGHT_PERWORKER().key(), "3");
    conf.set(CelebornConf.CLIENT_PUSH_LIMIT_STRATEGY().key(), "slowstart");
    conf.set(CelebornConf.CLIENT_PUSH_SLOW_START_MAX_SLEEP_TIME().key(), "3s");
    SlowStartPushStrategy strategy = (SlowStartPushStrategy) PushStrategy.getStrategy(conf);
    String dummyHostPort1 = "test1:9087";
    String dummyHostPort2 = "test2:9087";
    SlowStartPushStrategy.CongestControlContext context1 =
        strategy.getCongestControlContextByAddress(dummyHostPort1);
    SlowStartPushStrategy.CongestControlContext context2 =
        strategy.getCongestControlContextByAddress(dummyHostPort2);

    Assert.assertEquals(440, strategy.getSleepTime(context1));
    Assert.assertEquals(440, strategy.getSleepTime(context2));

    // Control the dummyHostPort1, should not affect dummyHostPort2
    for (int i = 0; i < 3; i++) {
      strategy.onSuccess(dummyHostPort1);
    }
    Assert.assertEquals(0, strategy.getSleepTime(context1));
    Assert.assertEquals(440, strategy.getSleepTime(context2));
  }
}
