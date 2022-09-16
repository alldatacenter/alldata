/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corerpc.benchemark;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corerpc.RpcConfig;
import org.apache.inlong.tubemq.corerpc.RpcConstants;
import org.apache.inlong.tubemq.corerpc.RpcServiceFactory;
import org.apache.inlong.tubemq.corerpc.netty.NettyClientFactory;

public class RcpService4BenchmarkClient {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final String targetHost;
    private final int targetPort;
    private final RpcServiceFactory rpcServiceFactory;
    private final NettyClientFactory clientFactory = new NettyClientFactory();
    private final SimpleService simpleService;
    private int threadNum = 10;
    private int invokeTimes = 1000000;

    /**
     * Initial a benchmark client
     *
     * @param targetHost    the target host
     * @param targetPort    the target port
     * @param threadNum     the thread count
     * @param invokeTimes   the invoke count
     */
    public RcpService4BenchmarkClient(String targetHost, int targetPort, int threadNum,
                                      int invokeTimes) {
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.threadNum = threadNum;
        this.invokeTimes = invokeTimes;
        RpcConfig config = new RpcConfig();
        config.put(RpcConstants.RPC_CODEC, 6);
        config.put(RpcConstants.CONNECT_TIMEOUT, 3000);
        config.put(RpcConstants.REQUEST_TIMEOUT, 10000);

        clientFactory.configure(config);
        rpcServiceFactory = new RpcServiceFactory(clientFactory);
        BrokerInfo brokerInfo = new BrokerInfo(1, targetHost, targetPort);
        this.simpleService =
                rpcServiceFactory.getService(SimpleService.class, brokerInfo, config);
    }

    public static void main(String[] args) throws Exception {
        new RcpService4BenchmarkClient("127.0.0.1", 8088, 10, 100000).start();
    }

    /**
     * Start benchmark test
     *
     * @throws Exception the exception
     */
    public void start() throws Exception {
        for (int i = 0; i < threadNum; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    for (int j = 0; j < invokeTimes; j++) {
                        simpleService.echo("This is a test.");
                    }
                    System.out.println(Thread.currentThread().getName() + " execute " + invokeTimes);
                    long endTime = System.currentTimeMillis() - startTime;
                    System.out.println("cost time:" + endTime + " ms");
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }
}
