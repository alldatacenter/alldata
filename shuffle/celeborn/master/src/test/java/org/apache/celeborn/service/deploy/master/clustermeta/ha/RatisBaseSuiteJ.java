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

package org.apache.celeborn.service.deploy.master.clustermeta.ha;

import java.io.File;
import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;

import org.apache.celeborn.common.CelebornConf;

public class RatisBaseSuiteJ {
  HARaftServer ratisServer;

  @Before
  public void init() throws Exception {
    CelebornConf conf = new CelebornConf();
    HAMasterMetaManager metaSystem = new HAMasterMetaManager(null, conf);
    MetaHandler handler = new MetaHandler(metaSystem);
    File tmpDir1 = File.createTempFile("rss-ratis-tmp", "for-test-only");
    tmpDir1.delete();
    tmpDir1.mkdirs();
    conf.set("celeborn.ha.master.ratis.raft.server.storage.dir", tmpDir1.getAbsolutePath());
    String id = UUID.randomUUID().toString();
    int ratisPort = 9999;
    MasterNode masterNode =
        new MasterNode.Builder().setNodeId(id).setHost("localhost").setRatisPort(ratisPort).build();
    ratisServer =
        HARaftServer.newMasterRatisServer(handler, conf, masterNode, Collections.emptyList());
    ratisServer.start();
  }

  @After
  public void shutdown() {
    if (ratisServer != null) {
      ratisServer.stop();
    }
  }
}
