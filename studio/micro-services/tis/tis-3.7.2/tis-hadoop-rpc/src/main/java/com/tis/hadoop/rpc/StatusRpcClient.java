/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tis.hadoop.rpc;

import com.google.common.collect.Iterators;
import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildSharedPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus.TableDumpStatus;
import com.qlangtech.tis.realtime.yarn.rpc.*;
import com.qlangtech.tis.rpc.grpc.log.ILogReporter;
import com.qlangtech.tis.rpc.grpc.log.LogCollectorClient;
import com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget;
import com.qlangtech.tis.rpc.server.IncrStatusClient;
import com.qlangtech.tis.solrj.util.ZkUtils;
import com.qlangtech.tis.trigger.jst.ILogListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月22日
 */
public class StatusRpcClient {

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(.+?):(\\d+)$");

    private static final Logger logger = LoggerFactory.getLogger(StatusRpcClient.class);

    private static final StatusRpcClient instance = new StatusRpcClient();

    private StatusRpcClient() {
    }

    public static RpcServiceReference getService(ITISCoordinator zookeeper, AdapterAssembleSvcCompsiteCallback... callbacks) throws Exception {
        return instance.connect2RemoteIncrStatusServer(zookeeper, callbacks);
    }

    /**
     * 连接日志收集节点地址
     *
     * @param zookeeper zookeeper client
     * @param reConnect 是否需要重连
     * @throws Exception 异常
     */
    private void connect2RemoteIncrStatusServer(final ITISCoordinator zookeeper, boolean reConnect, final AssembleSvcCompsiteCallback rpcCallback) {
        // 增量状态收集节点
        final String incrStateCollectAddress = ZkUtils.getFirstChildValue(zookeeper, ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PATH
                ,
//                new AbstractWatcher() {
//            @Override
//            protected void process(Watcher watcher) throws KeeperException, InterruptedException {
//                try {
//                    connect2RemoteIncrStatusServer(zookeeper, false, /* reConnect */
//                            rpcCallback);
//                } catch (Exception e) {
//                    error(e.getMessage(), e);
//                    logger.error(e.getMessage(), e);
//                }
//            }
//        },
                reConnect);
        connect2RemoteIncrStatusServer(incrStateCollectAddress, rpcCallback);
    }

    public static AssembleSvcCompsite connect2RemoteIncrStatusServer(String incrStateCollectAddress) {
        return instance.connect2RemoteIncrStatusServer(incrStateCollectAddress, new AssembleSvcCompsiteCallback() {

            @Override
            public AssembleSvcCompsite process(AssembleSvcCompsite oldrpc, AssembleSvcCompsite newrpc) {
                return newrpc;
            }

            @Override
            public AssembleSvcCompsite getOld() {
                return null;
            }

            @Override
            public void errorOccur(AssembleSvcCompsite oldrpc, Exception e) {
            }
        });
    }

    private AssembleSvcCompsite connect2RemoteIncrStatusServer(String incrStateCollectAddress, AssembleSvcCompsiteCallback rpcCallback) {
        InetSocketAddress address;
        Matcher matcher = ADDRESS_PATTERN.matcher(incrStateCollectAddress);
        if (matcher.matches()) {
            address = new InetSocketAddress(matcher.group(1), Integer.parseInt(matcher.group(2)));
        } else {
            // setDoReport(false);
            throw new IllegalStateException("incrStatusRpcServer:" + incrStateCollectAddress + " is not match the pattern:" + ADDRESS_PATTERN);
        }
        info("status server address:" + address);
        AssembleSvcCompsite oldRpc = rpcCallback.getOld();
        try {
            if (oldRpc != null) {
                // RPC.stopProxy(oldRpc);
                oldRpc.close();
            }
            final ManagedChannel channel = ManagedChannelBuilder.forTarget(incrStateCollectAddress).usePlaintext().build();
            IncrStatusClient newRpc = new IncrStatusClient(channel);
            LogCollectorClient logCollectorClient = new LogCollectorClient(channel);
            // IncrStatusUmbilicalProtocol newRpc = RPC.getProxy(IncrStatusUmbilicalProtocol.class, IncrStatusUmbilicalProtocol.versionID, address, new Configuration());
            info("successful connect to " + address + ",pingResult:" + newRpc.ping());
            return rpcCallback.process(oldRpc, new AssembleSvcCompsite(newRpc, logCollectorClient) {
                @Override
                public void close() {
                    try {
                        channel.shutdownNow().awaitTermination(2, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            error(e.getMessage(), e);
            // setDoReport(false);
            rpcCallback.errorOccur(oldRpc, e);
        }
        return null;
    }

    /**
     * 连接到Assemble服务器
     *
     * @param
     * @throws Exception
     */
    private RpcServiceReference connect2RemoteIncrStatusServer(ITISCoordinator zookeeper, AdapterAssembleSvcCompsiteCallback... callbacks) throws Exception {
        final AtomicReference<ITISRpcService> ref = new AtomicReference<>();
        ref.set(AssembleSvcCompsite.MOCK_PRC);
        if (!zookeeper.shallConnect2RemoteIncrStatusServer()) {
            return new RpcServiceReference(ref, () -> {
            });
        }
        Runnable connect = () -> {
            StatusRpcClient statusRpcClient = new StatusRpcClient();
            statusRpcClient.connect2RemoteIncrStatusServer(zookeeper, true, /* reConnect */
                    new AssembleSvcCompsiteCallback() {
                        @Override
                        public AssembleSvcCompsite process(AssembleSvcCompsite oldrpc, AssembleSvcCompsite newrpc) {
                            ref.compareAndSet(oldrpc, newrpc);
                            for (AdapterAssembleSvcCompsiteCallback c : callbacks) {
                                c.process(oldrpc, newrpc);
                            }
                            return newrpc;
                        }

                        @Override
                        public AssembleSvcCompsite getOld() {
                            return ref.get().unwrap();
                        }

                        @Override
                        public void errorOccur(AssembleSvcCompsite oldrpc, Exception e) {
                            ref.compareAndSet(oldrpc, AssembleSvcCompsite.MOCK_PRC);
                        }
                    });
        };
        connect.run();
        return new RpcServiceReference(ref, connect);
    }

    public interface AssembleSvcCompsiteCallback {

        public AssembleSvcCompsite process(AssembleSvcCompsite oldrpc, AssembleSvcCompsite newrpc);

        public AssembleSvcCompsite getOld();

        // 当错误发生
        public void errorOccur(AssembleSvcCompsite oldrpc, Exception e);
    }

    /**
     * 将Assemble节点上的几个服务节点作一个组合，合并用一个端口
     */
    public abstract static class AssembleSvcCompsite implements ITISRpcService {

        public static final AssembleSvcCompsite MOCK_PRC = new AssembleSvcCompsite(new MockIncrStatusUmbilicalProtocol(), new MockLogReporter()) {
            @Override
            public void close() {
            }

            @Override
            public AssembleSvcCompsite unwrap() {
                return this;
            }
        };

        @Override
        public AssembleSvcCompsite unwrap() {
            return this;
        }

        // 各个子节点汇报状态用
        public final IncrStatusUmbilicalProtocol statReceiveSvc;

        // 汇总状态之后供，console节点来访问用
        public final ILogReporter statReportSvc;

        public abstract void close();

        public void reportDumpJobStatus(boolean faild, boolean complete, boolean waiting, Integer taskId, String jobName, int readRows, int allRows) {
            StatusRpcClient.AssembleSvcCompsite svc = this;
            DumpPhaseStatus.TableDumpStatus dumpStatus = new DumpPhaseStatus.TableDumpStatus(jobName, taskId);
            dumpStatus.setFaild(faild);
            dumpStatus.setComplete(complete);
            dumpStatus.setWaiting(waiting);
            dumpStatus.setReadRows(readRows);
            dumpStatus.setAllRows(allRows);
            svc.reportDumpTableStatus(dumpStatus);
        }

        public AssembleSvcCompsite(IncrStatusUmbilicalProtocol statReceiveSvc, ILogReporter statReportSvc) {
            Objects.requireNonNull(statReceiveSvc, "param statReceiveSvc can not be null");
            Objects.requireNonNull(statReportSvc, "param statReportSvc can not be null");
            this.statReceiveSvc = statReceiveSvc;
            this.statReportSvc = statReportSvc;
        }

        public StreamObserver<PMonotorTarget> registerMonitorEvent(ILogListener logListener) {
            return statReportSvc.registerMonitorEvent(logListener);
        }

        public java.util.Iterator<com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection> buildPhraseStatus(Integer taskid) throws Exception {
            return statReportSvc.buildPhraseStatus(taskid);
        }

        public PingResult ping() {
            return statReceiveSvc.ping();
        }

        public MasterJob reportStatus(UpdateCounterMap upateCounter) {
            return statReceiveSvc.reportStatus(upateCounter);
        }

        public void nodeLaunchReport(LaunchReportInfo launchReportInfo) {
            statReceiveSvc.nodeLaunchReport(launchReportInfo);
        }

        public void reportDumpTableStatus(TableDumpStatus tableDumpStatus) {
            statReceiveSvc.reportDumpTableStatus(tableDumpStatus);
        }

        public void reportBuildIndexStatus(BuildSharedPhaseStatus buildStatus) {
            statReceiveSvc.reportBuildIndexStatus(buildStatus);
        }
    }

    private static void info(String msg) {
        System.out.println(msg);
    }

    private static void error(String msg, Throwable e) {
        info("err:" + msg);
        if (e != null) {
            info(ExceptionUtils.getFullStackTrace(e));
        }
    }

    public static class NoopStreamObserver<V> implements StreamObserver<V> {

        @Override
        public void onNext(V value) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onCompleted() {
        }
    }

    public static class MockLogReporter implements ILogReporter {

        @Override
        public StreamObserver<PMonotorTarget> registerMonitorEvent(ILogListener logListener) {
            return new NoopStreamObserver<>();
        }

        @Override
        public java.util.Iterator<com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection> buildPhraseStatus(Integer taskid) throws Exception {
            return Iterators.forArray();
        }
    }

    private static class MockIncrStatusUmbilicalProtocol implements IncrStatusUmbilicalProtocol, Closeable {

        @Override
        public void nodeLaunchReport(LaunchReportInfo launchReportInfo) {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public PingResult ping() {
            return null;
        }

        @Override
        public MasterJob reportStatus(UpdateCounterMap upateCounter) {
            logger.warn("stat report server has not connect on!!!! using Mock channel");
            return null;
        }

        @Override
        public void reportDumpTableStatus(TableDumpStatus tableDumpStatus) {
        }

        @Override
        public void reportBuildIndexStatus(BuildSharedPhaseStatus buildStatus) {
        }
    }
}
