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

package org.apache.inlong.sdk.dataproxy.pb.config;

import java.util.List;
import java.util.Map;

import org.apache.flume.conf.Configurable;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyClusterResult;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyInfo;

/**
 * 
 * ProxyClusterConfigLoader
 */
public interface ProxyClusterConfigLoader extends Configurable {

    String KEY_RELOAD_INTERVAL = "config.reloadInterval";
    String KEY_LOADER_TYPE = "config.loaderType";
    // file
    String KEY_LOADER_TYPE_FILE_NAME = "config.loaderType.fileName";
    // context
    String KEY_LOADER_TYPE_CONTEXT_KEY = "config.loaderType.contextKey";
    // manager
    String KEY_LOADER_TYPE_MANAGER_STREAMURL = "config.loaderType.managerStreamUrl";
    String KEY_LOADER_TYPE_MANAGER_SDKURL = "config.loaderType.managerSdkUrl";
    // plugin
    String KEY_LOADER_TYPE_PLUGIN_CLASS = "config.loaderType.pluginClass";

    ProxyClusterResult loadByStream(String inlongGroupId, String inlongStreamId);

    Map<String, ProxyClusterResult> loadByClusterIds(List<ProxyInfo> proxys);
}
