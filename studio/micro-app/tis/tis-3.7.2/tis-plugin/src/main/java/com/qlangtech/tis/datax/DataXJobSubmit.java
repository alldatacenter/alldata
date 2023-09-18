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

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.job.DataXJobWorker;
import com.qlangtech.tis.extension.ExtensionList;
import com.qlangtech.tis.extension.TISExtensible;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DBIdentity;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.TableInDB;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.util.RobustReflectionConverter;
import com.qlangtech.tis.web.start.TisAppLaunch;
import com.tis.hadoop.rpc.RpcServiceReference;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-27 17:03
 **/
@TISExtensible
@Public
public abstract class DataXJobSubmit {
    public static final String KEY_DATAX_READERS = "dataX_readers";
    public static final int MAX_TABS_NUM_IN_PER_JOB = 40;

    public static Callable<DataXJobSubmit> mockGetter;

    @FormField(ordinal = 0, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public Integer parallelism;

    public static DataXJobSubmit.InstanceType getDataXTriggerType() {
        if (TisAppLaunch.isTestMock()) {
            return InstanceType.EMBEDDED;
        }
        DataXJobWorker jobWorker = DataXJobWorker.getJobWorker(DataXJobWorker.K8S_DATAX_INSTANCE_NAME);
        boolean dataXWorkerServiceOnDuty = jobWorker != null && jobWorker.inService();
        return dataXWorkerServiceOnDuty ? DataXJobSubmit.InstanceType.DISTRIBUTE : DataXJobSubmit.InstanceType.LOCAL;
    }

    public static Optional<DataXJobSubmit> getDataXJobSubmit(boolean dryRun, DataXJobSubmit.InstanceType expectDataXJobSumit) {
        try {
            if (mockGetter != null) {
                return Optional.ofNullable(mockGetter.call());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 如果是DryRun则只需要在内部执行
        final DataXJobSubmit.InstanceType targetType = dryRun ? InstanceType.EMBEDDED : expectDataXJobSumit;
//        if (joinTaskContext.isDryRun()) {
//            expectDataXJobSumit = InstanceType.EMBEDDED;
//        }

        ExtensionList<DataXJobSubmit> jobSumits = TIS.get().getExtensionList(DataXJobSubmit.class);
        Optional<DataXJobSubmit> jobSubmit = jobSumits.stream()
                .filter((jsubmit) -> (targetType) == jsubmit.getType()).findFirst();
        return jobSubmit;
    }

    public static Optional<DataXJobSubmit> getDataXJobSubmit(
            IJoinTaskContext joinTaskContext, DataXJobSubmit.InstanceType expectDataXJobSumit) {
        return getDataXJobSubmit(joinTaskContext.isDryRun(), expectDataXJobSumit);
    }

    public enum InstanceType {
        DISTRIBUTE("distribute") {
            @Override
            public boolean validate(IControlMsgHandler controlMsgHandler, Context context, List<DataXCfgGenerator.DataXCfgFile> cfgFileNames) {
                return true;
            }
        },
        EMBEDDED("embedded") {
            @Override
            public boolean validate(IControlMsgHandler controlMsgHandler, Context context, List<DataXCfgGenerator.DataXCfgFile> cfgFileNames) {
                return true;
            }
        }
        //
        , LOCAL("local") {
            @Override
            public boolean validate(IControlMsgHandler controlMsgHandler, Context context, List<DataXCfgGenerator.DataXCfgFile> cfgFileNames) {
                if (cfgFileNames.size() > MAX_TABS_NUM_IN_PER_JOB) {
                    controlMsgHandler.addErrorMessage(context, "单机版，单次表导入不能超过"
                            + MAX_TABS_NUM_IN_PER_JOB + "张，如需要导入更多表，请使用分布式K8S DataX执行期");
                    return false;
                }
                return true;
            }
        };
        public final String literia;

        public static InstanceType parse(String val) {
            for (InstanceType t : InstanceType.values()) {
                if (t.literia.equals(val)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("value:" + val + " is not illegal");
        }

        private InstanceType(String val) {
            this.literia = val;
        }

        public abstract boolean validate(IControlMsgHandler controlMsgHandler, Context context, List<DataXCfgGenerator.DataXCfgFile> cfgFileNames);
    }


    public abstract InstanceType getType();


    public CuratorDataXTaskMessage getDataXJobDTO(IDataXJobContext jobContext, DataXJobInfo dataXJobInfo, IDataxProcessor processor) {

        IJoinTaskContext taskContext = jobContext.getTaskContext();
        if (processor.getResType() == null) {
            throw new NullPointerException("dataXJobDTO.getResType() can not be null");
        }
        CuratorDataXTaskMessage msg = new CuratorDataXTaskMessage();
        if (taskContext.hasIndexName()) {
            msg.setDataXName(taskContext.getIndexName());
        } else {
            msg.setDataXName(processor.identityValue());
        }
        msg.setTaskSerializeNum(jobContext.getTaskSerializeNum());
        msg.setJobId(taskContext.getTaskId());
        msg.setJobName(dataXJobInfo.serialize());
        msg.setExecTimeStamp(taskContext.getPartitionTimestampWithMillis());
        msg.setResType(processor.getResType());

        PhaseStatusCollection preTaskStatus = taskContext.loadPhaseStatusFromLatest();
        DumpPhaseStatus.TableDumpStatus dataXJob = null;
        if (preTaskStatus != null
                && (dataXJob = preTaskStatus.getDumpPhase().getTable(dataXJobInfo.jobFileName)) != null
                && dataXJob.getAllRows() > 0
        ) {
            msg.setAllRowsApproximately(dataXJob.getReadRows());
        } else {
            msg.setAllRowsApproximately(-1);
        }
        return msg;
    }

    /**
     * 创建dataX任务
     *
     * @param taskContext
     * @param tabDataXEntity
     * @return
     */
    public final IRemoteTaskTrigger createDataXJob(IDataXJobContext taskContext
            , RpcServiceReference statusRpc, IDataxProcessor processor, TableDataXEntity tabDataXEntity //, List<String> dependencyTasks
    ) {
        final DataXJobInfo jobName = getDataXJobInfo(tabDataXEntity, taskContext, processor);
        if (this.getType() == InstanceType.DISTRIBUTE) {
            //TODO: 获取DataXProcess 相关元数据 用于远程分布式执行任务
            RobustReflectionConverter.PluginMetas pluginMetas
                    = RobustReflectionConverter.PluginMetas.collectMetas(() -> {

            });
        }

        CuratorDataXTaskMessage dataXJobDTO = getDataXJobDTO(taskContext, jobName, processor);

        return createDataXJob(taskContext, statusRpc, jobName, processor, dataXJobDTO);
    }

    protected abstract IRemoteTaskTrigger createDataXJob(IDataXJobContext taskContext
            , RpcServiceReference statusRpc, DataXJobInfo jobName
            , IDataxProcessor dataxProcessor, CuratorDataXTaskMessage dataXJobDTO);


    private DataXJobInfo getDataXJobInfo(
            final TableDataXEntity tabDataXEntity, IDataXJobContext taskContext, IDataxProcessor dataxProcessor) {

        List<IDataxReader> readers = taskContext.getTaskContext().getAttribute(KEY_DATAX_READERS
                , () -> dataxProcessor.getReaders(null));

        return getDataXJobInfo(tabDataXEntity, (p) -> {
            TableInDB tabsInDB = p.getLeft();
            DataXJobInfo jobName = tabsInDB.createDataXJobInfo(tabDataXEntity);
            return jobName;
        }, readers);
    }

    public static <T> T getDataXJobInfo(DBIdentity targetDBId, Function<Pair<TableInDB, IDataxReader>, T> convert, List<IDataxReader> readers) {

        for (IDataxReader reader : readers) {
            TableInDB tabsInDB = reader.getTablesInDB();
            if (tabsInDB.isMatch(targetDBId)) {
                return convert.apply(Pair.of(tabsInDB, reader));
            }
        }

        throw new IllegalStateException(targetDBId.toString());
    }


    public static class TableDataXEntity implements DBIdentity {
        public static final String TEST_JDBC_URL = "jdbc_url_test";
        public final DataXCfgGenerator.DBDataXChildTask fileName;
        private final ISelectedTab selectedTab;

        @Override
        public String identityValue() {
            return fileName.getDbFactoryId();
        }

        public static DataXJobSubmit.TableDataXEntity createTableEntity4Test(String dataXCfgFileName, String tabName) {
            return createTableEntity(dataXCfgFileName, TEST_JDBC_URL, tabName);
//            ISelectedTab selTab = new ISelectedTab() {
//                @Override
//                public String getName() {
//                    return tabName;
//                }
//
//                @Override
//                public List<CMeta> getCols() {
//                    throw new UnsupportedOperationException();
//                }
//            };
//            return new DataXJobSubmit.TableDataXEntity(
//                    new DataXCfgGenerator.DBDataXChildTask(TEST_JDBC_URL, dataXCfgFileName), selTab);
        }

        public static DataXJobSubmit.TableDataXEntity createTableEntity(String dataXCfgFileName, String dbIdenetity, String tabName) {

            ISelectedTab selTab = new ISelectedTab() {
                @Override
                public String getName() {
                    return tabName;
                }

                @Override
                public List<CMeta> getCols() {
                    throw new UnsupportedOperationException();
                }
            };
            return new DataXJobSubmit.TableDataXEntity(
                    new DataXCfgGenerator.DBDataXChildTask(dbIdenetity, null, dataXCfgFileName), selTab);
        }

        public TableDataXEntity(DataXCfgGenerator.DBDataXChildTask fileName, ISelectedTab selectedTab) {
            this.fileName = fileName;
            this.selectedTab = selectedTab;
        }

        public String getFileName() {
            return this.fileName.getDataXCfgFileNameWithSuffix();
        }

        public String getDbIdenetity() {
            return this.fileName.getDbIdenetity();
        }

        public ISelectedTab getSelectedTab() {
            return this.selectedTab;
        }

        public String getSourceTableName() {
            return this.selectedTab.getName();
        }

        @Override
        public String toString() {
            return "{" +
                    fileName +
                    ", selectedTab=" + selectedTab.getName() +
                    '}';
        }
    }

    public abstract IDataXJobContext createJobContext(IJoinTaskContext parentContext);


    public interface IDataXJobContext extends IDataXTaskRelevant {
        // public <T> T getContextInstance();

        IJoinTaskContext getTaskContext();

        AtomicInteger order = new AtomicInteger();

        /**
         * 保证一个批次执行的DataX任务的每个子任务都有一个唯一的序列号，例如在ODPS数据导入的场景中
         * ，MySQL中有多个分库的表需要导入到ODPS中采用pt+pmod（该值通过唯一序列号）的分区组合来避免不同分库数据导入相同分区的冲突
         *
         * @return
         */
        public default int getTaskSerializeNum() {
            return order.getAndIncrement();
        }

        public default String getFormatTime(TimeFormat format) {
            return format.format(getTaskContext().getPartitionTimestampWithMillis());
        }

        /**
         * 任务执行完成之后回收
         */
        void destroy();
    }

}
