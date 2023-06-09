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

package com.qlangtech.tis.plugin.incr;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.IRCController;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.PluginStore;
import com.qlangtech.tis.trigger.jst.ILogListener;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: baisui 百岁
 * @create: 2020-08-17 11:22
 **/
public class TestDefaultIncrK8sConfig extends TestCase {
    public static final String totalpay = "search4totalpay";
//    public void testDefaultIncrK8sConfig() throws Exception {
//        DefaultIncrK8sConfig incrK8sCfg = new DefaultIncrK8sConfig();
//        incrK8sCfg.namespace = "tis";
//        incrK8sCfg.k8sName = "minikube";
//        incrK8sCfg.imagePath = "registry.cn-hangzhou.aliyuncs.com/tis/tis-incr:latest";
//
//        incrK8sCfg.getIncrSync().removeInstance("search4totalpay");
//    }

    public void testlistPodsAndWatchLog() throws Exception {
        IRCController incrSync = getIncrSync();
        String podName = "podname";

        assertNotNull(incrSync);
        final AtomicInteger msgReceiveCount = new AtomicInteger();
        incrSync.listPodAndWatchLog(new TargetResName(totalpay), podName, new ILogListener() {
            @Override
            public void sendMsg2Client(Object biz) throws IOException {

                System.out.println("flushCount:" + msgReceiveCount.incrementAndGet());
            }

            @Override
            public void read(Object event) {

            }

            @Override
            public boolean isClosed() {
                return false;
            }
        });
        Thread.sleep(5000);
        assertTrue(msgReceiveCount.get() > 0);
    }

    public static IRCController getIncrSync() {
        IPluginStore<IncrStreamFactory> store = TIS.getPluginStore(totalpay, IncrStreamFactory.class);
        assertNotNull(store);
        IncrStreamFactory incrStream = store.getPlugin();
        assertNotNull(incrStream);

        IRCController incrSync = incrStream.getIncrSync();
        assertNotNull(incrSync);
        return incrSync;
    }
}
