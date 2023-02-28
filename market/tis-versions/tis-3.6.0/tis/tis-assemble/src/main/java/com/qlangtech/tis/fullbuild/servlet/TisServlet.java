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
package com.qlangtech.tis.fullbuild.servlet;

import com.google.common.collect.Maps;
import com.qlangtech.tis.assemble.ExecResult;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.assemble.TriggerType;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.ExecuteResult;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.exec.impl.DefaultChainContext;
import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.servlet.impl.HttpExecContext;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.common.DagTaskUtils;
import com.qlangtech.tis.manage.common.DagTaskUtils.NewTaskParam;
import com.qlangtech.tis.manage.common.TISCollectionUtils;
import com.qlangtech.tis.order.center.IndexSwapTaskflowLauncher;
import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 触发全量索引构建任务<br>
 * 例子： curl
 * 'http://localhost:8080/trigger?appname=search4totalpay&component.start=
 * tableJoin&ps=20160622110738'<br>
 * curl 'http://localhost:8080/trigger?component.start=indexBackflow&ps=
 * 20160623001000&appname=search4_fat_instance' <br>
 * curl 'http://localhost:8080/tis-assemble/trigger?component.start=indexBuild&ps=20200525134425&appname=search4totalpay&workflow_id=45&workflow_name=totalpay&index_shard_count=1&history.task.id=1'
 * <br>
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年11月6日 下午1:32:24
 */
public class TisServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TisServlet.class);

    private static final long serialVersionUID = 1L;

    private IndexSwapTaskflowLauncher indexSwapTaskflowLauncher;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.indexSwapTaskflowLauncher = IndexSwapTaskflowLauncher.getIndexSwapTaskflowLauncher(config.getServletContext());
//        ComponentMeta assembleComponent = TIS.getAssembleComponent();
//        assembleComponent.synchronizePluginsFromRemoteRepository();
        logger.info("synchronize Plugins FromRemoteRepository success");
    }

    static final ExecutorService executeService = Executors.newCachedThreadPool(new ThreadFactory() {

        int index = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("triggerTask#" + (index++));
            t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            });
            return t;
        }
    });

    // private static final AtomicBoolean idle = new AtomicBoolean(true);
    static final Map<String, ExecuteLock> idles = new HashMap<String, ExecuteLock>();

    // public TisServlet() {
    // super();
    // }

    /**
     * 校验参数是否正确
     *
     * @param execContext
     * @param req
     * @param res
     * @return
     * @throws ServletException
     */
    protected boolean isValidParams(HttpExecContext execContext, HttpServletRequest req, HttpServletResponse res) throws ServletException {
        return true;
    }

    protected boolean shallValidateCollectionExist() {
        return true;
    }

    /**
     * 执行任务终止
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpExecContext execContext = new HttpExecContext(req, Maps.newHashMap(), true);
        int taskId = execContext.getInt(JobCommon.KEY_TASK_ID);
        // 当前是否是异步任务
        final boolean asynJob = execContext.getBoolean(IExecChainContext.KEY_ASYN_JOB_NAME);
        String appName = execContext.getString(IFullBuildContext.KEY_APP_NAME);
        logger.info("receive a processing job CANCEL signal,taskId:{},asynJob:{},appName:{}", taskId, asynJob, appName);
        if (asynJob) {
            IncrStatusUmbilicalProtocolImpl incrController = IncrStatusUmbilicalProtocolImpl.getInstance();
            // 给远程进程服下毒丸，让其终止
            incrController.stop(appName);
        } else {
            Map.Entry<String, ExecuteLock> targetExecLock = null;
            synchronized (this) {
                for (Map.Entry<String, ExecuteLock> execLock : idles.entrySet()) {
                    if (execLock.getValue().matchTask(taskId)) {
                        targetExecLock = execLock;
                    }
                }
                if (targetExecLock == null) {
                    writeResult(false, "任务已经失效，无法终止", resp);
                    return;
                }

                targetExecLock.getValue().cancelAllFuture();
                targetExecLock.getValue().clearLockFutureQueue();
            }
        }

        PhaseStatusCollection phaseStatusCollection = TrackableExecuteInterceptor.getTaskPhaseReference(taskId);
        if (phaseStatusCollection != null) {
            // 这样会将当前状态写入本地磁盘
            phaseStatusCollection.flushStatus2Local();
        }

        writeResult(true, null, resp, new KV(JobCommon.KEY_TASK_ID, String.valueOf(taskId)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        final HttpExecContext execContext = createHttpExecContext(req);
        final MDCParamContext mdcContext = this.getMDCParam(execContext, res);
        try {
            if (!isValidParams(execContext, req, res)) {
                return;
            }
            if (!mdcContext.validateParam()) {
                return;
            }
            final ExecuteLock lock = mdcContext.getExecLock();
            // getLog().info("start to execute index swap work flow");
            final CountDownLatch countDown = new CountDownLatch(1);

            DefaultChainContext chainContext = new DefaultChainContext(execContext);
            chainContext.setMdcParamContext(mdcContext);
            chainContext.setAppSourcePipelineController(IncrStatusUmbilicalProtocolImpl.getInstance());


            final ExecuteLock.TaskFuture future = new ExecuteLock.TaskFuture();

            future.setFuture(executeService.submit(() -> {
                // MDC.put("app", indexName);
                getLog().info("index swap start to work");
                try {
                    while (true) {
                        try {
                            if (lock.lock()) {
                                final Integer newTaskId = createNewTask(chainContext);
                                future.setTaskId(newTaskId);
                                try {
                                    String msg = "trigger task" + mdcContext.getExecLockKey() + " successful";
                                    getLog().info(msg);
                                    mdcContext.resetParam(newTaskId);
                                    writeResult(true, msg, res, new KV(JobCommon.KEY_TASK_ID, String.valueOf(newTaskId)));

                                    countDown.countDown();
                                    /************************************************************
                                     * 开始执行内部任务
                                     ************************************************************/
                                    ExecResult execResult = startWork(chainContext).isSuccess() ? ExecResult.SUCCESS : ExecResult.FAILD;

                                    DagTaskUtils.createTaskComplete(newTaskId, chainContext, execResult);
                                } catch (InterruptedException e) {
                                    // 说明当前任务被 终止了
                                    logger.info("taskid:{} has been canceled", newTaskId);
                                    return;
                                } catch (Throwable e) {
                                    DagTaskUtils.createTaskComplete(newTaskId, chainContext, ExecResult.FAILD);
                                    getLog().error(e.getMessage(), e);
                                    throw new RuntimeException(e);
                                } finally {
                                    lock.clearLockFutureQueue();
                                }
                            } else {
                                if (lock.isExpire()) {
                                    getLog().warn("this lock has expire,this lock will cancel");
                                    // 执行已經超時
                                    lock.clearLockFutureQueue();
                                    // while (lock.futureQueue.size() >= 1)
                                    // {
                                    // lock.futureQueue.poll().cancel(true);
                                    // }
                                    getLog().warn("this lock[" + lock.getTaskOwnerUniqueName() + "] has expire,has unlocked");
                                    continue;
                                } else {
                                    String msg = "pre task[" + lock.getTaskOwnerUniqueName() + "] is executing ,so this commit will be ignore";
                                    getLog().warn(msg);
                                    writeResult(false, msg, res);
                                }
                                countDown.countDown();
                            }
                            // }
                            break;
                        } catch (Throwable e) {
                            getLog().error(e.getMessage(), e);
                            try {
                                if (countDown.getCount() > 0) {
                                    writeResult(false, ExceptionUtils.getMessage(e), res);
                                }
                            } catch (Exception e1) {
                            } finally {
                                try {
                                    countDown.countDown();
                                } catch (Throwable ee) {
                                }
                            }
                            break;
                        }
                    }
                } finally {
                    mdcContext.removeParam();
                }
                // end run
            }));

            mdcContext.getExecLock().addTaskFuture(future);

            try {
                countDown.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        } finally {
            mdcContext.removeParam();
        }
    }

//    protected final void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
//        super.service(req, res);
//    }

    private MDCParamContext getMDCParam(final HttpExecContext execContext, HttpServletResponse res) {
        final String indexName = execContext.getString(IFullBuildContext.KEY_APP_NAME);
        if (StringUtils.isNotEmpty(indexName)) {
            MDC.put("app", indexName);
            return StringUtils.startsWith(indexName, TISCollectionUtils.NAME_PREFIX) ?
                    new FullPhraseMDCParamContext(indexName, res)
                    : new DataXMDCParamContext(indexName, res);
        }
        Long wfid = execContext.getLong(IFullBuildContext.KEY_WORKFLOW_ID);
        MDC.put(IFullBuildContext.KEY_WORKFLOW_ID, String.valueOf(wfid));
        return new JustDataFlowMDCParamContext(wfid, res);
    }

    private abstract class MDCParamContext implements IRebindableMDC {

        protected final HttpServletResponse res;

        Integer taskid;

        public MDCParamContext(HttpServletResponse res) {
            super();
            this.res = res;
        }

        abstract boolean validateParam() throws ServletException;

        protected abstract String getExecLockKey();

        public final ExecuteLock getExecLock() {
            ExecuteLock lock = idles.get(getExecLockKey());
            if (lock == null) {
                synchronized (TisServlet.class) {
                    lock = idles.get(getExecLockKey());
                    if (lock == null) {
                        lock = new ExecuteLock(getExecLockKey());
                        idles.put(getExecLockKey(), lock);
                    }
                }
            }
            return lock;
        }

        void resetParam(Integer taskid) {
            if (taskid == null || taskid < 1) {
                throw new IllegalArgumentException("param taskid can not be empty");
            }
            this.taskid = taskid;
            MDC.put(JobCommon.KEY_TASK_ID, String.valueOf(taskid));
        }

        /**
         * 当子流程在新的线程中执行，需要重新绑定上下文参数
         */
        @Override
        public void rebind() {
            this.resetParam(this.taskid);
        }

        abstract void removeParam();
    }

    private class JustDataFlowMDCParamContext extends MDCParamContext {

        private final Long workflowId;

        public JustDataFlowMDCParamContext(Long workflowId, HttpServletResponse res) {
            super(res);
            this.workflowId = workflowId;
        }

        @Override
        protected String getExecLockKey() {
            return IFullBuildContext.KEY_WORKFLOW_ID + "-" + this.getWorkflowId();
        }

        @Override
        void removeParam() {
            MDC.remove("app");
            MDC.remove(IFullBuildContext.KEY_WORKFLOW_ID);
        }

        @Override
        boolean validateParam() {
            return true;
        }

        @Override
        void resetParam(Integer taskid) {
            super.resetParam(taskid);
            MDC.put(IFullBuildContext.KEY_WORKFLOW_ID, String.valueOf(this.getWorkflowId()));
        }

        private Long getWorkflowId() {
            return this.workflowId;
        }
    }

    /**
     * 全阶段构参数上下文
     */
    private class FullPhraseMDCParamContext extends MDCParamContext {

        private final String indexName;

        public FullPhraseMDCParamContext(String indexName, HttpServletResponse res) {
            super(res);
            this.indexName = indexName;
        }

        protected String getExecLockKey() {
            return this.indexName;
        }

        private String getIndexName() {
            return this.indexName;
        }

        @Override
        void removeParam() {
            MDC.remove("app");
            MDC.remove(IFullBuildContext.KEY_WORKFLOW_ID);
        }

        @Override
        void resetParam(Integer taskid) {
            super.resetParam(taskid);
            MDC.put("app", indexName);
        }

        @Override
        boolean validateParam() throws ServletException {
            if (shallValidateCollectionExist() //&& !indexSwapTaskflowLauncher.containIndex(indexName)
            ) {
                String msg = "indexName:" + indexName + " is not acceptable";
                //getLog().error(msg + ",exist collection:{}", indexSwapTaskflowLauncher.getIndexNames());
                writeResult(false, msg, res);
                return false;
            }
            return true;
        }
    }

    private class DataXMDCParamContext extends FullPhraseMDCParamContext {
        public DataXMDCParamContext(String dataxName, HttpServletResponse res) {
            super(dataxName, res);
        }

        @Override
        boolean validateParam() throws ServletException {
            return true;
        }
    }

    /**
     * 创建新的task
     *
     * @param chainContext
     * @return taskid
     */
    Integer createNewTask(IExecChainContext chainContext) {
        Integer workflowId = chainContext.getWorkflowId();
        NewTaskParam newTaskParam = new NewTaskParam();
        ExecutePhaseRange executeRanage = chainContext.getExecutePhaseRange();
        if (chainContext.hasIndexName() || executeRanage.getEnd().bigThan(FullbuildPhase.JOIN)) {
            String indexname = chainContext.getIndexName();
            newTaskParam.setAppname(indexname);
        }
        String histroyTaskId = chainContext.getString(IFullBuildContext.KEY_BUILD_HISTORY_TASK_ID);
        if (StringUtils.isNotBlank(histroyTaskId)) {
            newTaskParam.setHistoryTaskId(Integer.parseInt(histroyTaskId));
        }
        newTaskParam.setWorkflowid(workflowId);
        newTaskParam.setExecuteRanage(executeRanage);

        newTaskParam.setTriggerType(TriggerType.MANUAL);
        Integer taskid = DagTaskUtils.createNewTask(newTaskParam);
        logger.info("create new taskid:" + taskid);
        chainContext.setAttribute(JobCommon.KEY_TASK_ID, taskid);
        return taskid;
    }

    protected Logger getLog() {
        return logger;
    }

    protected ExecuteResult startWork(final DefaultChainContext chainContext) throws Exception {
        return indexSwapTaskflowLauncher.startWork(chainContext);
    }

    protected HttpExecContext createHttpExecContext(HttpServletRequest req) {
        return new HttpExecContext(req);
    }

    protected void writeResult(boolean success, String msg, ServletResponse res, KV... kvs) throws ServletException {
        res.setContentType("text/json");
        try {
            JSONObject json = new JSONObject();
            json.put("success", success);
            if (StringUtils.isNotBlank(msg)) {
                json.put("msg", msg);
            }
            if (kvs != null) {
                JSONObject kvjson = new JSONObject();
                for (KV kv : kvs) {
                    kvjson.put(kv.key, kv.value);
                }
                json.put("biz", kvjson);
            }
            res.getWriter().write(json.toString(1));
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public static class KV {

        private final String key;

        private final String value;

        /**
         * @param key
         * @param value
         */
        public KV(String key, String value) {
            super();
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
