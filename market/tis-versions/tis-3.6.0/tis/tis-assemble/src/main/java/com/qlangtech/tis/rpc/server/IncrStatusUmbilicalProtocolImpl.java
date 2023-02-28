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
package com.qlangtech.tis.rpc.server;

import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildSharedPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus.TableDumpStatus;
import com.qlangtech.tis.grpc.Empty;
import com.qlangtech.tis.grpc.IncrStatusGrpc;
import com.qlangtech.tis.grpc.MasterJob;
import com.qlangtech.tis.grpc.TableSingleDataIndexStatus;
import com.qlangtech.tis.order.center.IAppSourcePipelineController;
import com.qlangtech.tis.realtime.transfer.IIncreaseCounter;
import com.qlangtech.tis.realtime.transfer.TableMultiDataIndexStatus;
import com.qlangtech.tis.realtime.yarn.rpc.ConsumeDataKeeper;
import com.qlangtech.tis.realtime.yarn.rpc.IndexJobRunningStatus;

import com.qlangtech.tis.realtime.yarn.rpc.PingResult;
import com.qlangtech.tis.realtime.yarn.rpc.impl.YarnStateStatistics;
import com.tis.hadoop.rpc.StatusRpcClient;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * 服务端接收客户端发送过来的日志消息(服务端)
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年4月7日
 */
public class IncrStatusUmbilicalProtocolImpl extends IncrStatusGrpc.IncrStatusImplBase implements IAppSourcePipelineController {

    private final HashMap<String /**indexName*/, ConcurrentHashMap<String /**uuid代表监听信息的节点*/, TableMultiDataIndexStatus>> //
            updateCounterStatus = new HashMap<>();

    // 存储各个索引執行以来的topic及tags
    private final ConcurrentHashMap<String, com.qlangtech.tis.grpc.TopicInfo> /* indexName */
            indexTopicInfo = new ConcurrentHashMap<>();

    private final BlockingQueue<com.qlangtech.tis.grpc.MasterJob> jobQueue = new ArrayBlockingQueue<>(100);

    private static final IncrStatusUmbilicalProtocolImpl instance = new IncrStatusUmbilicalProtocolImpl();

    public static IncrStatusUmbilicalProtocolImpl getInstance() {
        return instance;
    }

    private IncrStatusUmbilicalProtocolImpl() {
    }

    // 单位ms
    private static final int JOB_EXPIRE_TIME = 30000;

    private static final int TABLE_COUNT_GAP = 5;

    // 定时任务，打印日志
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static final Logger log = LoggerFactory.getLogger(IncrStatusUmbilicalProtocolImpl.class);

    public static final Logger statisLog = LoggerFactory.getLogger("statis");

    private static final JsonFormat.Printer jsonPrint = JsonFormat.printer();

    private static final ThreadLocal<SimpleDateFormat> formatYyyyMMddHHmmss
            = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    @Override
    public void ping(Empty request, StreamObserver<com.qlangtech.tis.grpc.PingResult> responseObserver) {
        PingResult result = new PingResult();
        com.qlangtech.tis.grpc.PingResult.Builder builder = com.qlangtech.tis.grpc.PingResult.newBuilder();
        builder.setValue(result.getValue());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void reportStatus(com.qlangtech.tis.grpc.UpdateCounterMap updateCounter, StreamObserver<com.qlangtech.tis.grpc.MasterJob> responseObserver) {
        String from = updateCounter.getFrom();
        // 为了避免分布式集群中多个节点时间不同，现在统一使用本节点的时间
        // updateCounter.getUpdateTime();
        long updateTime = ConsumeDataKeeper.getCurrentTimeInSec();
        for (Map.Entry<String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> entry : updateCounter.getDataMap().entrySet()) {
            String indexName = entry.getKey();
            com.qlangtech.tis.grpc.TableSingleDataIndexStatus updateCounterFromClient = entry.getValue();
            String uuid = updateCounterFromClient.getUuid();
            TableMultiDataIndexStatus tableMultiDataIndexStatus = getAppSubExecNodeMetrixStatus(indexName, uuid);
            tableMultiDataIndexStatus.setBufferQueueRemainingCapacity(updateCounterFromClient.getBufferQueueRemainingCapacity());
            tableMultiDataIndexStatus.setConsumeErrorCount(updateCounterFromClient.getConsumeErrorCount());
            tableMultiDataIndexStatus.setIgnoreRowsCount(updateCounterFromClient.getIgnoreRowsCount());
            tableMultiDataIndexStatus.setUUID(updateCounterFromClient.getUuid());
            tableMultiDataIndexStatus.setFromAddress(from);
            tableMultiDataIndexStatus.setUpdateTime(updateTime);
            tableMultiDataIndexStatus.setIncrProcessPaused(updateCounterFromClient.getIncrProcessPaused());
            // tableMultiDataIndexStatus.setTis30sAvgRT(updateCounterFromClient.getTis30sAvgRT());
            tableMultiDataIndexStatus.setTis30sAvgRT(updateCounterFromClient.getTis30SAvgRT());
            for (Map.Entry<String, Long> tabUpdate : entry.getValue().getTableConsumeDataMap().entrySet()) {
                tableMultiDataIndexStatus.put(tabUpdate.getKey(), new ConsumeDataKeeper(tabUpdate.getValue(), updateTime));
            }
        }
        MasterJob masterJob = pollJob(updateCounter);
        if (masterJob != null) {
            responseObserver.onNext(masterJob);
        } else {
            responseObserver.onNext(MasterJob.newBuilder().setJobType(MasterJob.JobType.None).build());
        }
        responseObserver.onCompleted();
    }

    public TableMultiDataIndexStatus getAppSubExecNodeMetrixStatus(String indexName, String subExecNodeId) {
        ConcurrentHashMap<String, TableMultiDataIndexStatus> indexStatus = updateCounterStatus.get(indexName);
        if (indexStatus == null) {
            synchronized (updateCounterStatus) {
                indexStatus = updateCounterStatus.computeIfAbsent(indexName, k -> new ConcurrentHashMap<>());
            }
        }
        TableMultiDataIndexStatus tableMultiDataIndexStatus = indexStatus.get(subExecNodeId);
        if (tableMultiDataIndexStatus == null) {
            tableMultiDataIndexStatus = indexStatus.computeIfAbsent(subExecNodeId, k -> new TableMultiDataIndexStatus());
        }
        return tableMultiDataIndexStatus;
    }

    @Override
    public void nodeLaunchReport(com.qlangtech.tis.grpc.LaunchReportInfo launchReportInfo, StreamObserver<Empty> responseObserver) {
        try {
            synchronized (indexTopicInfo) {
                for (Map.Entry<String, com.qlangtech.tis.grpc.TopicInfo> /* collection */
                        entry : launchReportInfo.getCollectionFocusTopicInfoMap().entrySet()) {
                    this.indexTopicInfo.put(entry.getKey(), entry.getValue());
                    log.info("collection:" + entry.getKey() + " topicfocuse:" + jsonPrint.print(entry.getValue()));
                }
            }
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        returnEmpty(responseObserver);
    }

    public static ExecHook execHook = new ExecHook();

//    public void reportDumpTableStatusError(Integer taskid, String fullTableName) {
//        execHook.reportDumpTableStatusError(taskid, fullTableName);
//        reportDumpTableStatus(taskid, true, false, true, fullTableName);
//    }

    public void reportDumpTableStatus(DumpPhaseStatus.TableDumpStatus tableDumpStatus) {
//        com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus.Builder builder = com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus.newBuilder();
//        builder.setTaskid(taskid);
//        builder.setTableName(fullTableName);
//        builder.setFaild(faild);
//        builder.setComplete(complete);
//        builder.setWaiting(waiting);

        this.reportDumpTableStatus(IncrStatusClient.convert(tableDumpStatus), new StatusRpcClient.NoopStreamObserver<>());
    }


    public static class ExecHook {

        public void reportDumpTableStatusError(Integer taskid, String fullTableName) {
        }

        public void reportBuildIndexStatErr(int taskid, String shardName) {
        }
    }

    public void reportBuildIndexStatErr(int taskid, String shardName) {
        execHook.reportBuildIndexStatErr(taskid, shardName);
        com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.Builder builder
                = com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.newBuilder();
        builder.setTaskid(taskid);
        builder.setSharedName(shardName);
        builder.setFaild(true);
        builder.setComplete(true);
        builder.setWaiting(false);
        this.reportBuildIndexStatus(builder.build(), new StatusRpcClient.NoopStreamObserver<>());
    }

    @Override
    public void reportDumpTableStatus(com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus tableDumpStatus, StreamObserver<Empty> responseObserver) {
        Integer taskid = tableDumpStatus.getTaskid();
        if (taskid == null || taskid < 1) {
            throw new IllegalArgumentException("taskid illegal:" + taskid);
        }
        PhaseStatusCollection phaseStatusSet = TrackableExecuteInterceptor.getTaskPhaseReference(taskid);
        if (phaseStatusSet == null) {
            returnEmpty(responseObserver);
            return;
        }
        log.info("taskid:" + taskid + ",tablename:" + tableDumpStatus.getTableName() + ",read:"
                + tableDumpStatus.getReadRows() + ",all:" + tableDumpStatus.getAllRows());
        DumpPhaseStatus dumpPhase = phaseStatusSet.getDumpPhase();
        TableDumpStatus dumpStatus = phaseStatusSet.getDumpPhase().getTable(tableDumpStatus.getTableName());
        // }
        if (tableDumpStatus.getComplete()) {
            // 成功
            dumpStatus.setComplete(true);
        }
        if (tableDumpStatus.getFaild()) {
            // 失败了
            dumpStatus.setFaild(true);
        }
        dumpStatus.setReadRows(tableDumpStatus.getReadRows());
        dumpStatus.setAllRows(tableDumpStatus.getAllRows());
        if (!tableDumpStatus.getWaiting()) {
            dumpStatus.setWaiting(tableDumpStatus.getWaiting());
        }
        dumpPhase.isComplete();
        returnEmpty(responseObserver);
    }

    private void returnEmpty(StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void reportBuildIndexStatus(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus buildStatus
            , StreamObserver<Empty> responseObserver) {
        Integer taskid = buildStatus.getTaskid();
        if (taskid == null) {
            throw new IllegalArgumentException("taskid can not be null");
        }
        PhaseStatusCollection phaseStatusSet = TrackableExecuteInterceptor.getTaskPhaseReference(taskid);
        if (phaseStatusSet == null) {
            log.warn("taskid:" + taskid + " relevent phaseStatusSet is null");
            returnEmpty(responseObserver);
            return;
        }
        BuildPhaseStatus status = phaseStatusSet.getBuildPhase();
        BuildSharedPhaseStatus sharedBuildStatus = status.getBuildSharedPhaseStatus(buildStatus.getSharedName());
        if (!sharedBuildStatus.isFaild()) {
            sharedBuildStatus.setFaild(buildStatus.getFaild());
        }
        if (!sharedBuildStatus.isComplete()) {
            sharedBuildStatus.setComplete(buildStatus.getComplete());
        }
        if (sharedBuildStatus.isWaiting()) {
            sharedBuildStatus.setWaiting(buildStatus.getWaiting());
        }
        sharedBuildStatus.setAllBuildSize(buildStatus.getAllBuildSize());
        sharedBuildStatus.setBuildReaded(buildStatus.getBuildReaded());
        status.isComplete();
        returnEmpty(responseObserver);
    }


    public IndexJobRunningStatus getIndexJobRunningStatus(String collection) {
        return new IndexJobRunningStatus(this.isIncrGoingOn(collection), this.isIncrProcessPaused(collection));
    }

    /**
     * 增量引擎是否开启中
     *
     * @param collection
     * @return
     */
    private boolean isIncrGoingOn(String collection) {
        if (!this.updateCounterStatus.containsKey(collection)) {
            return false;
        }
        ConcurrentHashMap<String, TableMultiDataIndexStatus> /* uuid发送过来的节点id */
                indexStatus = updateCounterStatus.get(collection);
        return (indexStatus.size() > 0);
    }

    /**
     * 判断索引增量是否暂停中
     *
     * @param collection
     * @return
     */
    private boolean isIncrProcessPaused(String collection) {
        ConcurrentHashMap<String, TableMultiDataIndexStatus> indexStatus = updateCounterStatus.get(collection);
        if (indexStatus == null || indexStatus.size() < 1) {
            return false;
        }
        for (TableMultiDataIndexStatus status : indexStatus.values()) {
            // 遍历所有的节点，只要有一个节点运行中，结果就为运行中
            if (!status.isIncrProcessPaused()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stop(String appName) {
        return this.pauseConsume(appName);
    }

    @Override
    public boolean resume(String appName) {
        return this.resumeConsume(appName);
    }

    private boolean resumeConsume(String indexName) {
        return addJob(indexName, false);
    }

    private boolean pauseConsume(String indexName) {
        return addJob(indexName, true);
    }

    private boolean addJob(String indexName, boolean isPaused) {
        if (!updateCounterStatus.containsKey(indexName)) {
            log.error(indexName + " doesn't not exist in assemble node");
            return false;
        }
        StringBuffer logContent = new StringBuffer("add job to worker by " + indexName + ",stop:" + isPaused + " nodes:");
        for (String uuid : updateCounterStatus.get(indexName).keySet()) {
            com.qlangtech.tis.grpc.MasterJob.Builder masterBuilder = com.qlangtech.tis.grpc.MasterJob.newBuilder();
            //masterBuilder.setJobTypeValue(JobType.IndexJobRunning.getValue());
            masterBuilder.setJobTypeValue(1);
            masterBuilder.setIndexName(indexName);
            masterBuilder.setUuid(uuid);
            masterBuilder.setStop(isPaused);
            masterBuilder.setCreateTime(System.currentTimeMillis());
            // MasterJob job = new MasterJob(JobType.IndexJobRunning, indexName, uuid);
            // job.setStop(isPaused);
            this.sendJob2Worker(masterBuilder.build());
            logContent.append(uuid).append(",");
        }
        log.info(logContent.toString());
        return true;
    }

    private void sendJob2Worker(com.qlangtech.tis.grpc.MasterJob job) {
        try {
            jobQueue.put(job);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static final com.qlangtech.tis.grpc.TopicInfo NULL_TOPIC_INFO;

    static {
        com.qlangtech.tis.grpc.TopicInfo.Builder tiBuilder = com.qlangtech.tis.grpc.TopicInfo.newBuilder();
        NULL_TOPIC_INFO = tiBuilder.build();
    }

    public com.qlangtech.tis.grpc.TopicInfo getFocusTopicInfo(String collection) {
        return indexTopicInfo.getOrDefault(collection, NULL_TOPIC_INFO);
        // return indexTopicInfo.get(collection);
    }

    public Map<String, TableMultiDataIndexStatus> getIndexUpdateCounterStatus(String collection) {
        return updateCounterStatus.get(collection);
    }


    void removeIndexUpdateCounterStatus(String collection) {
        this.updateCounterStatus.remove(collection);
    }

    private boolean startLogging = false;

    public void startLogging() {
        if (startLogging) {
            // 已经开始了则退出
            return;
        }
        try {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    long currentTimeInSec = ConsumeDataKeeper.getCurrentTimeInSec();
                    for (String indexName : updateCounterStatus.keySet()) {
                        ConcurrentHashMap<String, TableMultiDataIndexStatus> /* uuid发送过来的节点id */
                                indexStatus = updateCounterStatus.get(indexName);
                        indexStatus.entrySet().removeIf(entry -> {
                            synchronized (indexTopicInfo) {
                                boolean expire = entry.getValue().isExpire(currentTimeInSec);
                                return expire;
                            }
                        });
                        if (indexStatus.size() <= 0) {
                            continue;
                        }
                        setCollectionName(indexName);
                        printLog(indexStatus, currentTimeInSec);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }, 15, TABLE_COUNT_GAP, TimeUnit.SECONDS);
        } finally {
            startLogging = true;
        }
    }

    /**
     * 取得索引对应的各个tag下的绝对更新累计值
     *
     * @param collection
     * @return
     */
    public Map<String, /* tag */
            Long> getUpdateAbsoluteCountMap(String collection) {
        return getTableUpdateCountMap(updateCounterStatus.get(collection));
    }

    private void printLog(ConcurrentHashMap<String, TableMultiDataIndexStatus> indexStatus, long currentTimeInSec) {
        String dateString = formatYyyyMMddHHmmss.get().format(new Date());
        Map<String, YarnStateStatistics> yarnStateMap = getYarnStateMap(indexStatus, currentTimeInSec);
        YarnStateStatistics yarnState = getMapCount(yarnStateMap);
        statisLog.info(dateString + ", tbTPS:" + yarnState.getTbTPS() + ", tisTPS:" + yarnState.getSorlTPS() + "\r\n"
                + "detail:" + getYarnStateString(yarnStateMap) + "tableCount:" + getTableUpdateCount(indexStatus));
    }

    private Map<String, YarnStateStatistics> getYarnStateMap(ConcurrentHashMap<String, /* uuid */
            TableMultiDataIndexStatus> indexStatus, long currentTimeInSec) {
        Map<String, YarnStateStatistics> yarnStateMap = new HashMap<>();
        String uuid = null;
        TableMultiDataIndexStatus status = null;
        for (Map.Entry<String, TableMultiDataIndexStatus> entry : indexStatus.entrySet()) {
            uuid = entry.getKey();
            status = entry.getValue();
            YarnStateStatistics yarnStateStatistics = new YarnStateStatistics();
            yarnStateMap.put(uuid, yarnStateStatistics);
            yarnStateStatistics.setPaused(status.isIncrProcessPaused());
            // add queueRC
            yarnStateStatistics.setQueueRC(status.getBufferQueueRemainingCapacity());
            // add from
            yarnStateStatistics.setFrom(status.getFromAddress());
            // add Tis30sAvgRT
            yarnStateStatistics.setTis30sAvgRT(status.getTis30sAvgRT());
            // add tbTPS
            LinkedList<ConsumeDataKeeper> consumeDataKeepers = status.getConsumeDataKeepList(IIncreaseCounter.TABLE_CONSUME_COUNT);
            if (consumeDataKeepers == null || consumeDataKeepers.size() <= 0) {
                yarnStateStatistics.setTbTPS(0L);
            } else {
                ConsumeDataKeeper last = consumeDataKeepers.getLast();
                ConsumeDataKeeper start = null;
                for (int i = consumeDataKeepers.size() - 2; i >= 0; i--) {
                    ConsumeDataKeeper tmpKeeper = consumeDataKeepers.get(i);
                    if (tmpKeeper.getCreateTime() > currentTimeInSec - TABLE_COUNT_GAP) {
                        start = tmpKeeper;
                    } else {
                        break;
                    }
                }
                if (start == null) {
                    yarnStateStatistics.setTbTPS(last.getAccumulation() / TABLE_COUNT_GAP);
                } else {
                    yarnStateStatistics.setTbTPS((last.getAccumulation() - start.getAccumulation()) / TABLE_COUNT_GAP);
                }
            }
            // add solrTPS
            consumeDataKeepers = status.getConsumeDataKeepList(IIncreaseCounter.SOLR_CONSUME_COUNT);
            if (consumeDataKeepers == null || consumeDataKeepers.size() <= 0) {
                yarnStateStatistics.setTbTPS(0L);
            } else {
                ConsumeDataKeeper last = consumeDataKeepers.getLast();
                ConsumeDataKeeper start = null;
                for (int i = consumeDataKeepers.size() - 2; i >= 0; i--) {
                    ConsumeDataKeeper tmpKeeper = consumeDataKeepers.get(i);
                    if (tmpKeeper.getCreateTime() > currentTimeInSec - TABLE_COUNT_GAP) {
                        start = tmpKeeper;
                    } else {
                        break;
                    }
                }
                if (start == null) {
                    yarnStateStatistics.setSorlTPS(last.getAccumulation() / TABLE_COUNT_GAP);
                } else {
                    yarnStateStatistics.setSorlTPS((last.getAccumulation() - start.getAccumulation()) / TABLE_COUNT_GAP);
                }
            }
        }
        return yarnStateMap;
    }

    private YarnStateStatistics getMapCount(Map<String, YarnStateStatistics> yarnStateMap) {
        YarnStateStatistics yarnState = new YarnStateStatistics();
        for (YarnStateStatistics yarnStateStatistics : yarnStateMap.values()) {
            yarnState.setTbTPS(yarnState.getTbTPS() + yarnStateStatistics.getTbTPS());
            yarnState.setSorlTPS(yarnState.getSorlTPS() + yarnStateStatistics.getSorlTPS());
        }
        return yarnState;
    }

    private static String getYarnStateString(Map<String, YarnStateStatistics> yarnStateMap) {
        StringBuilder sb = new StringBuilder("\r\n");
        for (YarnStateStatistics yarnStateStatistics : yarnStateMap.values()) {
            String state = String.format("{'host':'%s'," + (yarnStateStatistics.isPaused() ? " paused ," : StringUtils.EMPTY)
                            + "'tbTPS':%d, 'solrTPS':%d, 'solr_30s_avg_rt':%dms, 'queueRC':%d}\r\n", yarnStateStatistics.getFrom()
                    , yarnStateStatistics.getTbTPS(), yarnStateStatistics.getSorlTPS()
                    , yarnStateStatistics.getTis30sAvgRT(), yarnStateStatistics.getQueueRC());
            sb.append(state);
        }
        return sb.toString();
    }

    private com.qlangtech.tis.grpc.MasterJob pollJob(com.qlangtech.tis.grpc.UpdateCounterMap upateCounter) {
        if (jobQueue.size() <= 0) {
            return null;
        }
        com.qlangtech.tis.grpc.MasterJob job;
        // 先剔除过期的job
        long nowTime = System.currentTimeMillis();
        while (true) {
            job = jobQueue.peek();
            if (job == null) {
                return null;
            }
            if (Math.abs(nowTime - job.getCreateTime()) > JOB_EXPIRE_TIME) {
                // 长时间没有下发直接抛弃掉了
                jobQueue.poll();
            } else {
                break;
            }
        }
        TableSingleDataIndexStatus singleDataIndexStatus = upateCounter.getDataMap().get(job.getIndexName());
        if (singleDataIndexStatus != null && StringUtils.equals(singleDataIndexStatus.getUuid(), job.getUuid()) && jobQueue.remove(job)) {
            return job;
        } else {
            return null;
        }
    }

    public static void setCollectionName(String collectionName) {
        if (StringUtils.isBlank(collectionName)) {
            throw new IllegalStateException("app name can not be blank");
        }
        MDC.put("app", collectionName);
    }

    /**
     * 取得索引最终更新的时间戳
     *
     * @param index
     * @return
     */
    public Map<String, /* FromAddress */
            Long> getLastUpdateTimeSec(String index) {
        ConcurrentHashMap<String, TableMultiDataIndexStatus> /* uuid发送过来的节点id */
                status = updateCounterStatus.get(index);
        Map<String, Long> /* node last update timesec */
                result = Maps.newHashMap();
        for (Map.Entry<String, TableMultiDataIndexStatus> /* uuid发送过来的节点id */
                entry : status.entrySet()) {
            result.put(entry.getValue().getFromAddress(), entry.getValue().getLastUpdateSec());
        }
        return result;
    }

    private String getTableUpdateCount(ConcurrentHashMap<String, /* uuid */
            TableMultiDataIndexStatus> indexStatus) {
        if (indexStatus == null || indexStatus.size() <= 0) {
            return "[]";
        }
        StringBuffer desc = new StringBuffer("<<\r\n");
        Map<String, Long> updateCountMap = getTableUpdateCountMap(indexStatus);
        for (Map.Entry<String, Long> entry : updateCountMap.entrySet()) {
            desc.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        desc.append(">>");
        return desc.toString();
    }

    private Map<String, /* tag */
            Long> getTableUpdateCountMap(ConcurrentHashMap<String, TableMultiDataIndexStatus> indexStatus) {
        if (indexStatus == null) {
            return Collections.emptyMap();
        }
        Map<String, Long> updateCountMap = new HashMap<>();
        for (TableMultiDataIndexStatus aIndexStatus : indexStatus.values()) {
            for (String tableName : aIndexStatus.getTableNames()) {
                LinkedList<ConsumeDataKeeper> consumeDataKeepers = aIndexStatus.getConsumeDataKeepList(tableName);
                if (consumeDataKeepers.size() <= 0) {
                    continue;
                }
                long accumulation = consumeDataKeepers.getLast().getAccumulation();
                if (updateCountMap.containsKey(tableName)) {
                    updateCountMap.put(tableName, updateCountMap.get(tableName) + accumulation);
                } else {
                    updateCountMap.put(tableName, accumulation);
                }
            }
        }
        return updateCountMap;
    }

    @Override
    public void registerAppSubExecNodeMetrixStatus(String appName, String subExecNodeId) {
        this.getAppSubExecNodeMetrixStatus(appName, subExecNodeId);
    }
}
