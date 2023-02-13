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

package com.qlangtech.tis.plugin;

import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.HttpUtils;
import junit.framework.TestCase;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-20 15:59
 **/
public class TestPluginUtils extends TestCase {

    public void testStubParamsConfig() throws Exception {
        PluginStubUtils.stubPluginConfig();
        URL url = new URL("http://192.168.28.200:8080/tjs/config/stream_script_repo.action?path=cfg_repo%2Ftis_plugin_config%2Fcom.qlangtech.tis.config.ParamsConfig.xml");
        HttpUtils.get(url, new ConfigFileContext.StreamProcess<Void>() {
            @Override
            public Void p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                assertNotNull(stream);
                return null;
            }
        });


    }
}
