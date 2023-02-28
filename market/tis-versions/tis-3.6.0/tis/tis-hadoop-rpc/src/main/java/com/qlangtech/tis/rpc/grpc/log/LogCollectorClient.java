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
package com.qlangtech.tis.rpc.grpc.log;

import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableMap;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.fullbuild.phasestatus.JobLog;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.IndexBackFlowPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus;
import com.qlangtech.tis.rpc.grpc.log.stream.*;
import com.qlangtech.tis.trigger.jst.ILogListener;
import com.qlangtech.tis.trigger.jst.MonotorTarget;
import com.qlangtech.tis.trigger.socket.ExecuteState;
import com.qlangtech.tis.trigger.socket.InfoType;
import com.qlangtech.tis.trigger.socket.LogType;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-05-11 15:19
 */
public class LogCollectorClient implements ILogReporter {

    private static final Logger logger = LoggerFactory.getLogger(LogCollectorClient.class);

    private final LogCollectorGrpc.LogCollectorStub stub;

    private final LogCollectorGrpc.LogCollectorBlockingStub blockStub;

    public LogCollectorClient(ManagedChannel channel) {
        this.blockStub = LogCollectorGrpc.newBlockingStub(channel);
        this.stub = LogCollectorGrpc.newStub(channel);

    }

    public static void main(String[] args) throws Exception {
        final String target = "10.1.24.59:46478";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        LogCollectorClient logCollectorClient = new LogCollectorClient(channel);
        int taskid = 323;
        System.out.println("start exec buildPhraseStatus");
        logCollectorClient.buildPhraseStatus(taskid);
        synchronized (target) {
            target.wait();
        }
    }

    @Override
    public StreamObserver<PMonotorTarget> registerMonitorEvent(ILogListener logListener) {
        ClientResponseObserver<PMonotorTarget, PExecuteState> clientResponseObserver = new ClientResponseObserver<PMonotorTarget, PExecuteState>() {

            ClientCallStreamObserver<PMonotorTarget> requestStream;

            @Override
            public void beforeStart(final ClientCallStreamObserver<PMonotorTarget> requestStream) {
                this.requestStream = requestStream;
                requestStream.setOnReadyHandler(new Runnable() {

                    // An iterator is used so we can pause and resume iteration of the request data.
                    @Override
                    public void run() {
                        // try {
                        // while (requestStream.isReady()) {
                        // MonotorTarget focus = focusTarget.take();
                        //
                        // if (RegisterMonotorTarget.isPoisonPill(focus)) {
                        // requestStream.onCompleted();
                        // return;
                        // } else {
                        // PMonotorTarget.Builder t = PMonotorTarget.newBuilder();
                        // t.setLogtype(convert(focus.logType.typeKind));
                        // t.setCollection(focus.getCollection());
                        // if (focus.getTaskid() != null) {
                        // t.setTaskid(focus.getTaskid());
                        // }
                        // requestStream.onNext(t.build());
                        // }
                        // }
                        // } catch (InterruptedException e) {
                        // requestStream.onCompleted();
                        // throw new RuntimeException(e);
                        // }
                    }
                });
            }

            @Override
            public void onNext(PExecuteState value) {
                // System.out.println("=================== receive from server");
                if (logListener.isClosed()) {
                    requestStream.onCompleted();
                    return;
                }
                try {
                    // logListener.read(convert(value));
                    logListener.read((value));
                    // requestStream.request(1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                // done.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("All Done");
                // done.countDown();
            }
        };
        StreamObserver<PMonotorTarget> pMonotorTargetStreamObserver = this.stub.registerMonitorEvent(clientResponseObserver);
        // pMonotorTargetStreamObserver.onNext(t.build());
        return pMonotorTargetStreamObserver;
    }

    public static MonotorTarget convert(PMonotorTarget request) {
        MonotorTarget monotorTarget = MonotorTarget.createRegister(request.getCollection(), convert(request.getLogtype()));
        if (request.getTaskid() > 0) {
            monotorTarget.setTaskid(request.getTaskid());
        }
        return monotorTarget;
    }

    private ExecuteState convert(PExecuteState state) throws Exception {
        ExecuteState s = ExecuteState.create(convert(state.getLogType()), state.getMsg());
        // s.setLogType(convert(state.getLogType()));
        s.setServiceName(state.getServiceName());
        s.setComponent(state.getComponent());
        s.setExecState(state.getExecState());
        // s.setFrom(InetAddress.getByName(state.getFrom()));
        s.setJobId(state.getJobId());
        s.setTaskId(state.getTaskId());
        s.setTime(state.getTime());
        return s;
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType convert(ch.qos.logback.classic.Level level) {
        if (level.toInt() == Level.DEBUG.toInt()) {
            return PExecuteState.InfoType.INFO;
        } else if (level.toInt() == Level.INFO.toInt()) {
            return PExecuteState.InfoType.INFO;
        } else if (level.toInt() == Level.WARN.toInt()) {
            return PExecuteState.InfoType.WARN;
        } else if (level.toInt() == Level.ERROR.toInt()) {
            return PExecuteState.InfoType.ERROR;
        } else {
            throw new IllegalArgumentException("level:" + level + " is illegal");
        }
    }

    private InfoType convert(com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType itype) {
        switch (itype) {
            case INFO:
                return InfoType.INFO;
            case WARN:
                return InfoType.WARN;
            case ERROR:
                return InfoType.ERROR;
            case FATAL:
                return InfoType.FATAL;
            default:
                throw new IllegalStateException("info type is illegal:" + itype);
        }
    }

    public static LogType convert(com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType logtype) {
        switch (logtype) {
            case MQ_TAGS_STATUS:
                //  return LogType.MQ_TAGS_STATUS;
                throw new UnsupportedOperationException("logtype :" + logtype);
            case INCR_SEND:
                return LogType.INCR_SEND;
            case INCR_DEPLOY_STATUS_CHANGE:
                return LogType.INCR_DEPLOY_STATUS_CHANGE;
            case FULL:
                return LogType.FULL;
            case INCR:
                return LogType.INCR;
            default:
                throw new IllegalStateException("in valid logtype:" + logtype);
        }
    }

    private static final Map<Integer, com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType> logTypeMap;

    static {
        ImmutableMap.Builder logTypeMapBuilder = ImmutableMap.builder();
        // logTypeMapBuilder.put(LogType.MQ_TAGS_STATUS.typeKind, PExecuteState.LogType.MQ_TAGS_STATUS);
        logTypeMapBuilder.put(LogType.INCR_SEND.typeKind, PExecuteState.LogType.INCR_SEND);
        logTypeMapBuilder.put(LogType.INCR_DEPLOY_STATUS_CHANGE.typeKind, PExecuteState.LogType.INCR_DEPLOY_STATUS_CHANGE);
        logTypeMapBuilder.put(LogType.FULL.typeKind, PExecuteState.LogType.FULL);
        logTypeMapBuilder.put(LogType.INCR.typeKind, PExecuteState.LogType.INCR);
        logTypeMap = logTypeMapBuilder.build();
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType convert(int logtypeKind) {
        com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType type = logTypeMap.get(logtypeKind);
        if (type == null) {
            throw new IllegalStateException("in valid logtype:" + logtypeKind);
        }
        return type;
    }

    @Override
    public java.util.Iterator<com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection> buildPhraseStatus(// , final IPhaseStatusCollectionListener slistener
                                                                                                              Integer taskid) throws Exception {
        PBuildPhaseStatusParam statusParam = PBuildPhaseStatusParam.newBuilder().setTaskid(taskid).build();
        return blockStub.buildPhraseStatus(statusParam);
        // responseObserver.onCompleted();
        // finishLatch.await();
    }

    public static PhaseStatusCollection convert(PPhaseStatusCollection stat, ExecutePhaseRange executePhaseRange) {


        PDumpPhaseStatus dumpPhase = stat.getDumpPhase();
        PJoinPhaseStatus joinPhase = stat.getJoinPhase();
        PBuildPhaseStatus buildPhase = stat.getBuildPhase();
        PIndexBackFlowPhaseStatus backflow = stat.getIndexBackFlowPhaseStatus();
        PhaseStatusCollection result = new PhaseStatusCollection(stat.getTaskId(), executePhaseRange);
        if (executePhaseRange.contains(FullbuildPhase.FullDump) && dumpPhase != null) {
            DumpPhaseStatus dump = result.getDumpPhase();
            dumpPhase.getTablesDumpMap().forEach((k, v) -> {
                DumpPhaseStatus.TableDumpStatus s = new DumpPhaseStatus.TableDumpStatus(v.getTableName(), v.getTaskid());
                s.setAllRows(v.getAllRows());
                s.setReadRows(v.getReadRows());
                s.setComplete(v.getComplete());
                s.setFaild(v.getFaild());
                s.setWaiting(v.getWaiting());
                dump.tablesDump.put(k, s);
            });
        }
        if (executePhaseRange.contains(FullbuildPhase.JOIN) && joinPhase != null) {
            JoinPhaseStatus join = result.getJoinPhase();
            Map<String, JoinPhaseStatus.JoinTaskStatus> sm = join.taskStatus;
            joinPhase.getTaskStatusMap().forEach((k, v) -> {
                JoinPhaseStatus.JoinTaskStatus s = new JoinPhaseStatus.JoinTaskStatus(v.getJoinTaskName());
                s.setComplete(v.getComplete());
                s.setFaild(v.getFaild());
                s.setWaiting(v.getWaiting());
                v.getJobStatusMap().forEach((jk, jv) -> {
                    JobLog jl = new JobLog();
                    jl.setMapper(jv.getMapper());
                    jl.setReducer(jv.getReducer());
                    jl.setWaiting(jv.getWaiting());
                    s.jobsStatus.put(jk, jl);
                });
                sm.put(k, s);
            });
        }
        if (executePhaseRange.contains(FullbuildPhase.BUILD) && buildPhase != null) {
            BuildPhaseStatus build = result.getBuildPhase();
            buildPhase.getNodeBuildStatusMap().forEach((k, v) -> {
                com.qlangtech.tis.fullbuild.phasestatus.impl.BuildSharedPhaseStatus s = new com.qlangtech.tis.fullbuild.phasestatus.impl.BuildSharedPhaseStatus();
                s.setAllBuildSize(v.getAllBuildSize());
                s.setBuildReaded(v.getBuildReaded());
                s.setTaskid(v.getTaskid());
                s.setSharedName(v.getSharedName());
                s.setComplete(v.getComplete());
                s.setFaild(v.getFaild());
                s.setWaiting(v.getWaiting());
                build.nodeBuildStatus.put(k, s);
            });
        }
        if (executePhaseRange.contains(FullbuildPhase.IndexBackFlow) && backflow != null) {
            IndexBackFlowPhaseStatus bf = result.getIndexBackFlowPhaseStatus();
            backflow.getNodesStatusMap().forEach((k, v) -> {
                IndexBackFlowPhaseStatus.NodeBackflowStatus s = new IndexBackFlowPhaseStatus.NodeBackflowStatus(v.getNodeName());
                s.setAllSize((int) v.getAllSize());
                s.setReaded((int) v.getReaded());
                s.setComplete(v.getComplete());
                s.setFaild(v.getFaild());
                s.setWaiting(v.getWaiting());
                bf.nodesStatus.put(k, s);
            });
        }
        return result;
    }

    public interface IPhaseStatusCollectionListener {

        /**
         * 是否正在监听中
         *
         * @return
         */
        boolean isReady();

        /**
         * 接收任务执行状态消息
         *
         * @param ss
         * @return true：任务仍在执行中 false：任务已经终止，不需要再继续监听了
         * @throws Exception
         */
        boolean process(PPhaseStatusCollection ss) throws Exception;
    }
}
