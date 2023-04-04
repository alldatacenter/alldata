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

package com.netease.arctic.ams.server.utils;

import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.Configuration;
import org.junit.Assert;
import org.junit.Test;

import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.CLUSTER_NAME;
import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.HA_ENABLE;
import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.THRIFT_BIND_HOST;
import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.THRIFT_BIND_PORT;
import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.ZOOKEEPER_SERVER;

public class AmsUtilsTest {

  @Test
  public void testGetAMSHaAddress() {
    ArcticMetaStore.conf = new Configuration();
    ArcticMetaStore.conf.set(HA_ENABLE, true);
    ArcticMetaStore.conf.set(ZOOKEEPER_SERVER, "127.0.0.1:2181");
    ArcticMetaStore.conf.set(CLUSTER_NAME, "default");
    Assert.assertEquals(AmsUtils.getAMSHaAddress(), "zookeeper://127.0.0.1:2181/default/");

    ArcticMetaStore.conf.set(HA_ENABLE, false);
    ArcticMetaStore.conf.set(THRIFT_BIND_HOST, "127.0.0.1");
    ArcticMetaStore.conf.set(THRIFT_BIND_PORT, 1260);
    Assert.assertEquals(AmsUtils.getAMSHaAddress(), "thrift://127.0.0.1:1260/");
  }
}
