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

package com.qlangtech.tis.plugin.datax.hudi;

import com.alibaba.datax.plugin.writer.hudi.HudiConfig;
import com.alibaba.datax.plugin.writer.hudi.TypedPropertiesBuilder;
import com.google.common.collect.Maps;
import com.qlangtech.tis.config.hive.HiveUserToken;
import com.qlangtech.tis.config.hive.IHiveConnGetter;
import com.qlangtech.tis.config.hive.IHiveUserTokenVisitor;
import com.qlangtech.tis.config.hive.impl.IKerberosUserToken;
import com.qlangtech.tis.config.hive.impl.IUserNamePasswordHiveUserToken;
import com.qlangtech.tis.config.spark.ISparkConnGetter;
import com.qlangtech.tis.config.yarn.IYarnConfig;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.TISCollectionUtils;
import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.plugin.PluginAndCfgsSnapshot;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Manifest;

/**
 * Hudi 文件导入完成之后，开始执行同步工作
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-09 12:22
 **/
public class HudiDumpPostTask implements IRemoteTaskTrigger {


    private static Logger logger = LoggerFactory.getLogger(HudiDumpPostTask.class);

    private final HudiSelectedTab hudiTab;
    //private final List<String> dataXFileNames;
    // private final HudiTableMeta tabMeta;
    private final ISparkConnGetter sparkConnGetter;
    private final IHiveConnGetter hiveConnMeta;
    private final DataXHudiWriter hudiWriter;
    private final DataXCfgGenerator.GenerateCfgs generateCfgs;
    private final IExecChainContext execContext;


    private SparkAppHandle sparkAppHandle;

    public HudiDumpPostTask(IExecChainContext execContext, HudiSelectedTab hudiTab, DataXHudiWriter hudiWriter, DataXCfgGenerator.GenerateCfgs generateCfgs) {
        if (hudiTab == null) {
            throw new IllegalArgumentException("param tableName can not be empty");
        }
        this.hudiTab = hudiTab;
        this.execContext = execContext;
        //  this.dataXFileNames = dataXFileNames;
        // this.tabMeta = tabMeta;
        this.sparkConnGetter = hudiWriter.getSparkConnGetter();
        this.hiveConnMeta = hudiWriter.getHiveConnMeta();
        this.hudiWriter = hudiWriter;
        this.generateCfgs = generateCfgs;

    }

    public static IPath createTabDumpParentPath(ITISFileSystem fs, IPath tabDumpDir) {
        Objects.requireNonNull(fs, "ITISFileSystem can not be null");
        //IPath tabDumpDir = getDumpDir();
        return fs.getPath(tabDumpDir, "data");
    }

    @Override
    public List<String> getTaskDependencies() {
//        File dataXWorkDir = IDataxProcessor.getDataXWorkDir(null, this.hudiWriter.dataXName);
//        DataXCfgGenerator.GenerateCfgs generateCfgs = DataXCfgGenerator.GenerateCfgs.readFromGen(dataXWorkDir);
//        return generateCfgs.getGroupedChildTask().get(tableName);
        return this.generateCfgs.getDataXTaskDependencies(hudiTab.getName());
    }

    @Override
    public String getTaskName() {
        return "hudi_delta_" + this.hudiTab.getName();
    }

    @Override
    public void run() {

        ITISFileSystem fs = this.hudiWriter.getFs().getFileSystem();
        IPath dumpDir = HudiTableMeta.getDumpDir(fs, this.hudiTab.getName(), execContext.getPartitionTimestamp(), this.hiveConnMeta);
        IPath fsSourcePropsPath = fs.getPath(dumpDir, "meta/" + this.hudiTab.getName() + "-source.properties");


        try {
            this.writeSourceProps(fs, dumpDir, fsSourcePropsPath);
            SparkAppHandle handle = this.launchSparkRddConvert(fs, dumpDir, fsSourcePropsPath);
            if (handle != null) {
                try {
                    handle.stop();
                } catch (Throwable e) { }
            }
        } catch (Throwable e) {
            if (this.sparkAppHandle != null) {
                try {
                    this.sparkAppHandle.kill();
                } catch (Throwable ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }
            throw new RuntimeException(e);
        }
    }

    private SparkAppHandle launchSparkRddConvert(ITISFileSystem fs, IPath dumpDir, IPath fsSourcePropsPath) throws Exception {

        Map<String, String> env = Config.getInstance().getAllKV();
        File sparkHome = HudiConfig.getSparkHome();
        File sparkCfgDir = new File(sparkHome, "conf");
        this.hudiWriter.getFs().setConfigFile(sparkCfgDir);
        String mdcCollection = MDC.get(JobCommon.KEY_COLLECTION);
        final String taskId = MDC.get(JobCommon.KEY_TASK_ID);
        if (StringUtils.isEmpty(taskId)) {
            throw new IllegalStateException("mdc param taskId can not be null");
        }
        env.put(JobCommon.KEY_TASK_ID, taskId);
        if (StringUtils.isNotEmpty(mdcCollection)) {
            env.put(JobCommon.KEY_COLLECTION, mdcCollection);
        }
        env.put(IYarnConfig.ENV_YARN_CONF_DIR, String.valueOf(sparkCfgDir.toPath().normalize()));

        logger.info("environment props ===========================");
        for (Map.Entry<String, String> entry : env.entrySet()) {
            logger.info("key:{},value:{}", entry.getKey(), entry.getValue());
        }
        logger.info("=============================================");
        SparkLauncher handle = new SparkLauncher(env);
        //handle.setSparkHome()
        // handle.directory();
//        File logFile = new File(TisAppLaunchPort.getAssebleTaskDir(), "full-" + taskId + ".log");
//        FileUtils.touch(logFile);
//        handle.redirectError(logFile);
        // 测试用
        handle.redirectError(new File("error.log"));
        //  handle.redirectToLog(DataXHudiWriter.class.getName());
        // String tabName = this.getFileName();

        File hudiDependencyDir = HudiConfig.getHudiDependencyDir();

        final String[] jarExtend = new String[]{"jar"};
        File resJar = FileUtils.listFiles(hudiDependencyDir, jarExtend, false)
                .stream().findFirst().orElseThrow(
                        () -> new IllegalStateException("must have resJar hudiDependencyDir:" + hudiDependencyDir.getAbsolutePath()));

        File addedJars = new File(hudiDependencyDir, "lib");
        boolean[] hasAddJar = new boolean[1];
        FileUtils.listFiles(addedJars, jarExtend, false).forEach((jar) -> {
            handle.addJar(String.valueOf(jar.toPath().normalize()));
            hasAddJar[0] = true;
        });
        if (!hasAddJar[0]) {
            throw new IllegalStateException("path must contain jars:" + addedJars.getAbsolutePath());
        }
        handle.addJar(String.valueOf(addManifestCfgJar().toPath().normalize()));

        handle.setAppResource(String.valueOf(resJar.toPath().normalize()));
        // ISparkConnGetter sparkConnGetter = writerPlugin.getSparkConnGetter();
        logger.info("sparkCfgDir:{}", sparkCfgDir.getAbsolutePath());
        final String sparkMaster = sparkConnGetter.getSparkMaster(sparkCfgDir);
        handle.setMaster(sparkMaster);
        handle.setSparkHome(String.valueOf(sparkHome.toPath().normalize()));
        handle.setMainClass("com.alibaba.datax.plugin.writer.hudi.TISHoodieDeltaStreamer");

        if (IYarnConfig.KEY_DISPLAY_NAME.equalsIgnoreCase(sparkMaster)) {
            // 此时使用yarn方式连接
            // 由于在 @see org.apache.hudi.utilities.UtilHelpers 中将spark.eventLog.enabled开启，所以必须要设置hdfs log dir
            String fsDefault = this.hudiWriter.getFs().getFSAddress();
            boolean endWithSlash = StringUtils.endsWith(fsDefault, "/");
            fsDefault = fsDefault + (endWithSlash ? StringUtils.EMPTY : "/") + "tmp/spark-events";
            IPath eventDir = fs.getPath(fsDefault);
            if (!fs.exists(eventDir)) {
                fs.mkdirs(eventDir);
            }
            handle.setConf("spark.eventLog.dir", fsDefault);
        }

        handle.addAppArgs("--table-type", this.hudiWriter.getHudiTableType().getValue()
                , "--source-class", "org.apache.hudi.utilities.sources.AvroDFSSource"
                , "--source-ordering-field", hudiTab.sourceOrderingField
                , "--target-base-path", String.valueOf(HudiTableMeta.getHudiDataDir(fs, dumpDir))
                , "--target-table", this.hudiTab.getName() + "/" + hudiWriter.dataXName
                , "--props", String.valueOf(fsSourcePropsPath)
                , "--schemaprovider-class", "org.apache.hudi.utilities.schema.FilebasedSchemaProvider"
                , "--enable-sync"
        );

        if (hudiWriter.getHudiTableType() == HudiWriteTabType.MOR) {
            handle.addAppArgs("--disable-compaction");
        }
        // https://hudi.apache.org/docs/tuning-guide/

        StringBuffer javaOpts = new StringBuffer("-D" + Config.SYSTEM_KEY_LOGBACK_PATH_KEY + "=" + Config.SYSTEM_KEY__LOGBACK_HUDI);
        javaOpts.append(" -D" + Config.KEY_JAVA_RUNTIME_PROP_ENV_PROPS + "=true");


        if (Boolean.getBoolean(KEY_DELTA_STREM_DEBUG)) {
            // 测试中使用
            // javaOpts.append(" -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=18888");
            javaOpts.append(" -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=18888");

        }
        // handle.addSparkArg()
        // handle.setConf(SparkLauncher.EXECUTOR_EXTRA_JAVA_OPTIONS, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=28888");
        handle.setConf(SparkLauncher.DRIVER_EXTRA_JAVA_OPTIONS, javaOpts.toString());
//        handle.setConf(SparkLauncher.DRIVER_MEMORY, "4G");
//        handle.setConf(SparkLauncher.EXECUTOR_MEMORY, "6G");
//        handle.addSparkArg("--driver-memory", "1024M");
//        handle.addSparkArg("--executor-memory", "2G");
        //spark.eventLog.dir               hdfs://namenode:8021/directory

        this.hudiWriter.sparkSubmitParam.setHandle(handle);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        this.sparkAppHandle = handle.startApplication(new SparkAppHandle.Listener() {
            @Override
            public void stateChanged(SparkAppHandle sparkAppHandle) {
                SparkAppHandle.State state = sparkAppHandle.getState();
                if (state.isFinal()) {
                    // finalState[0] = state;
                    // System.out.println("Info:" + state + ",appId:" + sparkAppHandle.getAppId());
                    logger.info("spark job stateChanged Info:" + sparkAppHandle.getAppId() + ":" + sparkAppHandle.getState());
                    countDownLatch.countDown();
                }
            }

            @Override
            public void infoChanged(SparkAppHandle sparkAppHandle) {
                logger.info("spark job infoChanged change Info:" + sparkAppHandle.getAppId() + ":" + sparkAppHandle.getState());
            }
        });

        countDownLatch.await();
        if (sparkAppHandle.getState() != SparkAppHandle.State.FINISHED) {
            throw new TisException("spark app:" + sparkAppHandle.getAppId()
                    + " execute result not successful:" + sparkAppHandle.getState());
        }
        return sparkAppHandle;
    }

    /**
     * 将本地的配置映射到manifest中，可以让远端同步本地的配置及jar资源
     *
     * @throws Exception
     */
    private File addManifestCfgJar() throws Exception {
        File manifestJar = new File(Config.getPluginCfgDir(), IFullBuildContext.NAME_APP_DIR + "/"
                + execContext.getIndexName() + "/hudi_delta_stream/"
                + execContext.getTaskId() + "/" + PluginAndCfgsSnapshot.getTaskEntryName() + ".jar");

        if (!manifestJar.exists()) {
            Map<String, String> extraEnvProps = Maps.newHashMap();
            extraEnvProps.put(JobCommon.KEY_TASK_ID, String.valueOf(execContext.getTaskId()));
            extraEnvProps.put(JobCommon.KEY_COLLECTION, execContext.getIndexName());
            Pair<PluginAndCfgsSnapshot, Manifest> manifest = PluginAndCfgsSnapshot.createManifestCfgAttrs2File(manifestJar
                    , new TargetResName(execContext.getIndexName()), -1, Optional.of((meta) -> {
                        // 目前只需要同步hdfs相关的配置文件，hudi相关的tpi包因为体积太大且远端spark中用不上先不传了
                        boolean collect = !(meta.getPluginName().indexOf("hudi") > -1);
                        if (collect) {
                            // 与增量无关
                            collect = !meta.getPluginName().startsWith(TISSinkFactory.KEY_PLUGIN_TPI_CHILD_PATH);
                        }
                        return collect;
                    }), extraEnvProps);
            if (manifest.getLeft().appLastModifyTimestamp < 1) {
                throw new IllegalStateException("appname:" + execContext.getIndexName() + " relevant appLastModifyTimestamp illegal:"
                        + manifest.getLeft().appLastModifyTimestamp);
            }
        }
//        Manifest manifest = PluginAndCfgsSnapshot.createManifestCfgAttrs(new TargetResName(execContext.getIndexName()), -1);
//        try (JarOutputStream jaroutput = new JarOutputStream(
//                FileUtils.openOutputStream(manifestJar, false), manifest)) {
//            jaroutput.putNextEntry(new ZipEntry(PluginAndCfgsSnapshot.getTaskEntryName()));
//            jaroutput.flush();
//        }

        return manifestJar;
    }


    @Override
    public void cancel() {
        try {
            sparkAppHandle.stop();
            sparkAppHandle.kill();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private void writeSourceProps(ITISFileSystem fs, IPath dumpDir, IPath fsSourcePropsPath) {


        IPath sourceSchemaPath = HudiTableMeta.getTableSourceSchema(fs, dumpDir);// HudiTableMeta.createFsSourceSchema(fs, this.hudiTab.getName(), dumpDir, this.hudiTab);
        if (!fs.exists(sourceSchemaPath)) {
            throw new IllegalStateException("sourceSchemaPath:" + sourceSchemaPath.getName() + " is not exist");
        }
        IPath tabDumpParentPath = createTabDumpParentPath(fs, dumpDir);
        // 写csv文件属性元数据文件

        try (OutputStream write = fs.create(fsSourcePropsPath, true)) {
            // TypedProperties props = new TypedProperties();
            TypedPropertiesBuilder props = new TypedPropertiesBuilder();

            String shuffleParallelism = String.valueOf(this.hudiWriter.shuffleParallelism);
            props.setProperty("hoodie.upsert.shuffle.parallelism", shuffleParallelism);
            props.setProperty("hoodie.insert.shuffle.parallelism", (shuffleParallelism));
            props.setProperty("hoodie.delete.shuffle.parallelism", (shuffleParallelism));
            props.setProperty("hoodie.bulkinsert.shuffle.parallelism", (shuffleParallelism));
            props.setProperty("hoodie.embed.timeline.server", "false");
            //props.setProperty("hoodie.filesystem.view.type", "EMBEDDED_KV_STORE");
            props.setProperty("hoodie.filesystem.view.type", "MEMORY");

            // @see HoodieCompactionConfig.INLINE_COMPACT
            // props.setProperty("hoodie.compact.inline", (hudiTabType == HudiWriteTabType.MOR) ? "true" : "false");
            // BasicFSWriter writerPlugin = this.getWriterPlugin();
//https://spark.apache.org/docs/3.2.1/sql-data-sources-csv.html
            props.setProperty("hoodie.deltastreamer.source.dfs.root", String.valueOf(tabDumpParentPath));
//            props.setProperty("hoodie.deltastreamer.csv.header", Boolean.toString(CSVWriter.CSV_FILE_USE_HEADER));
//            props.setProperty("hoodie.deltastreamer.csv.sep", String.valueOf(CSVWriter.CSV_Column_Separator));
//            props.setProperty("hoodie.deltastreamer.csv.nullValue", CSVWriter.CSV_NULL_VALUE);
//            props.setProperty("hoodie.deltastreamer.csv.escape", String.valueOf(CSVWriter.CSV_ESCAPE_CHAR));
            //  props.setProperty("hoodie.deltastreamer.csv.escapeQuotes", "false");


            props.setProperty("hoodie.deltastreamer.schemaprovider.source.schema.file", String.valueOf(sourceSchemaPath));
            props.setProperty("hoodie.deltastreamer.schemaprovider.target.schema.file", String.valueOf(sourceSchemaPath));

            // please reference: DataSourceWriteOptions , HiveSyncConfig
            final IHiveConnGetter hiveMeta = this.hudiWriter.getHiveConnMeta();
            props.setProperty("hoodie.datasource.hive_sync.database", hiveMeta.getDbName());
            props.setProperty("hoodie.datasource.hive_sync.table", this.hudiTab.getName());

            if (this.hudiTab.getKeyGenerator() == null) {
                throw new IllegalStateException(this.hudiTab.getName() + " relevant hudiPlugin.partitionedBy can not be empty");
            }

            this.hudiTab.getKeyGenerator().setProps(props, this.hudiWriter);
//            props.setProperty("hoodie.datasource.hive_sync.partition_fields", hudiPlugin.partitionedBy);
//            // "org.apache.hudi.hive.MultiPartKeysValueExtractor";
//            // partition 分区值抽取类
//            props.setProperty("hoodie.datasource.hive_sync.partition_extractor_class"
//                    , "org.apache.hudi.hive.MultiPartKeysValueExtractor");

            HiveUserToken hiveUserToken = hiveMeta.getUserToken();
            // if (hiveUserToken.isPresent()) {
            hiveUserToken.accept(new IHiveUserTokenVisitor() {
                @Override
                public void visit(IKerberosUserToken token) {

                }

                @Override
                public void visit(IUserNamePasswordHiveUserToken token) {
                    props.setProperty("hoodie.datasource.hive_sync.username", token.getUserName());
                    props.setProperty("hoodie.datasource.hive_sync.password", token.getPassword());
                }
            });

//
//                IHiveUserToken token = hiveUserToken.get();

            //  }
            if (StringUtils.isEmpty(hiveMeta.getMetaStoreUrls())) {
                throw new IllegalStateException("hiveMeta:" + hiveMeta.identityValue() + " metaStoreUrls can not be empty");
            }
            // props.setProperty("hoodie.datasource.hive_sync.jdbcurl", hiveMeta.getMetaStoreUrls());
            props.setProperty("hoodie.datasource.hive_sync.metastore.uris", hiveMeta.getMetaStoreUrls());

            props.setProperty("hoodie.datasource.hive_sync.mode", "hms");

            //   props.setProperty("hoodie.datasource.write.recordkey.field", this.hudiTab.keyGenerator.getLiteriaRecordFields());
            //  props.setProperty("hoodie.datasource.write.partitionpath.field", hudiWriter.partitionedBy);

            props.store(write);

        } catch (IOException e) {
            throw new RuntimeException("faild to write " + tabDumpParentPath + " CSV file metaData", e);
        }
    }

}
