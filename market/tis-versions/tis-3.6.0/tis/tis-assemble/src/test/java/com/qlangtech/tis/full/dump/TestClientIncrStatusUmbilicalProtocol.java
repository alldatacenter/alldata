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
package com.qlangtech.tis.full.dump;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
// import org.apache.hadoop.ipc.RPC;
import com.qlangtech.tis.realtime.yarn.rpc.IncrStatusUmbilicalProtocol;
import com.qlangtech.tis.realtime.yarn.rpc.UpdateCounterMap;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年6月6日
 */
public class TestClientIncrStatusUmbilicalProtocol extends TestCase {

    private IncrStatusUmbilicalProtocol incrStatusUmbilicalProtocol;

    public void testClient() throws Exception {
        final AtomicInteger i = new AtomicInteger();
        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (incrStatusUmbilicalProtocol != null) {
                        UpdateCounterMap m = new UpdateCounterMap();
                        incrStatusUmbilicalProtocol.reportStatus(m);
                        System.out.println("send times:" + i.incrementAndGet() + ",instacne:");
                    }
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        Thread t = new Thread(runnable);
        t.setDaemon(false);
        t.start();
        startClient(0);
    }

    protected void startClient(int incrPort) throws Exception {
    // final int port = TestServerIncrStatusUmbilicalProtocol.INIT_PORT + incrPort;
    // InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
    // this.incrStatusUmbilicalProtocol = (IncrStatusUmbilicalProtocol) RPC.getProxy(IncrStatusUmbilicalProtocol.class, IncrStatusUmbilicalProtocol.versionID, address, new Configuration());
    // System.out.println("connect server port:" + port);
    // Thread.sleep(60 * 1000);
    // startClient(++incrPort);
    }
}
