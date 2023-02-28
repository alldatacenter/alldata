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
package com.qlangtech.tis.coredefine.module.action;

import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.realtime.yarn.rpc.IndexJobRunningStatus;

import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class IndexIncrStatus extends K8SControllerStatus {

    private Map<String, Object> writerDesc;
    private Map<String, Object> readerDesc;
    /**
     * 增量source的元数据
     *
     * @see MQListenerFactory 的desc Props
     */
    private Map<String, Object> incrSourceDesc;
    /**
     * 增量sink的元数据
     */
    private Map<String, Object> incrSinkDesc;

    public IndexIncrStatus() {
    }

    public Map<String, Object> getIncrSinkDesc() {
        return incrSinkDesc;
    }

    public void setIncrSinkDesc(Map<String, Object> incrSinkDesc) {
        this.incrSinkDesc = incrSinkDesc;
    }

    public Map<String, Object> getIncrSourceDesc() {
        return incrSourceDesc;
    }

    public void setIncrSourceDesc(Map<String, Object> incrSourceDesc) {
        this.incrSourceDesc = incrSourceDesc;
    }

    public Map<String, Object> getWriterDesc() {
        return writerDesc;
    }

    public void setWriterDesc(Map<String, Object> writerDesc) {
        this.writerDesc = writerDesc;
    }

    public Map<String, Object> getReaderDesc() {
        return readerDesc;
    }

    public void setReaderDesc(Map<String, Object> readerDesc) {
        this.readerDesc = readerDesc;
    }

    private long incrScriptTimestamp;
    // k8s的插件是否配置完成
    private boolean k8sPluginInitialized = false;

    // 增量执行任务是否正在执行
    private IndexJobRunningStatus incrProcessStatus;


    public IndexJobRunningStatus getIncrProcess() {
        return this.incrProcessStatus;
    }

    public void setIncrProcess(IndexJobRunningStatus incrProcessStatus) {
        this.incrProcessStatus = incrProcessStatus;
    }


    // 增量脚本是否已经生成？
    private boolean incrScriptCreated;

    private String incrScriptMainFileContent;


    public boolean isK8sPluginInitialized() {
        return k8sPluginInitialized;
    }

    public void setK8sPluginInitialized(boolean k8sPluginInitialized) {
        this.k8sPluginInitialized = k8sPluginInitialized;
    }

    public boolean isIncrScriptCreated() {
        return incrScriptCreated;
    }

    public String getIncrScriptMainFileContent() {
        return incrScriptMainFileContent;
    }

    public void setIncrScriptMainFileContent(String incrScriptMainFileContent) {
        this.incrScriptMainFileContent = incrScriptMainFileContent;
    }

    public void setIncrScriptCreated(boolean incrScriptCreated) {
        this.incrScriptCreated = incrScriptCreated;
    }

    public long getIncrScriptTimestamp() {
        return incrScriptTimestamp;
    }

    public void setIncrScriptTimestamp(long incrScriptTimestamp) {
        this.incrScriptTimestamp = incrScriptTimestamp;
    }
}
