/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.service;

import com.google.common.collect.Maps;
import com.netease.arctic.ams.api.InvalidObjectException;
import com.netease.arctic.ams.api.MetaException;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.OptimizerDescriptor;
import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.ConfigFileProperties;
import com.netease.arctic.ams.server.model.OptimizeQueueMeta;
import com.netease.arctic.ams.server.model.TableTaskStatus;
import com.netease.arctic.ams.server.service.impl.OptimizerService;
import com.netease.arctic.ams.server.utils.JDBCSqlSessionFactoryProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Map;

@PowerMockIgnore({"javax.management.*"})
@PrepareForTest({
    ServiceContainer.class,
    JDBCSqlSessionFactoryProvider.class,
    OptimizerService.class,
    ArcticMetaStore.class
})
public class TestOptimizerService {

  private static final String optimizerGroupName = "testGroup";
  private static final String containerName = "testContainer";

  @BeforeClass
  public static void before() throws MetaException, NoSuchObjectException {
    OptimizeQueueMeta optimizeQueueMeta = new OptimizeQueueMeta();
    optimizeQueueMeta.setName(optimizerGroupName);
    optimizeQueueMeta.setContainer(containerName);
    optimizeQueueMeta.setSchedulingPolicy(ConfigFileProperties.OPTIMIZE_SCHEDULING_POLICY_QUOTA);
    Map<String, String> properties = Maps.newHashMap();
    optimizeQueueMeta.setProperties(properties);
    ServiceContainer.getOptimizeQueueService().createQueue(optimizeQueueMeta);
  }

  @Test
  public void testRegisterOptimizer() throws MetaException, InvalidObjectException {
    OptimizerRegisterInfo optimizerRegisterInfo = new OptimizerRegisterInfo();
    optimizerRegisterInfo.setOptimizerGroupName(optimizerGroupName);
    optimizerRegisterInfo.setCoreNumber(1);
    optimizerRegisterInfo.setMemorySize(1);
    OptimizerDescriptor descriptor = ServiceContainer.getOptimizerService().registerOptimizer(optimizerRegisterInfo);
    int groupId = ServiceContainer.getOptimizerService().getOptimizerGroupInfo(optimizerGroupName).getId();
    Assert.assertEquals(descriptor.getGroupId(), groupId);
    Assert.assertEquals(descriptor.getGroupName(), optimizerGroupName);
    Assert.assertEquals(descriptor.getContainer(), containerName);
    Assert.assertEquals(descriptor.getStatus(), TableTaskStatus.STARTING.name());
  }
}
