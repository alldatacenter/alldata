/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.pb;

import org.apache.flume.Context;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.INLONG_COMPRESSED_TYPE;
import org.apache.inlong.sdk.dataproxy.MessageSender;
import org.apache.inlong.sdk.dataproxy.pb.config.LoaderType;

/**
 * MessageSenderBuilder
 */
public class MessageSenderBuilder implements ISenderBuilder {

    // context
    private Context context = new Context();
    // name
    private String name;
    // reload
    private long reloadInterval;
    // channel
    private int maxBufferQueueSizeKb;
    // config
    private LoaderType loaderType;
    private long loaderReloadInterval;
    private String loaderFileName;
    private String loaderContextKey;
    private String loaderManagerStreamUrl;
    private String loaderManagerSdkUrl;
    private String loaderPluginClass;
    // pack
    private long sdkPackTimeout;
    private INLONG_COMPRESSED_TYPE compressedType;
    private String nodeId;
    private int maxThreads;
    private long processInterval;
    private long auditFormatInterval;
    // dispatch
    private long dispatchTimeout;
    private int dispatchMaxPackCount;
    private int dispatchMaxPackSize;

    /**
     * get name
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * set name
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * get context
     * 
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * set context
     * 
     * @param context the context to set
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * get reloadInterval
     * 
     * @return the reloadInterval
     */
    public long getReloadInterval() {
        return reloadInterval;
    }

    /**
     * set reloadInterval
     * 
     * @param reloadInterval the reloadInterval to set
     */
    public void setReloadInterval(long reloadInterval) {
        this.reloadInterval = reloadInterval;
        context.put(KEY_RELOADINTERVAL, String.valueOf(reloadInterval));
    }

    /**
     * get maxBufferQueueSizeKb
     * 
     * @return the maxBufferQueueSizeKb
     */
    public int getMaxBufferQueueSizeKb() {
        return maxBufferQueueSizeKb;
    }

    /**
     * set maxBufferQueueSizeKb
     * 
     * @param maxBufferQueueSizeKb the maxBufferQueueSizeKb to set
     */
    public void setMaxBufferQueueSizeKb(int maxBufferQueueSizeKb) {
        this.maxBufferQueueSizeKb = maxBufferQueueSizeKb;
        context.put(KEY_MAX_BUFFERQUEUE_SIZE_KB, String.valueOf(maxBufferQueueSizeKb));
    }

    /**
     * get loaderType
     * 
     * @return the loaderType
     */
    public LoaderType getLoaderType() {
        return loaderType;
    }

    /**
     * set loaderType
     * 
     * @param loaderType the loaderType to set
     */
    public void setLoaderType(LoaderType loaderType) {
        this.loaderType = loaderType;
        context.put(KEY_LOADER_TYPE, loaderType.name());
    }

    /**
     * get loaderReloadInterval
     * 
     * @return the loaderReloadInterval
     */
    public long getLoaderReloadInterval() {
        return loaderReloadInterval;
    }

    /**
     * set loaderReloadInterval
     * 
     * @param loaderReloadInterval the loaderReloadInterval to set
     */
    public void setLoaderReloadInterval(long loaderReloadInterval) {
        this.loaderReloadInterval = loaderReloadInterval;
        context.put(KEY_LOADER_RELOADINTERVAL, String.valueOf(loaderReloadInterval));
    }

    /**
     * get loaderFileName
     * 
     * @return the loaderFileName
     */
    public String getLoaderFileName() {
        return loaderFileName;
    }

    /**
     * set loaderFileName
     * 
     * @param loaderFileName the loaderFileName to set
     */
    public void setLoaderFileName(String loaderFileName) {
        this.loaderFileName = loaderFileName;
        context.put(KEY_LOADER_TYPE_FILE_NAME, loaderFileName);
    }

    /**
     * get loaderContextKey
     * 
     * @return the loaderContextKey
     */
    public String getLoaderContextKey() {
        return loaderContextKey;
    }

    /**
     * set loaderContextKey
     * 
     * @param loaderContextKey the loaderContextKey to set
     */
    public void setLoaderContextKey(String loaderContextKey) {
        this.loaderContextKey = loaderContextKey;
        context.put(KEY_LOADER_TYPE_CONTEXT_KEY, loaderContextKey);
    }

    /**
     * get loaderManagerStreamUrl
     * 
     * @return the loaderManagerStreamUrl
     */
    public String getLoaderManagerStreamUrl() {
        return loaderManagerStreamUrl;
    }

    /**
     * set loaderManagerStreamUrl
     * 
     * @param loaderManagerStreamUrl the loaderManagerStreamUrl to set
     */
    public void setLoaderManagerStreamUrl(String loaderManagerStreamUrl) {
        this.loaderManagerStreamUrl = loaderManagerStreamUrl;
        context.put(KEY_LOADER_TYPE_MANAGER_STREAMURL, loaderManagerStreamUrl);
    }

    /**
     * get loaderManagerSdkUrl
     * 
     * @return the loaderManagerSdkUrl
     */
    public String getLoaderManagerSdkUrl() {
        return loaderManagerSdkUrl;
    }

    /**
     * set loaderManagerSdkUrl
     * 
     * @param loaderManagerSdkUrl the loaderManagerSdkUrl to set
     */
    public void setLoaderManagerSdkUrl(String loaderManagerSdkUrl) {
        this.loaderManagerSdkUrl = loaderManagerSdkUrl;
        context.put(KEY_LOADER_TYPE_MANAGER_SDKURL, loaderManagerSdkUrl);
    }

    /**
     * get loaderPluginClass
     * 
     * @return the loaderPluginClass
     */
    public String getLoaderPluginClass() {
        return loaderPluginClass;
    }

    /**
     * set loaderPluginClass
     * 
     * @param loaderPluginClass the loaderPluginClass to set
     */
    public void setLoaderPluginClass(String loaderPluginClass) {
        this.loaderPluginClass = loaderPluginClass;
        context.put(KEY_LOADER_TYPE_PLUGIN_CLASS, loaderPluginClass);
    }

    /**
     * get sdkPackTimeout
     * 
     * @return the sdkPackTimeout
     */
    public long getSdkPackTimeout() {
        return sdkPackTimeout;
    }

    /**
     * set sdkPackTimeout
     * 
     * @param sdkPackTimeout the sdkPackTimeout to set
     */
    public void setSdkPackTimeout(long sdkPackTimeout) {
        this.sdkPackTimeout = sdkPackTimeout;
        context.put(KEY_SDK_PACK_TIMEOUT, String.valueOf(sdkPackTimeout));
    }

    /**
     * get compressedType
     * 
     * @return the compressedType
     */
    public INLONG_COMPRESSED_TYPE getCompressedType() {
        return compressedType;
    }

    /**
     * set compressedType
     * 
     * @param compressedType the compressedType to set
     */
    public void setCompressedType(INLONG_COMPRESSED_TYPE compressedType) {
        this.compressedType = compressedType;
        context.put(KEY_COMPRESSED_TYPE, String.valueOf(compressedType.getNumber()));
    }

    /**
     * get nodeId
     * 
     * @return the nodeId
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * set nodeId
     * 
     * @param nodeId the nodeId to set
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
        context.put(KEY_NODE_ID, nodeId);
    }

    /**
     * get maxThreads
     * 
     * @return the maxThreads
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * set maxThreads
     * 
     * @param maxThreads the maxThreads to set
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        context.put(KEY_MAX_THREADS, String.valueOf(maxThreads));
    }

    /**
     * get processInterval
     * 
     * @return the processInterval
     */
    public long getProcessInterval() {
        return processInterval;
    }

    /**
     * set processInterval
     * 
     * @param processInterval the processInterval to set
     */
    public void setProcessInterval(long processInterval) {
        this.processInterval = processInterval;
        context.put(KEY_PROCESSINTERVAL, String.valueOf(processInterval));
    }

    /**
     * get auditFormatInterval
     * 
     * @return the auditFormatInterval
     */
    public long getAuditFormatInterval() {
        return auditFormatInterval;
    }

    /**
     * set auditFormatInterval
     * 
     * @param auditFormatInterval the auditFormatInterval to set
     */
    public void setAuditFormatInterval(long auditFormatInterval) {
        this.auditFormatInterval = auditFormatInterval;
        context.put(KEY_AUDITFORMATINTERVAL, String.valueOf(auditFormatInterval));
    }

    /**
     * get dispatchTimeout
     * 
     * @return the dispatchTimeout
     */
    public long getDispatchTimeout() {
        return dispatchTimeout;
    }

    /**
     * set dispatchTimeout
     * 
     * @param dispatchTimeout the dispatchTimeout to set
     */
    public void setDispatchTimeout(long dispatchTimeout) {
        this.dispatchTimeout = dispatchTimeout;
        context.put(KEY_DISPATCH_TIMEOUT, String.valueOf(dispatchTimeout));
    }

    /**
     * get dispatchMaxPackCount
     * 
     * @return the dispatchMaxPackCount
     */
    public int getDispatchMaxPackCount() {
        return dispatchMaxPackCount;
    }

    /**
     * set dispatchMaxPackCount
     * 
     * @param dispatchMaxPackCount the dispatchMaxPackCount to set
     */
    public void setDispatchMaxPackCount(int dispatchMaxPackCount) {
        this.dispatchMaxPackCount = dispatchMaxPackCount;
        context.put(KEY_DISPATCH_MAX_PACKCOUNT, String.valueOf(dispatchMaxPackCount));
    }

    /**
     * get dispatchMaxPackSize
     * 
     * @return the dispatchMaxPackSize
     */
    public int getDispatchMaxPackSize() {
        return dispatchMaxPackSize;
    }

    /**
     * set dispatchMaxPackSize
     * 
     * @param dispatchMaxPackSize the dispatchMaxPackSize to set
     */
    public void setDispatchMaxPackSize(int dispatchMaxPackSize) {
        this.dispatchMaxPackSize = dispatchMaxPackSize;
        context.put(KEY_DISPATCH_MAX_PACKSIZE, String.valueOf(dispatchMaxPackSize));
    }

    /**
     * build
     * 
     * @return
     */
    public MessageSender build() {
        PbProtocolMessageSender sender = new PbProtocolMessageSender(name);
        sender.configure(context);
        sender.start();
        return sender;
    }
}
