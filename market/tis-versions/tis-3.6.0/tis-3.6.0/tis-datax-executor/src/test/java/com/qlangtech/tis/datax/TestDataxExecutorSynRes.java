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

package com.qlangtech.tis.datax;

import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.plugin.PluginStubUtils;
import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 10:23
 **/
public class TestDataxExecutorSynRes extends TestCase implements IExecutorContext {


    @Override
    protected void setUp() throws Exception {
        System.clearProperty(Config.KEY_DATA_DIR);
        Config.setTestDataDir();
        HttpUtils.mockConnMaker = new HttpUtils.DefaultMockConnectionMaker();
        HttpUtils.addMockGlobalParametersConfig();
        PluginStubUtils.stubPluginConfig();
        PluginStubUtils.setTISField();
        TIS.clean();
        CenterResource.setFetchFromCenterRepository(false);
    }

    public void testSynchronizeDataXPluginsFromRemoteRepository() {
        DataxExecutor.synchronizeDataXPluginsFromRemoteRepository(dataXName, jobName);
        DataxProcessor dataxProcessor = IAppSource.load(null, dataXName);
        File dataxCfgDir = dataxProcessor.getDataxCfgDir(null);
        assertTrue(dataxCfgDir.getAbsolutePath(), dataxCfgDir.exists());
        File jobCfgFile = new File(dataxCfgDir, jobName);
        assertTrue("jobCfgFile must exist:" + jobCfgFile.getAbsolutePath(), jobCfgFile.exists());
    }

    /**
     * create DDL 下的文件是否同步过来
     */
    public void testSynchronizeCreateDDLFromRemoteRepository() {
        String dataX = "mysql_clickhouse";
        DataxExecutor.synchronizeDataXPluginsFromRemoteRepository(dataX, jobName);
        DataxProcessor dataxProcessor = IAppSource.load(null, dataX);
        File dataxCreateDDLDir = dataxProcessor.getDataxCreateDDLDir(null);
        List<String> synFiles = Lists.newArrayList(dataxCreateDDLDir.list((dir, name) -> !StringUtils.endsWith(name, CenterResource.KEY_LAST_MODIFIED_EXTENDION)));
        assertEquals(1, synFiles.size());
        String synFilesStr = synFiles.stream().collect(Collectors.joining(","));
        assertTrue(synFilesStr, synFiles.contains("customer_order_relation.sql"));
        //assertTrue(synFilesStr, synFiles.contains("instancedetail.sql"));
    }
}
