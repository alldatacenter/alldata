/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.coredefine.module.action;

import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.plugin.PluginStubUtils;
import com.qlangtech.tis.trigger.jst.ILogListener;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-09-01 15:06
 */
public class TestTISK8sDelegate extends TestCase {

  private static final String totalpay = "search4totalpay";

  @Override
  public void setUp() throws Exception {
    super.setUp();
//    PluginStubUtils.setDataDir("/tmp/tis");
    CenterResource.setNotFetchFromCenterRepository();
//    HttpUtils.mockConnMaker = new HttpUtils.DefaultMockConnectionMaker();
//    PluginStubUtils.stubPluginConfig();
  }

//  /**
//   * 前提要保证pod实例启动
//   */
//  public void testMessageListener() throws Exception {
//    TISK8sDelegate k8SDelegate = TISK8sDelegate.getK8SDelegate(totalpay);
//    System.out.println("get k8SDelegate instance");
//    AtomicInteger msgReceiveCount = new AtomicInteger();
//    RcDeployment rc = k8SDelegate.getRcConfig(true);
//    assertTrue("pod size must big than 1", rc.getPods().size() > 0);
//    RcDeployment.PodStatus podStatus = rc.getPods().get(0);
//    k8SDelegate.listPodsAndWatchLog(podStatus.getName(), new ILogListener() {
//
//      @Override
//      public void sendMsg2Client(Object biz) throws IOException {
//        System.out.println("flushCount:" + msgReceiveCount.incrementAndGet());
//      }
//
//      @Override
//      public void read(Object event) {
//      }
//
//      @Override
//      public boolean isClosed() {
//        return false;
//      }
//    });
//    System.out.println("start listPodsAndWatchLog");
//    Thread.sleep(60000);
//    assertTrue(msgReceiveCount.get() > 0);
//  }
}
