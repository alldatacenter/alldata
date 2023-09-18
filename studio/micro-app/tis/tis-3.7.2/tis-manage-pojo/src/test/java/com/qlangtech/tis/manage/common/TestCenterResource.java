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
package com.qlangtech.tis.manage.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-02-05 09:09
 */
public class TestCenterResource extends TestCase  {

    static final String cfgFile = "com.qlangtech.tis.plugin.incr.IncrStreamFactory.xml";
    static final String KEY_TIS_PLUGIN_CONFIG = "tis_plugin_config";


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Config.setTestDataDir();
    }

    /**
     * CenterResource.copyFromRemote2Local
     */
    public void testCopyFromRemote2LocalServer() throws Exception {

        // 模拟服务端没有数据
        HttpUtils.addMockApply(0, cfgFile, new HttpUtils.IClasspathRes() {
            @Override
            public InputStream getResourceAsStream(URL url) {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public Map<String, List<String>> headerFields() {
                List<String> vals = Collections.singletonList("true");
                return ImmutableMap.of(ConfigFileContext.KEY_HEAD_FILE_NOT_EXIST, vals);
            }
        });

        // 模拟服务端有文件
        final List<String> vals = EasyMock.createMock("lastupdate_vals", List.class);
        long lastupdate = System.currentTimeMillis();
        EasyMock.expect(vals.stream()).andReturn(Lists.newArrayList(String.valueOf(lastupdate)).stream()).times(1);
        HttpUtils.addMockApply(1, cfgFile, new HttpUtils.IClasspathRes() {
            @Override
            public InputStream getResourceAsStream(URL url) {
                return TestCenterResource.class.getResourceAsStream(cfgFile);
            }

            @Override
            public Map<String, List<String>> headerFields() {
                return ImmutableMap.of(ConfigFileContext.KEY_HEAD_LAST_UPDATE, vals);
            }
        });

        EasyMock.replay(vals);

        File local = CenterResource.copyFromRemote2Local(
                KEY_TIS_PLUGIN_CONFIG + "/" + cfgFile, true);
        assertFalse("local file is not exist", local.exists());

        local = CenterResource.copyFromRemote2Local(
                KEY_TIS_PLUGIN_CONFIG + "/" + cfgFile, true);
        assertTrue("local file is exist", local.exists());
        try (InputStream input = TestCenterResource.class.getResourceAsStream(cfgFile)) {
            assertNotNull(input);
            assertEquals(IOUtils.toString(input, TisUTF8.get()), FileUtils.readFileToString(local, TisUTF8.get()));
        }

        // timestamp 文件比较
        File lastModifiedFile = new File(Config.getMetaCfgDir(), KEY_TIS_PLUGIN_CONFIG + "/" + cfgFile + CenterResource.KEY_LAST_MODIFIED_EXTENDION);
        assertTrue(lastModifiedFile.getAbsolutePath(), lastModifiedFile.exists());
        assertEquals(String.valueOf(lastupdate), FileUtils.readFileToString(lastModifiedFile, TisUTF8.get()));

        EasyMock.verify(vals);
    }


}
