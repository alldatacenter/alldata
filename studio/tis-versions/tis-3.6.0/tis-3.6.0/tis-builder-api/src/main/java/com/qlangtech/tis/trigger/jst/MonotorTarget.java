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
package com.qlangtech.tis.trigger.jst;

import com.qlangtech.tis.trigger.socket.LogType;
import java.io.Serializable;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class MonotorTarget implements Serializable {

    // 当DF构建时，没有绑定collection，先用一个假的collection名称代替一下
    public static final String DUMP_COLLECTION = "dummpCollection";

    private static final long serialVersionUID = 1L;

    public final String collection;

    public final LogType logType;

    private Integer taskid;

    private static MonotorTarget create(String collection, LogType logtype) {
        if (logtype == null) {
            throw new IllegalArgumentException("log type can not be null");
        }
        return new MonotorTarget(collection, logtype);
    }

    public Integer getTaskid() {
        return taskid;
    }

    public void setTaskid(Integer taskid) {
        this.taskid = taskid;
    }

    public static RegisterMonotorTarget createRegister(MonotorTarget target) {
        return new RegisterMonotorTarget(true, target.collection, target.logType);
    }

    public static RegisterMonotorTarget createRegister(String collection, LogType logtype) {
        MonotorTarget target = create(collection, logtype);
        return new RegisterMonotorTarget(true, target.collection, target.logType);
    }

    public static PayloadMonitorTarget createPayloadMonitor(String collection, String payload, LogType logtype) {
        return new PayloadMonitorTarget(true, collection, payload, logtype);
    }

    public static RegisterMonotorTarget createUnregister(String collection, LogType logtype) {
        MonotorTarget target = create(collection, logtype);
        return new RegisterMonotorTarget(false, target.collection, target.logType);
    }

    public static RegisterMonotorTarget createUnregister(MonotorTarget target) {
        return new RegisterMonotorTarget(false, target.collection, target.logType);
    }

    @Override
    public int hashCode() {
        return (collection + logType.getValue()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    MonotorTarget(String collection, LogType logType) {
        super();
        this.collection = collection;
        this.logType = logType;
    }

    public String getCollection() {
        return collection;
    }

    @Override
    public String toString() {
        return "monitorTarget[collection:" + collection + ",type:" + logType + "]";
    }

    public LogType getLogType() {
        return logType;
    }

    /**
     * 是否有匹配的日志类型？
     *
     * @param testType
     * @return
     */
    public boolean testLogType(LogType... testType) {
        for (LogType type : testType) {
            if (type == this.logType) {
                return true;
            }
        }
        return false;
    }
}
