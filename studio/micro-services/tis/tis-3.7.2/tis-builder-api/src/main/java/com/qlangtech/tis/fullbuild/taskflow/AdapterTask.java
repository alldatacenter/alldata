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
package com.qlangtech.tis.fullbuild.taskflow;

import com.qlangtech.tis.exec.ExecChainContextUtils;
import com.qlangtech.tis.fs.ITaskContext;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.sql.parser.TabPartitions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年8月9日下午12:45:31
 */
public abstract class AdapterTask extends DataflowTask {

    public static final String KEY_TASK_WORK_STATUS = "TaskWorkStatus";

    // private String content;


    private ITaskContext taskContext;
    private IJoinTaskContext joinExecContext;


    protected final TabPartitions getDumpPartition() {
        TabPartitions dumpPartition = ExecChainContextUtils.getDependencyTablesPartitions(this.joinExecContext);
        return dumpPartition;
    }

    @Override
    protected Map<String, Boolean> getTaskWorkStatus() {
        return createTaskWorkStatus(this.joinExecContext);
    }

    public static Map<String, Boolean> createTaskWorkStatus(IJoinTaskContext chainContext) {
        Map<String, Boolean> taskWorkStatus = chainContext.getAttribute(KEY_TASK_WORK_STATUS);
        if (taskWorkStatus == null) {
            taskWorkStatus = new HashMap<>();
            chainContext.setAttribute(KEY_TASK_WORK_STATUS, taskWorkStatus);
        }
        return taskWorkStatus;
    }

    public AdapterTask(String id) {
        super(id);
    }

    public abstract String getName();

    /**
     * 取得运行时Hive连接对象
     *
     * @return
     */
    protected <T> T getTaskContextObj() {
        Objects.requireNonNull(this.taskContext, "task content can not be null");
        return this.taskContext.getObj();
    }

    // {
    // return HiveTaskFactory.getConnection(this.getContext());
    // }
    // protected abstract Connection getHiveConnection() ;// {
    // return HiveTaskFactory.getConnection(this.getContext());
    // }
    @Override
    public void run() throws Exception {
        try {
            // Map<String, Object> params = Collections.emptyMap();
            // String sql = getExecuteSQL();
            executeTask(this.getName());
            this.signTaskSuccess();
        } catch (Exception e) {
            this.signTaskFaild();
            throw e;
        }
    }

//    protected abstract String getExecuteSQL();


    protected abstract void executeTask(String taskname);

    public IJoinTaskContext getExecContext() {
        return this.joinExecContext;
    }

    public void setContext(IJoinTaskContext context, ITaskContext taskContext) {
        Objects.requireNonNull(context, "param context can not be null");
        Objects.requireNonNull(taskContext, "param taskContext can not be null");
        this.joinExecContext = context;
        this.taskContext = taskContext;
    }
}
