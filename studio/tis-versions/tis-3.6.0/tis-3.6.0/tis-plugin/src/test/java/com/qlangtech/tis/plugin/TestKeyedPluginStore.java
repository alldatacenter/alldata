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

import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.util.IPluginContext;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-18 10:03
 **/
public class TestKeyedPluginStore extends TestCase {

    @Override
    protected void setUp() throws Exception {
        CenterResource.setNotFetchFromCenterRepository();
        HttpUtils.addMockGlobalParametersConfig();
    }

    public void testSetPlugins() throws Exception {
        String appName = "testAppName";
        IPluginContext pluginContext = EasyMock.createMock("pluginContext", IPluginContext.class);
        EasyMock.expect(pluginContext.getRequestHeader(DataxReader.HEAD_KEY_REFERER)).andReturn("");
        //  EasyMock.expect(pluginContext.isCollectionAware()).andReturn(true);

        EasyMock.replay(pluginContext);
        KeyedPluginStore.AppKey appKey = IAppSource.createAppSourceKey(pluginContext, appName);
        KeyedPluginStore pluginStore = new KeyedPluginStore(appKey);

        File storeFile = pluginStore.getTargetFile().getFile();
        FileUtils.deleteQuietly(storeFile.getParentFile());

        List<Descriptor.ParseDescribable<TestPlugin>> testPlugin = TestPluginStore.createTestPlugin();


        SetPluginsResult result = pluginStore.setPlugins(pluginContext, Optional.empty(), testPlugin);
        Assert.assertTrue(result.success);
        Assert.assertTrue(storeFile.getAbsolutePath(), result.cfgChanged);
        Assert.assertTrue(result.lastModifyTimeStamp > 0);

        File lastModifyTimeStampFile = pluginStore.getLastModifyTimeStampFile();

       // File lastModifyToken = KeyedPluginStore.getLastModifyToken(appKey);

        Assert.assertEquals(result.lastModifyTimeStamp
                , Long.parseLong(FileUtils.readFileToString(lastModifyTimeStampFile, TisUTF8.get())));

        EasyMock.verify(pluginContext);
    }

}
