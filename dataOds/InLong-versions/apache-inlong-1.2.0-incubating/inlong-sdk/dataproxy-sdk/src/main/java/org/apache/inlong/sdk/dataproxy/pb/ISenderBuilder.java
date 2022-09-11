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

import org.apache.inlong.sdk.dataproxy.MessageSender;
import org.apache.inlong.sdk.dataproxy.pb.channel.BufferQueueChannel;
import org.apache.inlong.sdk.dataproxy.pb.config.ProxyClusterConfigLoader;
import org.apache.inlong.sdk.dataproxy.pb.context.Constants;
import org.apache.inlong.sdk.dataproxy.pb.context.SdkSinkContext;
import org.apache.inlong.sdk.dataproxy.pb.context.SinkContext;
import org.apache.inlong.sdk.dataproxy.pb.dispatch.DispatchManager;

/**
 * ISenderBuilder
 */
public interface ISenderBuilder {

    // reload
    String KEY_RELOADINTERVAL = Constants.RELOAD_INTERVAL;
    // channel
    String KEY_MAX_BUFFERQUEUE_SIZE_KB = BufferQueueChannel.KEY_MAX_BUFFERQUEUE_SIZE_KB;
    // config
    String KEY_LOADER_RELOADINTERVAL = ProxyClusterConfigLoader.KEY_RELOAD_INTERVAL;
    String KEY_LOADER_TYPE = ProxyClusterConfigLoader.KEY_LOADER_TYPE;
    // config.file
    String KEY_LOADER_TYPE_FILE_NAME = ProxyClusterConfigLoader.KEY_LOADER_TYPE_FILE_NAME;
    // config.context
    String KEY_LOADER_TYPE_CONTEXT_KEY = ProxyClusterConfigLoader.KEY_LOADER_TYPE_CONTEXT_KEY;
    // config.manager
    String KEY_LOADER_TYPE_MANAGER_STREAMURL = ProxyClusterConfigLoader.KEY_LOADER_TYPE_MANAGER_STREAMURL;
    String KEY_LOADER_TYPE_MANAGER_SDKURL = ProxyClusterConfigLoader.KEY_LOADER_TYPE_MANAGER_SDKURL;
    // config.plugin
    String KEY_LOADER_TYPE_PLUGIN_CLASS = ProxyClusterConfigLoader.KEY_LOADER_TYPE_PLUGIN_CLASS;
    // pack
    String KEY_SDK_PACK_TIMEOUT = SdkSinkContext.KEY_SDK_PACK_TIMEOUT;
    String KEY_COMPRESSED_TYPE = SdkSinkContext.KEY_COMPRESSED_TYPE;
    String KEY_NODE_ID = SinkContext.KEY_NODE_ID;
    String KEY_MAX_THREADS = SinkContext.KEY_MAX_THREADS;
    String KEY_PROCESSINTERVAL = SinkContext.KEY_PROCESSINTERVAL;
    String KEY_AUDITFORMATINTERVAL = SinkContext.KEY_AUDITFORMATINTERVAL;
    // dispatch
    String KEY_DISPATCH_TIMEOUT = DispatchManager.KEY_DISPATCH_TIMEOUT;
    String KEY_DISPATCH_MAX_PACKCOUNT = DispatchManager.KEY_DISPATCH_MAX_PACKCOUNT;
    String KEY_DISPATCH_MAX_PACKSIZE = DispatchManager.KEY_DISPATCH_MAX_PACKSIZE;

    /**
     * build
     * 
     * @return
     */
    public MessageSender build();
}
