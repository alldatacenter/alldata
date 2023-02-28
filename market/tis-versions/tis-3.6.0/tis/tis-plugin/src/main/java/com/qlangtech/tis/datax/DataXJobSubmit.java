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

package com.qlangtech.tis.datax;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
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
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.web.start.TisAppLaunch;
import com.tis.hadoop.rpc.RpcServiceReference;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-27 17:03
 **/
@TISExtensible
@Public
public abstract class DataXJobSubmit {

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

    public static Optional<DataXJobSubmit> getDataXJobSubmit(DataXJobSubmit.InstanceType expectDataXJobSumit) {
        try {
            if (mockGetter != null) {
                return Optional.ofNullable(mockGetter.call());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ExtensionList<DataXJobSubmit> jobSumits = TIS.get().getExtensionList(DataXJobSubmit.class);
        Optional<DataXJobSubmit> jobSubmit = jobSumits.stream()
                .filter((jsubmit) -> (expectDataXJobSumit) == jsubmit.getType()).findFirst();
        return jobSubmit;
    }

    public enum InstanceType {
        DISTRIBUTE("distribute") {
            @Override
            public boolean validate(IControlMsgHandler controlMsgHandler, Context context, List<File> cfgFileNames) {
                return true;
            }
        },
        EMBEDDED("embedded") {
            @Override
            public boolean validate(IControlMsgHandler controlMsgHandler, Context context, List<File> cfgFileNames) {
                return true;
            }
        }
        //
        , LOCAL("local") {
            @Override
            public boolean validate(IControlMsgHandler controlMsgHandler, Context context, List<File> cfgFileNames) {
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

        public abstract boolean validate(IControlMsgHandler controlMsgHandler, Context context, List<File> cfgFileNames);
    }


    public abstract InstanceType getType();


    protected CuratorDataXTaskMessage getDataXJobDTO(IJoinTaskContext taskContext, String dataXfileName) {
        CuratorDataXTaskMessage msg = new CuratorDataXTaskMessage();
        msg.setDataXName(taskContext.getIndexName());
        msg.setJobId(taskContext.getTaskId());
        msg.setJobName(dataXfileName);
        msg.setExecTimeStamp(taskContext.getPartitionTimestamp());
        PhaseStatusCollection preTaskStatus = taskContext.loadPhaseStatusFromLatest(taskContext.getIndexName());
        DumpPhaseStatus.TableDumpStatus dataXJob = null;
        if (preTaskStatus != null
                && (dataXJob = preTaskStatus.getDumpPhase().getTable(dataXfileName)) != null
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
     * @param dataXfileName
     * @param dependencyTasks 前置依赖需要执行的任务节点
     * @return
     */
    public abstract IRemoteTaskTrigger createDataXJob(IDataXJobContext taskContext
            , RpcServiceReference statusRpc, IDataxProcessor dataxProcessor, String dataXfileName, List<String> dependencyTasks);


    public abstract IDataXJobContext createJobContext(IJoinTaskContext parentContext);


    public interface IDataXJobContext {
        // public <T> T getContextInstance();

        IJoinTaskContext getTaskContext();

        /**
         * 任务执行完成之后回收
         */
        void destroy();
    }

}
