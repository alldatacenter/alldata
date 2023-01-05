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
package com.qlangtech.tis.datax;


import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.statistics.PerfTrace;
import com.alibaba.datax.common.statistics.VMInfo;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.AbstractContainer;
import com.alibaba.datax.core.Engine;
import com.alibaba.datax.core.job.JobContainer;
import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.communication.CommunicationTool;
import com.alibaba.datax.core.statistics.container.communicator.job.StandAloneJobContainerCommunicator;
import com.alibaba.datax.core.util.ConfigParser;
import com.alibaba.datax.core.util.ConfigurationValidate;
import com.alibaba.datax.core.util.FrameworkErrorCode;
import com.alibaba.datax.core.util.container.CoreConstant;
import com.alibaba.datax.core.util.container.JarLoader;
import com.alibaba.datax.core.util.container.LoadUtil;
import com.gilt.logback.flume.tis.TisFlumeLogstashV1Appender;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.DagTaskUtils;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.order.center.IAppSourcePipelineController;
import com.qlangtech.tis.plugin.ComponentMeta;
import com.qlangtech.tis.plugin.IRepositoryResource;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.realtime.transfer.TableSingleDataIndexStatus;
import com.qlangtech.tis.realtime.utils.NetUtils;
import com.qlangtech.tis.realtime.yarn.rpc.MasterJob;
import com.qlangtech.tis.realtime.yarn.rpc.UpdateCounterMap;
import com.tis.hadoop.rpc.RpcServiceReference;
import com.tis.hadoop.rpc.StatusRpcClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 执行DataX任务入口
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-20 12:38
 */
public class DataxExecutor {
    public static int DATAX_THREAD_PROCESSING_CANCAL_EXITCODE = 943;
    private static final Logger logger = LoggerFactory.getLogger(DataxExecutor.class);

    public static void synchronizeDataXPluginsFromRemoteRepository(String dataxName, String jobName) {

        if (CenterResource.notFetchFromCenterRepository()) {
            return;
        }

        TIS.permitInitialize = false;
        try {
            if (StringUtils.isBlank(dataxName)) {
                throw new IllegalArgumentException("param dataXName can not be null");
            }
            if (StringUtils.isBlank(jobName)) {
                throw new IllegalArgumentException("param jobName can not be null");
            }

            KeyedPluginStore<DataxProcessor> processStore = IAppSource.getPluginStore(null, dataxName);
            List<IRepositoryResource> keyedPluginStores = Lists.newArrayList();
            keyedPluginStores.add(TIS.getPluginStore(ParamsConfig.class));
            keyedPluginStores.add(processStore);
            keyedPluginStores.add(DataxReader.getPluginStore(null, dataxName));
            keyedPluginStores.add(DataxWriter.getPluginStore(null, dataxName));

            ComponentMeta dataxComponentMeta = new ComponentMeta(keyedPluginStores);
            dataxComponentMeta.synchronizePluginsFromRemoteRepository();

            CenterResource.copyFromRemote2Local(
                    Config.KEY_TIS_PLUGIN_CONFIG + "/" + processStore.key.getSubDirPath()
                            + "/" + DataxProcessor.DATAX_CFG_DIR_NAME + "/" + jobName, true);

            CenterResource.synchronizeSubFiles(
                    Config.KEY_TIS_PLUGIN_CONFIG + "/" + processStore.key.getSubDirPath() + "/" + DataxProcessor.DATAX_CREATE_DDL_DIR_NAME);

        } finally {
            TIS.permitInitialize = true;
        }
    }

    /**
     * 入口开始执行
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            throw new IllegalArgumentException("args length must be 6,but now is " + args.length);
        }
        Integer jobId = Integer.parseInt(args[0]);
        String jobName = args[1];
        String dataXName = args[2];
        String incrStateCollectAddress = args[3];
        DataXJobSubmit.InstanceType execMode = DataXJobSubmit.InstanceType.parse(args[4]);

        final int allRows = Integer.parseInt(args[5]);
        // 任务每次执行会生成一个时间戳
        // final String execTimeStamp = args[6];
        //configuration.set(DataxUtils.EXEC_TIMESTAMP, args.execTimeStamp);
        if (StringUtils.isEmpty(System.getProperty(DataxUtils.EXEC_TIMESTAMP))) {
            throw new IllegalArgumentException("system prop '" + DataxUtils.EXEC_TIMESTAMP + "' can not be empty");
        }
        MDC.put(JobCommon.KEY_TASK_ID, String.valueOf(jobId));
        MDC.put(JobCommon.KEY_COLLECTION, dataXName);

        if (StringUtils.isEmpty(jobName)) {
            throw new IllegalArgumentException("arg 'jobName' can not be null");
        }
        if (StringUtils.isEmpty(dataXName)) {
            throw new IllegalArgumentException("arg 'dataXName' can not be null");
        }
        if (StringUtils.isEmpty(incrStateCollectAddress)) {
            throw new IllegalArgumentException("arg 'incrStateCollectAddress' can not be null");
        }

        StatusRpcClient.AssembleSvcCompsite statusRpc = StatusRpcClient.connect2RemoteIncrStatusServer(incrStateCollectAddress);
        Runtime.getRuntime().addShutdownHook(new Thread("dataX ShutdownHook") {
            @Override
            public void run() {
                statusRpc.close();
                // if (flumeAppendEnable) {
                TisFlumeLogstashV1Appender.instance.stop();
                // }
            }
        });

        DataxExecutor dataxExecutor = new DataxExecutor(new RpcServiceReference(new AtomicReference<>(statusRpc), () -> {
        }), execMode, allRows);

        if (execMode == DataXJobSubmit.InstanceType.DISTRIBUTE) {
            // 如果是分布式执行状态，需要通过RPC的方式来监听监工是否执行了客户端终止操作
            Object thread = monitorDistributeCommand(jobId, jobName, dataXName, statusRpc, dataxExecutor);
            Objects.requireNonNull(thread);
            DataxExecutor.synchronizeDataXPluginsFromRemoteRepository(dataXName, jobName);
        }

        try {
            dataxExecutor.reportDataXJobStatus(false, false, false, jobId, jobName);
            dataxExecutor.exec(jobId, jobName, dataXName);
            dataxExecutor.reportDataXJobStatus(false, jobId, jobName);
        } catch (Throwable e) {
            dataxExecutor.reportDataXJobStatus(true, jobId, jobName);
            logger.error(e.getMessage(), e);
            try {
                //确保日志向远端写入了
                Thread.sleep(3000);
            } catch (InterruptedException ex) {

            }
            System.exit(1);
        }
        logger.info("dataX:" + dataXName + ",taskid:" + jobId + " finished");
        System.exit(0);
    }

    private static Thread monitorDistributeCommand(Integer jobId, String jobName, String dataXName
            , StatusRpcClient.AssembleSvcCompsite statusRpc, DataxExecutor dataxExecutor) {
        Thread overseerListener = new Thread() {
            @Override
            public void run() {
                UpdateCounterMap status = new UpdateCounterMap();
                status.setFrom(NetUtils.getHost());
                logger.info("start to listen the dataX job taskId:{},jobName:{},dataXName:{} overseer cancel", jobId, jobName, dataXName);
                TableSingleDataIndexStatus dataXStatus = new TableSingleDataIndexStatus();
                dataXStatus.setUUID(jobName);
                status.addTableCounter(IAppSourcePipelineController.DATAX_FULL_PIPELINE + dataXName, dataXStatus);

                while (true) {
                    status.setUpdateTime(System.currentTimeMillis());
                    MasterJob masterJob = statusRpc.reportStatus(status);
                    if (masterJob != null && masterJob.isStop()) {
                        logger.info("datax job:{},taskid:{} has received an CANCEL signal", jobName, jobId);
                        dataxExecutor.reportDataXJobStatus(true, jobId, jobName);
                        System.exit(DATAX_THREAD_PROCESSING_CANCAL_EXITCODE);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        overseerListener.setUncaughtExceptionHandler((thread, e) -> logger.error("jobId:" + jobId + ",jobName:" + jobName, e));
        overseerListener.start();
        return overseerListener;
    }

    public void exec(Integer jobId, String jobName, String dataxName) throws Exception {
        final JarLoader uberClassLoader = new TISJarLoader(TIS.get().getPluginManager());
        LoadUtil.cleanJarLoaderCenter();
        this.exec(uberClassLoader, jobId, jobName, dataxName);
    }


    public void exec(final JarLoader uberClassLoader, Integer jobId, String jobName, String dataxName) throws Exception {
        if (uberClassLoader == null) {
            throw new IllegalArgumentException("param uberClassLoader can not be null");
        }
        boolean success = false;
        MDC.put(JobCommon.KEY_TASK_ID, String.valueOf(jobId));
        try {
            logger.info("process DataX job, dataXName:{},jobid:{},jobName:{}", dataxName, jobId, jobName);


            DataxProcessor dataxProcessor = DataxProcessor.load(null, dataxName);
            this.startWork(dataxName, jobId, jobName, dataxProcessor, uberClassLoader);
            success = true;
        } finally {
            TIS.clean();
            if (execMode == DataXJobSubmit.InstanceType.DISTRIBUTE) {
                try {
                    DagTaskUtils.feedbackAsynTaskStatus(jobId, jobName, success);
                } catch (Throwable e) {
                    logger.warn("notify exec result faild,jobId:" + jobId + ",jobName:" + jobName, e);
                }
            }
        }
    }


    private static final MessageFormat FormatKeyPluginReader = new MessageFormat("plugin.reader.{0}");
    private static final MessageFormat FormatKeyPluginWriter = new MessageFormat("plugin.writer.{0}");

    private IDataXPluginMeta.DataXMeta readerMeta;
    private IDataXPluginMeta.DataXMeta writerMeta;


    private final RpcServiceReference statusRpc;
    //private final JarLoader uberClassLoader;
    private DataXJobSubmit.InstanceType execMode;
    private final int allRowsApproximately;
    private final long[] allReadApproximately = new long[1];

    public DataxExecutor(RpcServiceReference statusRpc, DataXJobSubmit.InstanceType execMode, int allRows) {
        this.statusRpc = statusRpc;
        this.execMode = execMode;
        this.allRowsApproximately = allRows;
    }


    /**
     * 开始执行数据同步任务
     *
     * @param dataxName
     * @throws IOException
     * @throws Exception
     */
    public void startWork(String dataxName, Integer jobId, String jobName, IDataxProcessor dataxProcessor
            , final JarLoader uberClassLoader) throws IOException, Exception {
        try {

            Objects.requireNonNull(dataxProcessor, "dataxProcessor can not be null");
            KeyedPluginStore<DataxReader> readerStore = DataxReader.getPluginStore(null, dataxName);
            KeyedPluginStore<DataxWriter> writerStore = DataxWriter.getPluginStore(null, dataxName);
            File jobPath = new File(dataxProcessor.getDataxCfgDir(null), jobName);
//            String[] args = new String[]{
//                    "-mode", "standalone"
//                    , "-jobid", String.valueOf(jobId)
//                    , "-job", jobPath.getAbsolutePath()
//                    , "-execTimeStamp", execTimeStamp};

            ;

            DataxReader reader = readerStore.getPlugin();
            Objects.requireNonNull(reader, "dataxName:" + dataxName + " relevant reader can not be null");
            DataxWriter writer = writerStore.getPlugin();
            Objects.requireNonNull(writer, "dataxName:" + dataxName + " relevant writer can not be null");
            this.readerMeta = reader.getDataxMeta();
            this.writerMeta = writer.getDataxMeta();
            Objects.requireNonNull(readerMeta, "readerMeta can not be null");
            Objects.requireNonNull(writerMeta, "writerMeta can not be null");

            initializeClassLoader(Sets.newHashSet(this.getPluginReaderKey(), this.getPluginWriterKey()), uberClassLoader);


            entry(new DataXJobArgs(jobPath, jobId, "standalone"), jobName);

        } catch (Throwable e) {
            throw new Exception(e);
        } finally {
            cleanPerfTrace();
        }

    }

    public static void initializeClassLoader(Set<String> pluginKeys, JarLoader classLoader) throws IllegalAccessException {
//        Map<String, JarLoader> jarLoaderCenter = (Map<String, JarLoader>) jarLoaderCenterField.get(null);
//        jarLoaderCenter.clear();
//
//        for (String pluginKey : pluginKeys) {
//            jarLoaderCenter.put(pluginKey, classLoader);
//        }
//        Objects.requireNonNull(jarLoaderCenter, "jarLoaderCenter can not be null");
        LoadUtil.initializeJarClassLoader(pluginKeys, classLoader);
    }

    public void reportDataXJobStatus(boolean faild, Integer taskId, String jobName) {
        reportDataXJobStatus(faild, true, false, taskId, jobName);
    }

    public void reportDataXJobStatus(boolean faild, boolean complete, boolean waiting, Integer taskId, String jobName) {
        StatusRpcClient.AssembleSvcCompsite svc = statusRpc.get();
        int readed = (int) allReadApproximately[0];
        boolean success = (complete && !faild);
        svc.reportDumpJobStatus(faild, complete, waiting, taskId, jobName, readed, (success ? readed : this.allRowsApproximately));
    }

    private static class DataXJobArgs {
        private final File jobPath;
        private final Integer jobId;
        private final String runtimeMode;
//        private final String execTimeStamp;

        public DataXJobArgs(File jobPath, Integer jobId, String runtimeMode) {
            this.jobPath = jobPath;
            this.jobId = jobId;
            this.runtimeMode = runtimeMode;
//            if (StringUtils.isEmpty(execTimeStamp)) {
//                throw new IllegalArgumentException("param execTimeStamp can not be empty");
//            }
//            this.execTimeStamp = execTimeStamp;
        }

        @Override
        public String toString() {
            return "{" +
                    "jobPath=" + jobPath.getAbsolutePath() +
                    ", jobId=" + jobId +
                    ", runtimeMode='" + runtimeMode + '\'' +
                    '}';
        }
    }

    public void entry(DataXJobArgs args, String jobName) throws Throwable {
        Configuration configuration = parse(args);
        logger.info("exec params:{}", args.toString());
        Objects.requireNonNull(configuration, "configuration can not be null");
        int jobId = args.jobId;

        boolean isStandAloneMode = "standalone".equalsIgnoreCase(args.runtimeMode);
        if (!isStandAloneMode && jobId == -1L) {
            throw DataXException.asDataXException(FrameworkErrorCode.CONFIG_ERROR, "非 standalone 模式必须在 URL 中提供有效的 jobId.");
        }

        VMInfo vmInfo = VMInfo.getVmInfo();
        if (vmInfo != null) {
            logger.info(vmInfo.toString());
        }

        logger.info("\n" + filterJobConfiguration(configuration) + "\n");
        logger.debug(configuration.toJSON());
        ConfigurationValidate.doValidate(configuration);
        startEngine(configuration, jobId, jobName);

    }

    protected void startEngine(Configuration configuration, Integer jobId, String jobName) {

        Engine engine = new Engine() {
            @Override
            protected JobContainer createJobContainer(Configuration allConf) {
                return new TISDataXJobContainer(allConf, jobId, jobName);
            }
        };
        AbstractContainer dataXContainer = engine.start(configuration);
        setAllReadApproximately(dataXContainer.getContainerCommunicator().collect());
    }

    private class TISDataXJobContainer extends JobContainer {
        private final Integer jobId;
        private final String jobName;
        // private final Integer allRows;

        public TISDataXJobContainer(Configuration configuration, Integer jobId, String jobName) {
            super(configuration);
            this.jobId = jobId;
            this.jobName = jobName;
        }

        @Override
        protected StandAloneJobContainerCommunicator createContainerCommunicator(Configuration configuration) {
            return new StandAloneJobContainerCommunicator(configuration) {
                @Override
                public void report(Communication communication) {
                    super.report(communication);
                    setAllReadApproximately(communication);
                    reportDataXJobStatus(false, false, false, jobId, jobName);
                }
            };
        }
    }

    private void setAllReadApproximately(Communication communication) {
        allReadApproximately[0] = communication.getLongCounter(CommunicationTool.TOTAL_READ_RECORDS);
    }

    /**
     * 指定Job配置路径，ConfigParser会解析Job、Plugin、Core全部信息，并以Configuration返回
     */
    public Configuration parse(DataXJobArgs args) {
        final String jobPath = args.jobPath.getAbsolutePath();
        Configuration configuration = ConfigParser.parseJobConfig(jobPath);

        Configuration readerCfg = Configuration.newDefault();
        readerCfg.set("class", this.readerMeta.getImplClass());
        Configuration writerCfg = Configuration.newDefault();
        writerCfg.set("class", this.writerMeta.getImplClass());
        configuration.set(getPluginReaderKey(), readerCfg);
        configuration.set(getPluginWriterKey(), writerCfg);

        final String dataXKey = "job.content[0]." + DataxUtils.DATAX_NAME;
        final String dataxName = configuration.getString(dataXKey);
        if (StringUtils.isEmpty(dataxName)) {
            throw new IllegalStateException("param " + dataXKey + " can not be null");
        }
        configuration.set("job.content[0].reader.parameter." + DataxUtils.DATAX_NAME, dataxName);
        configuration.set("job.content[0].writer.parameter." + DataxUtils.DATAX_NAME, dataxName);

        String readerPluginName = configuration.getString("job.content[0].reader.name");
        String writerPluginName = configuration.getString("job.content[0].writer.name");

        DataXCfgGenerator.validatePluginName(writerMeta, readerMeta, writerPluginName, readerPluginName);

        Configuration coreCfg = Configuration.from(IOUtils.loadResourceFromClasspath(DataxExecutor.class, "core.json"));
        coreCfg.set(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID, args.jobId);

        configuration.merge(coreCfg,
                //ConfigParser.parseCoreConfig(CoreConstant.DATAX_CONF_PATH),
                false);


        Objects.requireNonNull(configuration.get(getPluginReaderKey()), FormatKeyPluginReader + " can not be null");
        Objects.requireNonNull(configuration.get(getPluginWriterKey()), FormatKeyPluginWriter + " can not be null");
        return configuration;
        // todo config优化，只捕获需要的plugin
    }

    private String getPluginReaderKey() {
        Objects.requireNonNull(readerMeta, "readerMeta can not be null");
        return FormatKeyPluginReader.format(new String[]{readerMeta.getName()});
    }


    private String getPluginWriterKey() {
        Objects.requireNonNull(writerMeta, "writerMeta can not be null");
        return FormatKeyPluginWriter.format(new String[]{writerMeta.getName()});
    }


    // 注意屏蔽敏感信息
    public static String filterJobConfiguration(final Configuration configuration) {
        Configuration job = configuration.getConfiguration("job");
        if (job == null) {
            throw new IllegalStateException("job relevant info can not be null,\n" + configuration.toJSON());
        }
        Configuration jobConfWithSetting = job.clone();

        Configuration jobContent = jobConfWithSetting.getConfiguration("content");

        filterSensitiveConfiguration(jobContent);

        jobConfWithSetting.set("content", jobContent);

        return jobConfWithSetting.beautify();
    }

    public static Configuration filterSensitiveConfiguration(Configuration configuration) {
        Set<String> keys = configuration.getKeys();
        for (final String key : keys) {
            boolean isSensitive = StringUtils.endsWithIgnoreCase(key, "password")
                    || StringUtils.endsWithIgnoreCase(key, "accessKey");
            if (isSensitive && configuration.get(key) instanceof String) {
                configuration.set(key, configuration.getString(key).replaceAll(".", "*"));
            }
        }
        return configuration;
    }

    private void cleanPerfTrace() {
        try {
            Field istField = PerfTrace.class.getDeclaredField("instance");
            istField.setAccessible(true);

            istField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
