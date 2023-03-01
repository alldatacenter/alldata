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

package org.apache.celeborn.service.deploy.worker.congestcontrol;

import java.util.Map;

import scala.collection.JavaConverters;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.service.deploy.worker.WorkerSource;

public class TestCongestionController {

  private CongestionController controller;
  private WorkerSource source = new WorkerSource(new CelebornConf());

  private long pendingBytes = 0L;
  private final long userInactiveTimeMills = 2000L;

  @Before
  public void initialize() {
    // Make sampleTimeWindow a bit larger in case the tests run time exceed this window.
    controller =
        new CongestionController(source, 10, 1000, 500, userInactiveTimeMills) {
          @Override
          public long getTotalPendingBytes() {
            return pendingBytes;
          }

          @Override
          public void trimMemoryUsage() {
            // No op
          }
        };
  }

  @After
  public void clear() {
    controller.close();
    source.destroy();
  }

  @Test
  public void testSingleUser() {
    UserIdentifier userIdentifier = new UserIdentifier("test", "celeborn");

    Assert.assertFalse(controller.isUserCongested(userIdentifier));

    controller.produceBytes(userIdentifier, 1001);
    pendingBytes = 1001;
    Assert.assertTrue(controller.isUserCongested(userIdentifier));

    controller.consumeBytes(1001);
    pendingBytes = 0;
    Assert.assertFalse(controller.isUserCongested(userIdentifier));
  }

  @Test
  public void testMultipleUsers() {
    UserIdentifier user1 = new UserIdentifier("test", "celeborn");
    UserIdentifier user2 = new UserIdentifier("test", "spark");

    // Both users should not be congested at first
    Assert.assertFalse(controller.isUserCongested(user1));
    Assert.assertFalse(controller.isUserCongested(user2));

    // If pendingBytes exceed the high watermark, user1 produce speed > avg consume speed
    // While user2 produce speed < avg consume speed
    controller.produceBytes(user1, 800);
    controller.produceBytes(user2, 201);
    controller.consumeBytes(500);
    pendingBytes = 1001;
    Assert.assertTrue(controller.isUserCongested(user1));
    Assert.assertFalse(controller.isUserCongested(user2));

    // If both users higher than the avg consume speed, should congest them all.
    controller.produceBytes(user1, 800);
    controller.produceBytes(user2, 800);
    controller.consumeBytes(500);
    pendingBytes = 1600;
    Assert.assertTrue(controller.isUserCongested(user1));
    Assert.assertTrue(controller.isUserCongested(user2));

    // If pending bytes lower than the low watermark, should don't congest all users.
    pendingBytes = 0;
    Assert.assertFalse(controller.isUserCongested(user1));
    Assert.assertFalse(controller.isUserCongested(user2));
  }

  @Test
  public void testUserMetrics() throws InterruptedException {
    UserIdentifier user = new UserIdentifier("test", "celeborn");
    Assert.assertFalse(controller.isUserCongested(user));
    controller.produceBytes(user, 800);

    Assert.assertTrue(
        isGaugeExist(
            WorkerSource.UserProduceSpeed(),
            JavaConverters.mapAsJavaMapConverter(user.toMap()).asJava()));

    Thread.sleep(userInactiveTimeMills * 2);

    Assert.assertFalse(
        isGaugeExist(
            WorkerSource.UserProduceSpeed(),
            JavaConverters.mapAsJavaMapConverter(user.toMap()).asJava()));
  }

  private boolean isGaugeExist(String name, Map<String, String> labels) {
    return source.namedGauges().stream()
            .filter(
                gauge -> {
                  if (gauge.name().equals(name)) {
                    return labels.entrySet().stream()
                        .noneMatch(
                            entry -> {
                              // Filter entry not exist in the gauge's labels
                              if (gauge.labels().get(entry.getKey()).nonEmpty()) {
                                return !gauge
                                    .labels()
                                    .get(entry.getKey())
                                    .get()
                                    .equals(entry.getValue());
                              } else {
                                return true;
                              }
                            });
                  }
                  return false;
                })
            .count()
        == 1;
  }
}
