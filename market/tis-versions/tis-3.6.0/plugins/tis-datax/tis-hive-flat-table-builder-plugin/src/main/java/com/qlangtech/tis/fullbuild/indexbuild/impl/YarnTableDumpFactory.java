///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.fullbuild.indexbuild.impl;
//
//import com.qlangtech.tis.TIS;
//import com.qlangtech.tis.build.task.TaskMapper;
//import com.qlangtech.tis.config.ParamsConfig;
//import com.qlangtech.tis.config.yarn.IYarnConfig;
//import com.qlangtech.tis.dump.hive.BindHiveTableTool;
//import com.qlangtech.tis.dump.hive.HiveRemoveHistoryDataTask;
//import com.qlangtech.tis.extension.Descriptor;
//import com.qlangtech.tis.extension.TISExtension;
//import com.qlangtech.tis.fs.ITISFileSystem;
//import com.qlangtech.tis.fs.ITableBuildTask;
//import com.qlangtech.tis.fs.ITaskContext;
//import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
//import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
//import com.qlangtech.tis.fullbuild.indexbuild.TaskContext;
//import com.qlangtech.tis.offline.FileSystemFactory;
//import com.qlangtech.tis.offline.FlatTableBuilder;
//import com.qlangtech.tis.offline.TableDumpFactory;
//import com.qlangtech.tis.plugin.annotation.FormField;
//import com.qlangtech.tis.plugin.annotation.FormFieldType;
//import com.qlangtech.tis.plugin.annotation.Validator;
//import com.qlangtech.tis.plugin.datax.MREngine;
//import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
//import org.apache.hadoop.fs.Path;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.sql.Connection;
//import java.util.Set;
//import java.util.concurrent.Callable;
//import java.util.stream.Collectors;
//
///**
// * 基于YARN容器的表Dump实现
// *
// * @author 百岁（baisui@qlangtech.com）
// * @create: 2020-03-31 15:39
// * @date 2020/04/13
// */
//public class YarnTableDumpFactory extends TableDumpFactory implements IContainerPodSpec {
//
//    private static final Logger logger = LoggerFactory.getLogger(YarnTableDumpFactory.class);
//
//    private static final String KEY_FIELD_YARN_CONTAINER = "yarnCfg";
//
//    private static final String KEY_FIELD_FS_NAME = "fsName";
//
//    private static final String KEY_FIELD_FLAT_TABLE_BUILDER_NAME = "destination";
//
//    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
//    public String name;
//
//    @FormField(ordinal = 1, validate = {Validator.require, Validator.identity}, type = FormFieldType.SELECTABLE)
//    public String yarnCfg;
//
//    @FormField(ordinal = 3, validate = {Validator.require, Validator.identity}, type = FormFieldType.SELECTABLE)
//    public String fsName;
//
//    @FormField(ordinal = 4, validate = {Validator.require, Validator.identity}, type = FormFieldType.SELECTABLE)
//    public String destination;
//
//    @FormField(ordinal = 5, validate = {Validator.require}, type = FormFieldType.INT_NUMBER, dftVal = "1024")
//    public int maxHeapMemory;
//
//    @FormField(ordinal = 6, validate = {Validator.require}, type = FormFieldType.INT_NUMBER, dftVal = "1")
//    public int maxCPUCores;
//
//    @FormField(ordinal = 7, type = FormFieldType.INT_NUMBER)
//    public int runjdwpPort;
//
//    private MREngine engine = MREngine.HIVE;
//
//
//    @Override
//    public int getMaxHeapMemory() {
//        return maxHeapMemory;
//    }
//
//    @Override
//    public int getMaxCPUCores() {
//        return maxCPUCores;
//    }
//
//    @Override
//    public int getRunjdwpPort() {
//        return runjdwpPort;
//    }
//
//    private transient ITISFileSystem fileSystem;
//
//    private transient HiveRemoveHistoryDataTask removeHistoryDataTask;
//
//    @Override
//    public ITISFileSystem getFileSystem() {
//        return this.getFs();
//    }
//
//    /**
//     * 构建宽表<br>
//     * ref: ITableBuildTaskContext
//     *
//     * @param task
//     */
//    @Override
//    public void startTask(ITableBuildTask task) {
//        FlatTableBuilder flatTableBuilder
//                = TIS.getPluginStore(FlatTableBuilder.class).find(this.destination);
//        flatTableBuilder.startTask(task);
//    }
//
//    private HiveRemoveHistoryDataTask getHiveRemoveHistoryDataTask() {
//        if (removeHistoryDataTask == null) {
//            this.removeHistoryDataTask = new HiveRemoveHistoryDataTask(getFs(), MREngine.HIVE);
//        }
//        return removeHistoryDataTask;
//    }
//
//    private ITISFileSystem getFs() {
//        if (fileSystem == null) {
//            fileSystem = FileSystemFactory.getFsFactory(this.fsName).getFileSystem();
//        }
//        return fileSystem;
//    }
//
//    @Override
//    public void dropHistoryTable(EntityName dumpTable, ITaskContext context) {
//        Connection conn = context.getObj();
//        this.getHiveRemoveHistoryDataTask().dropHistoryHiveTable(dumpTable, conn);
//    }
//
//    @Override
//    public void deleteHistoryFile(EntityName dumpTable, ITaskContext context) {
//        Connection hiveConnection = context.getObj();
//        getHiveRemoveHistoryDataTask().deleteHdfsHistoryFile(dumpTable, hiveConnection);
//    }
//
//    @Override
//    public void deleteHistoryFile(EntityName dumpTable, ITaskContext context, String timestamp) {
//        Connection hiveConnection = context.getObj();
//        getHiveRemoveHistoryDataTask().deleteHdfsHistoryFile(dumpTable, hiveConnection, timestamp);
//    }
//
//    @Override
//    public void bindTables(Set<EntityName> hiveTables, final String timestamp, ITaskContext context) {
//        BindHiveTableTool.bindHiveTables(engine, this.getFs()
//                , hiveTables.stream().collect(Collectors.toMap((e) -> e, (e) -> {
//                    return new Callable<BindHiveTableTool.HiveBindConfig>() {
//                        @Override
//                        public BindHiveTableTool.HiveBindConfig call() throws Exception {
//                            Path tabDumpParentPath = getFs().getPath(getFs().getRootDir() + "/" + e.getNameWithPath() + "/all/" + timestamp).unwrap(Path.class);
//                            return new BindHiveTableTool.HiveBindConfig(BindHiveTableTool.getColumns(getFs(), e, timestamp), tabDumpParentPath);
//                        }
//                    };
//                }))
//                , timestamp
//                , context);
//    }
//
//    @Override
//    public IRemoteTaskTrigger createSingleTableDumpJob(IDumpTable table, TaskContext context) {
//
//        Hadoop020RemoteJobTriggerFactory dumpTriggerFactory
//                = new Hadoop020RemoteJobTriggerFactory(getYarnConfig(), getFs(), this);
//        return dumpTriggerFactory.createSingleTableDumpJob(table, context);
//    }
//
//    /**
//     * 执行服务端表dump任务
//     *
//     * @param taskMapper
//     * @param taskContext
//     */
//    @Override
//    public void startTask(TaskMapper taskMapper, TaskContext taskContext) throws Exception {
//        ServerTaskExecutor taskExecutor = new ServerTaskExecutor(this.getYarnConfig());
//        DefaultCallbackHandler callbackHandler = new DefaultCallbackHandler();
//        taskExecutor.startTask(taskMapper, taskContext, callbackHandler);
//    }
//
//    private IYarnConfig getYarnConfig() {
//        return ParamsConfig.getItem(this.yarnCfg, IYarnConfig.KEY_DISPLAY_NAME);
//    }
//
//
//    @TISExtension()
//    public static class DefaultDescriptor extends Descriptor<TableDumpFactory> {
//
//        public DefaultDescriptor() {
//            super();
//            this.registerSelectOptions(KEY_FIELD_YARN_CONTAINER, () -> ParamsConfig.getItems(IYarnConfig.KEY_DISPLAY_NAME));
//            this.registerSelectOptions(KEY_FIELD_FS_NAME, () -> TIS.getPluginStore(FileSystemFactory.class).getPlugins());
//            this.registerSelectOptions(KEY_FIELD_FLAT_TABLE_BUILDER_NAME, () -> TIS.getPluginStore(FlatTableBuilder.class).getPlugins());
//        }
//
//        @Override
//        public String getDisplayName() {
//            return "yarn";
//        }
//    }
//}
