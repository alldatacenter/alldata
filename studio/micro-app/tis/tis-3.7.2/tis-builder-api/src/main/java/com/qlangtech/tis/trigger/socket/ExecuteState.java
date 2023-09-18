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
package com.qlangtech.tis.trigger.socket;

import java.io.Serializable;
import java.util.Objects;

/**
 * 业务端执行状态
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-6-19
 */
public class ExecuteState<T> implements Serializable {

    private static final long serialVersionUID = -2958243389261902244L;

    // static final InetAddress localhost;
    //
    // static {
    // try {
    // localhost = InetAddress.getLocalHost();
    // } catch (UnknownHostException e) {
    // throw new RuntimeException(e);
    // }
    // }
    private final LogType logType;

    private final T msg;

    // 代表这条消息是从哪里传过来的
    // private InetAddress from;
    private Long jobId;

    private Long taskId;

    private String serviceName;

    private String execState;

    // private Long phrase;
    private long time;

    public LogType getLogType() {
        return logType;
    }

    /**
     * 事件发生的地点
     */
    private String component;

    private ExecuteState(LogType logType, T msg) {
        super();
        Objects.requireNonNull(logType, "infotype can not be null");
        Objects.requireNonNull(msg, "param msg can not be null");
        this.logType = logType;
        this.msg = msg;
    }

    public String getExecState() {
        return execState;
    }

    public void setExecState(String execState) {
        this.execState = execState;
    }

    public String getCollectionName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    // public void setFrom(InetAddress from) {
    // this.from = from;
    // }
    public void setTime(long time) {
        this.time = time;
    }

    public static <TT> ExecuteState<TT> create(LogType infoType, TT msg) {
        // try {
        ExecuteState<TT> state = new ExecuteState(infoType, msg);
        // state.setFrom(localhost);
        // 当前时间
        state.time = System.currentTimeMillis();
        return state;
    // } catch (UnknownHostException e) {
    // throw new RuntimeException(e);
    // }
    }

    public long getTime() {
        return time;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    // public InetAddress getFrom() {
    // return from;
    // }
    public T getMsg() {
        return msg;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public static class TimeoutResult {

        boolean timeout;

        public TimeoutResult() {
            this.timeout = true;
        }

        public boolean isTimeout() {
            return timeout;
        }

        public void setTimeout(boolean timeout) {
            this.timeout = timeout;
        }
    }
}
