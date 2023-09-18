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
package com.qlangtech.tis.exec.impl;

import com.qlangtech.tis.ajax.AjaxResult;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.exec.*;
import com.qlangtech.tis.exec.datax.DataXAssembleSvcCompsite;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.phasestatus.impl.BasicPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.realtime.yarn.rpc.IncrStatusUmbilicalProtocol;
import com.qlangtech.tis.realtime.yarn.rpc.impl.AdapterStatusUmbilicalProtocol;
import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
import com.tis.hadoop.rpc.ITISRpcService;
import com.tis.hadoop.rpc.RpcServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 执行进度可跟踪的执行器
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月23日
 */
public abstract class TrackableExecuteInterceptor implements IExecuteInterceptor, ITaskPhaseInfo {

    private static final Logger log = LoggerFactory.getLogger(TrackableExecuteInterceptor.class);

    private static final Map<Integer, PhaseStatusCollection> /*** taskid*/
            taskPhaseReference = new HashMap<>();

    public static PhaseStatusCollection initialTaskPhase(Integer taskid) {
        PhaseStatusCollection statusCollection = new PhaseStatusCollection(taskid, ExecutePhaseRange.fullRange());
        taskPhaseReference.put(taskid, statusCollection);
        return statusCollection;
    }

    public static PhaseStatusCollection getTaskPhaseReference(Integer taskId) {
        PhaseStatusCollection status = taskPhaseReference.get(taskId);
        // Objects.requireNonNull(status, "taskId:" + taskId + " relevant status can not be null");
        return status;
    }

    protected RpcServiceReference getDataXExecReporter() {
        IncrStatusUmbilicalProtocolImpl statusServer = IncrStatusUmbilicalProtocolImpl.getInstance();
        IncrStatusUmbilicalProtocol statReceiveSvc = new AdapterStatusUmbilicalProtocol() {
            @Override
            public void reportDumpTableStatus(DumpPhaseStatus.TableDumpStatus tableDumpStatus) {
//                statusServer.reportDumpTableStatus(tableDumpStatus.getTaskid(), tableDumpStatus.isComplete()
//                        , tableDumpStatus.isWaiting(), tableDumpStatus.isFaild(), tableDumpStatus.getName());

                statusServer.reportDumpTableStatus(tableDumpStatus);
            }
        };
        AtomicReference<ITISRpcService> ref = new AtomicReference<>();
        ref.set(new DataXAssembleSvcCompsite(statReceiveSvc));
        return new RpcServiceReference(ref, () -> {
        });
    }

    /**
     * 标记当前任务的ID
     *
     * @return
     */
    @Override
    @SuppressWarnings("all")
    public <T extends BasicPhaseStatus<?>> T getPhaseStatus(IExecChainContext execContext, FullbuildPhase phase) {
        PhaseStatusCollection phaseStatusCollection = taskPhaseReference.get(execContext.getTaskId());
        Objects.requireNonNull(phaseStatusCollection, "phaseStatusCollection can not be null");
        switch (phase) {
            case FullDump:
                return (T) phaseStatusCollection.getDumpPhase();
            case JOIN:
                return (T) phaseStatusCollection.getJoinPhase();
            case BUILD:
                return (T) phaseStatusCollection.getBuildPhase();
            case IndexBackFlow:
                return (T) phaseStatusCollection.getIndexBackFlowPhaseStatus();
            default:
                throw new IllegalStateException(phase + " is illegal has not any match status");
        }
    }

    @Override
    public final ExecuteResult intercept(ActionInvocation invocation) throws Exception {
        IExecChainContext execChainContext = invocation.getContext();
        int taskid = execChainContext.getTaskId();
        log.info("phase:" + FullbuildPhase.desc(this.getPhase()) + " start ,taskid:" + taskid);
        // 开始执行一个新的phase需要通知console
        // final int phaseId = createNewPhase(taskid, FullbuildPhase.getFirst(this.getPhase()));
        ExecuteResult result = null;
        try {
            result = this.execute(execChainContext);
            if (!result.isSuccess()) {
                log.error("taskid:" + taskid + ",phase:" + FullbuildPhase.desc(this.getPhase()) + " faild,reason:" + result.getMessage());
            }
        } catch (Exception e) {
            // }
            throw e;
        }
        if (result.isSuccess()) {
            return invocation.invoke();
        } else {
            log.error("full build job is failed");
            // StringUtils.EMPTY);
            return result;
        }
    }

    /**
     * 执行
     *
     * @param execChainContext
     * @return
     * @throws Exception
     */
    protected abstract ExecuteResult execute(IExecChainContext execChainContext) throws Exception;

    /**
     * 创建新的Task执行结果
     */
    public static class IntegerAjaxResult extends AjaxResult<Integer> {
    }

    public static class CreateNewTaskResult {

        private int taskid;

        private Application app;

        public CreateNewTaskResult() {
            super();
        }

        public int getTaskid() {
            return taskid;
        }

        public void setTaskid(int taskid) {
            this.taskid = taskid;
        }

        public void setApp(Application app) {
            this.app = app;
        }

        public Application getApp() {
            return app;
        }
    }

}
