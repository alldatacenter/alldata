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

package com.qlangtech.plugins.incr.flink.launch;

import com.qlangtech.plugins.incr.flink.TISFlinkCDCStart;
import com.qlangtech.plugins.incr.flink.common.FlinkCluster;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.config.k8s.ReplicasSpec;
import com.qlangtech.tis.coredefine.module.action.IDeploymentDetail;
import com.qlangtech.tis.coredefine.module.action.IFlinkIncrJobStatus;
import com.qlangtech.tis.coredefine.module.action.IRCController;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.coredefine.module.action.impl.FlinkJobDeploymentDetails;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.manage.common.incr.StreamContextConstant;
import com.qlangtech.tis.plugin.PluginAndCfgsSnapshot;
import com.qlangtech.tis.plugin.incr.WatchPodLog;
import com.qlangtech.tis.plugins.flink.client.FlinkClient;
import com.qlangtech.tis.plugins.flink.client.JarSubmitFlinkRequest;
import com.qlangtech.tis.trigger.jst.ILogListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.client.program.rest.RestClusterClient;
import org.apache.flink.runtime.messages.Acknowledge;
import org.apache.flink.runtime.rest.messages.job.JobDetailsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * 增量任务提交到Flink集群网关
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-20 13:39
 **/
public class FlinkTaskNodeController implements IRCController {
    private static final Logger logger = LoggerFactory.getLogger(FlinkTaskNodeController.class);
    private final TISFlinkCDCStreamFactory factory;

    public FlinkTaskNodeController(TISFlinkCDCStreamFactory factory) {
        this.factory = factory;
    }

    public static void main(String[] args) {

    }


    @Override
    public void checkUseable() {
        FlinkCluster cluster = factory.getClusterCfg();
        cluster.checkUseable();
//        try {
//            try (RestClusterClient restClient = cluster.createFlinkRestClusterClient(Optional.of(1000l))) {
//                // restClient.getClusterId();
//                CompletableFuture<Collection<JobStatusMessage>> status = restClient.listJobs();
//                Collection<JobStatusMessage> jobStatus = status.get();
//            }
//        } catch (Exception e) {
//            throw new TisException("Please check link is valid:" + cluster.getJobManagerAddress().getURL(), e);
//        }
    }

    /**
     * 重新启动
     *
     * @param collection
     * @param targetPod
     */
    @Override
    public void relaunch(TargetResName collection, String... targetPod) {
        FlinkIncrJobStatus status = getIncrJobStatus(collection);
        try {

            for (String savepointPath : targetPod) {
                if ((status.getState() == IFlinkIncrJobStatus.State.STOPED
                        || !((FlinkJobDeploymentDetails) getRCDeployment(collection)).isRunning())
                        && status.containSavepoint(savepointPath)) {
                    File streamUberJar = getStreamUberJarFile(collection);
                    if (!streamUberJar.exists()) {
                        throw new IllegalStateException("streamUberJar is not exist:" + streamUberJar.getAbsolutePath());
                    }
                    this.deploy(collection, streamUberJar
                            , (request) -> {
                                request.setSavepointPath(savepointPath);
                                request.setAllowNonRestoredState(true);
                            }, (jobId) -> {
                                status.relaunch(jobId);
                            });
                    return;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(collection.getName(), e);
        }

        throw new IllegalStateException("targetPod length:" + targetPod.length
                + "，jobid:" + status.getLaunchJobID() + ",status:" + status.getState()
                + ",stored path:" + status.getSavepointPaths().stream().map((p) -> p.getPath()).collect(Collectors.joining(",")));
    }

    @Override
    public void deploy(TargetResName collection, ReplicasSpec incrSpec, long timestamp) throws Exception {

        File streamUberJar = getStreamUberJarFile(collection);
        Manifest manifest = PluginAndCfgsSnapshot.createFlinkIncrJobManifestCfgAttrs(collection, timestamp);
        try (JarOutputStream jaroutput = new JarOutputStream(
                FileUtils.openOutputStream(streamUberJar, false)
                , Objects.requireNonNull(manifest, "manifest can not be null"))) {
            jaroutput.flush();
        }

        this.deploy(collection, streamUberJar
                , (request) -> {
                }, (jobId) -> {
                    FlinkIncrJobStatus incrJob = getIncrJobStatus(collection);
                    incrJob.createNewJob(jobId);
                });
    }

    private File getStreamUberJarFile(TargetResName collection) {
        String streamJar = StreamContextConstant.getIncrStreamJarName(collection.getName());
        File streamUberJar = new File(FileUtils.getTempDirectory() + "/tmp", "uber_" + streamJar);
        logger.info("streamUberJar path:{}", streamUberJar.getAbsolutePath());
        return streamUberJar;
    }

    private void deploy(TargetResName collection, File streamUberJar
            , Consumer<JarSubmitFlinkRequest> requestSetter, Consumer<JobID> afterSuccess) throws Exception {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(TIS.get().getPluginManager().uberClassLoader);
        try (RestClusterClient restClient = factory.getFlinkCluster()) {


            FlinkClient flinkClient = new FlinkClient();


            JarSubmitFlinkRequest request = new JarSubmitFlinkRequest();
            request.setParallelism(factory.parallelism);
            request.setEntryClass(TISFlinkCDCStart.class.getName());


            request.setProgramArgs(collection.getName());
            request.setDependency(streamUberJar.getAbsolutePath());
            requestSetter.accept(request);

            long start = System.currentTimeMillis();
            JobID jobID = flinkClient.submitJar(restClient, request);

            afterSuccess.accept(jobID);

        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

//    private Manifest createManifestCfgAttrs(TargetResName collection, long timestamp) throws Exception {
//
//        Manifest manifest = new Manifest();
//        Map<String, Attributes> entries = manifest.getEntries();
//        Attributes attrs = new Attributes();
//        attrs.put(new Attributes.Name(collection.getName()), String.valueOf(timestamp));
//        // 传递App名称
//        entries.put(TISFlinkCDCStart.TIS_APP_NAME, attrs);
//
//        final Attributes cfgAttrs = new Attributes();
//        // 传递Config变量
//        Config.getInstance().visitKeyValPair((e) -> {
//            if (Config.KEY_TIS_HOST.equals(e.getKey())) {
//                // tishost为127.0.0.1会出错
//                return;
//            }
//            cfgAttrs.put(new Attributes.Name(TISFlinkCDCStart.convertCfgPropertyKey(e.getKey(), true)), e.getValue());
//        });
//        cfgAttrs.put(new Attributes.Name(
//                TISFlinkCDCStart.convertCfgPropertyKey(Config.KEY_TIS_HOST, true)), NetUtils.getHost());
//        entries.put(Config.KEY_JAVA_RUNTIME_PROP_ENV_PROPS, cfgAttrs);
//
//        //=====================================================================
//        if (!CenterResource.notFetchFromCenterRepository()) {
//            throw new IllegalStateException("must not fetchFromCenterRepository");
//        }
//        //"globalPluginStore"  "pluginMetas"  "appLastModifyTimestamp"
//        XStream2.PluginMeta flinkPluginMeta
//                = new XStream2.PluginMeta(TISSinkFactory.KEY_PLUGIN_TPI_CHILD_PATH + collection.getName()
//                , Config.getMetaProps().getVersion());
//        PluginAndCfgsSnapshot localSnapshot
//                = PluginAndCfgsSnapshot.getLocalPluginAndCfgsSnapshot(collection, flinkPluginMeta);
//
//        localSnapshot.attachPluginCfgSnapshot2Manifest(manifest);
//        return manifest;
//    }


    private FlinkIncrJobStatus getIncrJobStatus(TargetResName collection) {
        DataxProcessor processor = DataxProcessor.load(null, collection.getName());
        File dataXWorkDir = processor.getDataXWorkDir(null);
        return new FlinkIncrJobStatus(new File(dataXWorkDir, "incrJob.log"));
    }


    @Override
    public IDeploymentDetail getRCDeployment(TargetResName collection) {
        ExtendFlinkJobDeploymentDetails rcDeployment = null;
        FlinkIncrJobStatus incrJobStatus = this.getIncrJobStatus(collection);
        final FlinkJobDeploymentDetails noneStateDetail
                = new FlinkJobDeploymentDetails(factory.getClusterCfg(), incrJobStatus) {
            @Override
            public boolean isRunning() {
                return false;
            }
        };
        if (incrJobStatus.getState() == IFlinkIncrJobStatus.State.NONE) {
            // stop 或者压根没有创建
            return noneStateDetail;
        }
        JobID launchJobID = incrJobStatus.getLaunchJobID();
        try {
            try (RestClusterClient restClient = this.factory.getFlinkCluster()) {
                CompletableFuture<JobStatus> jobStatus = restClient.getJobStatus(launchJobID);
                JobStatus status = jobStatus.get(5, TimeUnit.SECONDS);
                if (status == null || status.isTerminalState()) {
                    incrJobStatus.setState(IFlinkIncrJobStatus.State.DISAPPEAR);
                    return noneStateDetail;
                }

                CompletableFuture<JobDetailsInfo> jobDetails = restClient.getJobDetails(launchJobID);
                JobDetailsInfo jobDetailsInfo = jobDetails.get(5, TimeUnit.SECONDS);
                rcDeployment = new ExtendFlinkJobDeploymentDetails(factory.getClusterCfg(), incrJobStatus, jobDetailsInfo);
                return rcDeployment;
            }
        } catch (TimeoutException e) {
            FlinkCluster clusterCfg = this.factory.getClusterCfg();
            throw new TisException("flinkClusterId:" + clusterCfg.getClusterId()
                    + ",Address:" + clusterCfg.getJobManagerAddress().getURL() + "连接超时，请检查相应配置是否正确", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (isNotFoundException(cause)) {
                //return null;
                incrJobStatus.setState(IFlinkIncrJobStatus.State.DISAPPEAR);
                return noneStateDetail;
            }
//            if (cause instanceof RestClientException) {
//                //cause.getStackTrace()
//                if (ExceptionUtils.indexOfType(cause, FlinkJobNotFoundException.class) > -1) {
//                    logger.warn("flink JobId:" + launchJobID.toHexString() + " relevant job instant is not found on ServerSize");
//                    return null;
//                }
//
//            }
            throw new RuntimeException(e);
//            if (cause instanceof ) {
//
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isNotFoundException(Throwable cause) {
        return StringUtils.indexOf(cause.getMessage(), "NotFoundException") > -1;
    }

//    private JobID getLaunchJobID(TargetResName collection) {
//        try {
//            File incrJobFile = getIncrJobRecordFile(collection);
//            if (!incrJobFile.exists()) {
//                return null;
//            }
//            return JobID.fromHexString(FileUtils.readFileToString(incrJobFile, TisUTF8.get()));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    @Override
    public SupportTriggerSavePointResult supportTriggerSavePoint(TargetResName collection) {
        SupportTriggerSavePointResult result = new SupportTriggerSavePointResult(false);
        ValidateFlinkJob validateFlinkJob = new ValidateFlinkJob(collection) {
            @Override
            protected void processCollectionNotSupportSavePoint(StateBackendFactory stateBackend) {
                //super.processCollectionNotSupportSavePoint(stateBackend);
                result.setUnSupportReason("当前实例不支持Flink SavePoint，请修改stateBackend配置以支持持久化StateBackend");
            }

            @Override
            protected void processJobNotRunning() {
                result.setUnSupportReason("当前实例状态不在运行中，不能执行该操作");
                //return super.processJobNotRunning();
            }
        };

        return validateFlinkJob.valiate().validateSucess
                ? new SupportTriggerSavePointResult(true) : result;
    }

    /**
     * 创建一个Savepoint
     *
     * @param collection
     */
    @Override
    public void triggerSavePoint(TargetResName collection) {
        processFlinkJob(collection, (restClient, savePoint, status) -> {
            String savepointDirectory = savePoint.createSavePointPath();
            CompletableFuture<String> result
                    = restClient.triggerSavepoint(status.getLaunchJobID(), savepointDirectory);
            status.addSavePoint(result.get(25, TimeUnit.SECONDS), IFlinkIncrJobStatus.State.RUNNING);
        });
    }

    @Override
    public void discardSavepoint(TargetResName collection, String savepointPath) {
//        FlinkIncrJobStatus status = getIncrJobStatus(resName);
//        status.discardSavepoint(savepointPath);


        processFlinkJob(collection, (restClient, savePoint, status) -> {

            CompletableFuture<Acknowledge> result = restClient.disposeSavepoint(savepointPath);

            result.get(25, TimeUnit.SECONDS);
            // String savepointDirectory = savePoint.createSavePointPath();
            status.discardSavepoint(savepointPath);
//            CompletableFuture<String> result
//                    = restClient.triggerSavepoint(status.getLaunchJobID(), savepointDirectory);
            //status.addSavePoint(result.get(25, TimeUnit.SECONDS), IFlinkIncrJobStatus.State.RUNNING);
        });

    }

    private void processFlinkJob(TargetResName collection, FlinkJobFunc jobFunc) {
        ValidateFlinkJob validateFlinkJob = new ValidateFlinkJob(collection).valiate();
        FlinkIncrJobStatus status = validateFlinkJob.getStatus();
        StateBackendFactory.ISavePointSupport savePoint = validateFlinkJob.getSavePoint();

        try (RestClusterClient restClient = this.factory.getFlinkCluster()) {
            CompletableFuture<JobStatus> jobStatus = restClient.getJobStatus(status.getLaunchJobID());
            JobStatus s = jobStatus.get(5, TimeUnit.SECONDS);
            if (s != null && !s.isTerminalState()) {

                jobFunc.apply(restClient, savePoint, status);
                //job 任务没有终止，立即停止
//                String savepointDirectory = savePoint.createSavePointPath();
//                CompletableFuture<String> result
//                        = restClient.stopWithSavepoint(status.getLaunchJobID(), true, savepointDirectory);
//                status.stop(result.get(25, TimeUnit.SECONDS));
            }

        } catch (Exception e) {
            throw new RuntimeException("appname:" + collection.getName(), e);
        }
    }

    private interface FlinkJobFunc {
        public void apply(RestClusterClient restClient
                , StateBackendFactory.ISavePointSupport savePoint, FlinkIncrJobStatus status) throws Exception;
    }


    @Override
    public void stopInstance(TargetResName collection) {
        processFlinkJob(collection, (restClient, savePoint, status) -> {
            //job 任务没有终止，立即停止
            String savepointDirectory = savePoint.createSavePointPath();
            // advanceToEndOfTime - flag indicating if the source should inject a MAX_WATERMARK in the pipeline
            CompletableFuture<String> result
                    = restClient.stopWithSavepoint(status.getLaunchJobID(), true, savepointDirectory);
            status.stop(result.get(3, TimeUnit.MINUTES));
            // status.stop(result.get());
        });
    }


    @Override
    public void removeInstance(TargetResName collection) throws Exception {
        FlinkIncrJobStatus status = getIncrJobStatus(collection);
        try {

            if (status.getLaunchJobID() == null) {
                throw new IllegalStateException("have not found any launhed job,app:" + collection.getName());
            }

            JobID jobID = status.getLaunchJobID();
            try (RestClusterClient restClient = this.factory.getFlinkCluster()) {
                // 先删除掉，可能cluster中
                CompletableFuture<JobStatus> jobStatus = restClient.getJobStatus(jobID);
                JobStatus s = jobStatus.get(5, TimeUnit.SECONDS);
                if (s != null && !s.isTerminalState()) {
                    //job 任务没有终止，立即停止
                    CompletableFuture<Acknowledge> result = restClient.cancel(jobID);
                    result.get(5, TimeUnit.SECONDS);
                }
                status.cancel();
            }
        } catch (Exception e) {
            Throwable[] throwables = ExceptionUtils.getThrowables(e);
            for (Throwable cause : throwables) {
                if (isNotFoundException(cause)) {
                    logger.warn(cause.getMessage() + ":" + collection.getName());
                    status.cancel();
                    return;
                }
            }
            throw new RuntimeException(e);
        }
    }


    @Override
    public WatchPodLog listPodAndWatchLog(TargetResName collection, String podName, ILogListener listener) {
        return null;
    }

    private class ValidateFlinkJob {
        protected TargetResName collection;
        private final FlinkIncrJobStatus status;
        private StateBackendFactory.ISavePointSupport savePoint;
        private boolean validateSucess = true;

        public ValidateFlinkJob fail() {
            this.validateSucess = false;
            return this;
        }

        public ValidateFlinkJob(TargetResName collection) {
            this.collection = collection;
            status = getIncrJobStatus(collection);
        }

        public FlinkIncrJobStatus getStatus() {
            return status;
        }

        public StateBackendFactory.ISavePointSupport getSavePoint() {
            return savePoint;
        }

        public ValidateFlinkJob valiate() {

            // IncrJobStatus launchJobID = getIncrJobRecordFile(collection);  // getLaunchJobID(collection);
            if (status.getState() != IFlinkIncrJobStatus.State.RUNNING) {
                processJobNotRunning();
                return fail();
            }
            if (status.getLaunchJobID() == null) {
                throw new IllegalStateException("have not found any launhed job,app:" + collection.getName());
            }

            StateBackendFactory stateBackend = factory.stateBackend;
            savePoint = null;

            if (!(stateBackend instanceof StateBackendFactory.ISavePointSupport)
                    || !(savePoint = (StateBackendFactory.ISavePointSupport) stateBackend).supportSavePoint()) {
                processCollectionNotSupportSavePoint(stateBackend);
                return fail();
            }
            return this;
        }

        protected void processCollectionNotSupportSavePoint(StateBackendFactory stateBackend) {
            throw new TisException("app:" + collection.getName()
                    + " is not support savePoint,stateFactoryClass:" + stateBackend.getClass().getName());
        }

        protected void processJobNotRunning() {
            throw new IllegalStateException("collection:" + collection.getName()
                    + " intend to stop incr processing,state must be running ,but now is " + status.getState());
        }
    }
}
