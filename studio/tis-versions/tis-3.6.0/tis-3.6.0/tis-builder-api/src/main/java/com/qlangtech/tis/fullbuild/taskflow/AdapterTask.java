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
//package com.qlangtech.tis.fullbuild.taskflow;
//
//import com.qlangtech.tis.exec.ExecChainContextUtils;
//import com.qlangtech.tis.fs.ITaskContext;
//import com.qlangtech.tis.order.center.IJoinTaskContext;
//import com.qlangtech.tis.sql.parser.TabPartitions;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2014年8月9日下午12:45:31
// */
//public abstract class AdapterTask extends DataflowTask {
//
//    public static final String KEY_TASK_WORK_STATUS = "TaskWorkStatus";
//
//    // private String content;
//    private ITemplateContext context;
//
//    private ITaskContext taskContext;
//
//    protected final TabPartitions getDumpPartition() {
//        TabPartitions dumpPartition = ExecChainContextUtils.getDependencyTablesPartitions(this.getContext().getExecContext());
//        return dumpPartition;
//    }
//
//    @Override
//    protected Map<String, Boolean> getTaskWorkStatus() {
//        return createTaskWorkStatus(this.getContext().getExecContext());
//    }
//
//    public static Map<String, Boolean> createTaskWorkStatus(IJoinTaskContext chainContext) {
//        Map<String, Boolean> taskWorkStatus = chainContext.getAttribute(KEY_TASK_WORK_STATUS);
//        if (taskWorkStatus == null) {
//            taskWorkStatus = new HashMap<>();
//            chainContext.setAttribute(KEY_TASK_WORK_STATUS, taskWorkStatus);
//        }
//        return taskWorkStatus;
//    }
//
//    public AdapterTask(String id) {
//        super(id);
//    }
//
//    public abstract String getName();
//
//    /**
//     * 取得运行时Hive连接对象
//     *
//     * @return
//     */
//    protected ITaskContext getTaskContext() {
//        Objects.requireNonNull(this.taskContext, "task content can not be null");
//        return this.taskContext;
//    }
//
//    // {
//    // return HiveTaskFactory.getConnection(this.getContext());
//    // }
//    // protected abstract Connection getHiveConnection() ;// {
//    // return HiveTaskFactory.getConnection(this.getContext());
//    // }
//    @Override
//    public void run() throws Exception {
//        try {
//            Map<String, Object> params = Collections.emptyMap();
//            String sql = mergeVelocityTemplate(params);
//            executeSql(this.getName(), sql);
//            this.signTaskSuccess();
//        } catch (Exception e) {
//            this.signTaskFaild();
//            throw e;
//        }
//    }
//
//    // public void exexute(Map<String, Object> params) {
//    // String sql = mergeVelocityTemplate(params);
//    // executeSql(this.getName(), sql);
//    // }
//    protected String mergeVelocityTemplate(Map<String, Object> params) {
//        return this.getContent();
//    // StringWriter writer = new StringWriter();
//    // try {
//    // velocityEngine.evaluate(createContext(params), writer, "sql", this.getContent());
//    // return writer.toString();
//    // } catch (Exception e) {
//    // throw new RuntimeException(this.getName(), e);
//    // } finally {
//    // IOUtils.close(writer);
//    // }
//    }
//
//    protected abstract void executeSql(String taskname, String sql);
//
//    // protected VelocityContext createContext(Map<String, Object> params) {
//    //
//    // VelocityContext velocityContext = new VelocityContext();
//    // velocityContext.put("context", this.getContext());
//    //
//    // for (Map.Entry<String, Object> entry : params.entrySet()) {
//    // velocityContext.put(entry.getKey(), entry.getValue());
//    // }
//    //
//    // return velocityContext;
//    // }
//    public abstract String getContent();
//
//    public ITemplateContext getContext() {
//        if (context == null) {
//            throw new NullPointerException("TemplateContext context can not be null");
//        }
//        return context;
//    }
//
//    public void setContext(ITemplateContext context, ITaskContext taskContext) {
//        this.context = context;
//        this.taskContext = taskContext;
//    }
//}
