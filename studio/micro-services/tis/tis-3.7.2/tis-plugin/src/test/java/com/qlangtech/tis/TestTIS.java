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
package com.qlangtech.tis;

import com.google.common.collect.Lists;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.extension.UberClassLoader;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.util.PluginMeta;
import junit.framework.TestCase;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestTIS extends TestCase {

    private static final String collection = "search4totalpay";

    @Override
    public void setUp() throws Exception {
        CenterResource.setNotFetchFromCenterRepository();
        HttpUtils.addMockGlobalParametersConfig();
        Config.setDataDir("./");
        TIS.clean();
    }



    public void testReadPluginInfo() throws Exception {
        final String collectionRelativePath = Config.KEY_TIS_PLUGIN_CONFIG + "/" + collection;
        List<String> subFiles = CenterResource.getSubFiles(collectionRelativePath, false, true);
        List<File> subs = Lists.newArrayList();
        for (String f : subFiles) {
            subs.add(CenterResource.copyFromRemote2Local(CenterResource.getPath(collectionRelativePath, f), true));
        }
        Set<PluginMeta> pluginMetas = TIS.loadIncrComponentUsedPlugin(collection, subs, true);

        assertEquals(2, pluginMetas.size());
        for (PluginMeta pluginName : pluginMetas) {
            System.out.println("used plugin:" + pluginName);
        }
    }
}
