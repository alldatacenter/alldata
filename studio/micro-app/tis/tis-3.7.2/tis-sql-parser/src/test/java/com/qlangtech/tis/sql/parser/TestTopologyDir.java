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
package com.qlangtech.tis.sql.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.manage.common.*;
import com.qlangtech.tis.order.dump.task.ITestDumpCommon;
import com.qlangtech.tis.test.TISEasyMock;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-15 13:43
 */
public class TestTopologyDir extends TestCase implements ITestDumpCommon, TISEasyMock {
    private static File testDataDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.clearProperty(Config.KEY_DATA_DIR);
        Config.setTestDataDir();
        CenterResource.setFetchFromCenterRepository(false);
    }

    public void testSynchronizeSubRemoteRes() throws Exception {

        final long lastUpdate = System.currentTimeMillis();
        String profileFileName = "profile.json";
        List<String> remoteFileNames
                = Lists.newArrayList("dependency_tabs.yaml", "employees_content.json", "er_rules.yaml", profileFileName);
        stubRemoteSubFilesMeta(0, remoteFileNames);
        // 模拟服务端删除了一个文件
        List<String> remoteFileNamesRemoveProfile
                = Lists.newArrayList("dependency_tabs.yaml", "employees_content.json", "er_rules.yaml");
        stubRemoteSubFilesMeta(1, remoteFileNamesRemoveProfile);

        for (String resName : remoteFileNames) {
            stubRemoteResource(resName, lastUpdate);
        }

        TopologyDir topologyDir = new TopologyDir(testDataDir, TOPOLOGY_EMPLOYEES);

        topologyDir.synchronizeSubRemoteRes();
        // 校验本地文件
        File localSubFileDir = topologyDir.getLocalSubFileDir();
        File localSubFile = null;
        File localLastModifyFile = null;
        assertTrue("localSubFileDir must exist,file:"
                + localSubFileDir.getAbsolutePath(), localSubFileDir.exists());

        validateSubDirFiles(lastUpdate, remoteFileNames, localSubFileDir);

        // 再尝试一次同步
        topologyDir.synchronizeSubRemoteRes();

        validateSubDirFiles(lastUpdate, remoteFileNamesRemoveProfile, localSubFileDir);
        // 确认要少了一个profile文件
        File profile = new File(localSubFileDir, profileFileName);
        File profileOfLastModify = new File(localSubFileDir
                , profileFileName + CenterResource.KEY_LAST_MODIFIED_EXTENDION);
        assertFalse("profile shall be remove already:"
                + profile.getAbsolutePath(), profile.exists());
        assertFalse("lastmodify of profile shall be remove already:"
                + profileOfLastModify.getAbsolutePath(), profileOfLastModify.exists());

    }

    private void validateSubDirFiles(long lastUpdate, List<String> remoteFileNames
            , File localSubFileDir) throws IOException {
        File localSubFile;
        File localLastModifyFile;
        for (String localFileName : remoteFileNames) {
            localSubFile = new File(localSubFileDir, localFileName);
            localLastModifyFile = new File(localSubFileDir
                    , localFileName + CenterResource.KEY_LAST_MODIFIED_EXTENDION);
            assertTrue("shall exist:" + localSubFile.getAbsolutePath(), localSubFile.exists());
            assertTrue(localLastModifyFile.getAbsolutePath(), localLastModifyFile.exists());

            try (InputStream input
                         = TestTopologyDir.class.getResourceAsStream(TOPOLOGY_EMPLOYEES + "/" + localFileName)) {
                assertNotNull(input);
                assertEquals("localFileName:" + localFileName, IOUtils.toString(input, TisUTF8.get()), FileUtils.readFileToString(localSubFile, TisUTF8.get()));
            }

            assertEquals(String.valueOf(lastUpdate), FileUtils.readFileToString(localLastModifyFile, TisUTF8.get()));
        }
    }

    private void stubRemoteSubFilesMeta(int tryIndex, List<String> remoteFileNames) {
        HttpUtils.addMockApply(tryIndex, new HttpUtils.MockMatchKey("stream_script_repo.action?path=cfg_repo%2Fdf%2F"
                        + TOPOLOGY_EMPLOYEES, false, true)
                , new HttpUtils.IClasspathRes() {
                    @Override
                    public InputStream getResourceAsStream(URL url) {
                        return new ByteArrayInputStream(new byte[]{});
                    }

                    @Override
                    public Map<String, List<String>> headerFields() {
                        Map<String, List<String>> heads = Maps.newHashMap();
                        String fields = remoteFileNames.stream().map((name) -> name + ":f").collect(Collectors.joining(","));// "dependency_tabs.yaml:f,profile.json:f,employees_content.json:f,er_rules.yaml:f";

                        heads.put(ConfigFileContext.KEY_HEAD_FILES, Collections.singletonList(fields));

                        return heads;
                    }
                });
    }

    private void stubRemoteResource(String resName, long lastUpdate) {
        HttpUtils.addMockApply(-1
                , new HttpUtils.MockMatchKey("cfg_repo%2Fdf%2F" + TOPOLOGY_EMPLOYEES + "%2F" + resName, false, true)
                , new HttpUtils.IClasspathRes() {
                    @Override
                    public InputStream getResourceAsStream(URL url) {
                        InputStream input = TestTopologyDir.class.getResourceAsStream(TOPOLOGY_EMPLOYEES + "/" + resName);
                        Objects.requireNonNull(input, "input can not be null");
                        return input;
                    }

                    @Override
                    public Map<String, List<String>> headerFields() {
                        Map<String, List<String>> head = Maps.newHashMap();
                        head.put(ConfigFileContext.KEY_HEAD_LAST_UPDATE, Collections.singletonList(String.valueOf(lastUpdate)));
                        return head;
                    }
                });
    }
}
