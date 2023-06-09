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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.plugin.common.PluginDesc;
import junit.framework.TestCase;

import java.util.regex.Matcher;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-27 09:37
 **/
public class TestK8SDataXJobWorker extends TestCase {
    static {
        CenterResource.setNotFetchFromCenterRepository();
        HttpUtils.addMockGlobalParametersConfig();
    }

    public void testZKhostPattern() {
        //  K8SDataXJobWorker dataXJobWorker = new K8SDataXJobWorker();

        Matcher matcher = K8SDataXJobWorker.zkhost_pattern.matcher("192.168.28.200:2181/tis/cloude");
        assertTrue(matcher.matches());

        matcher = K8SDataXJobWorker.zkhost_pattern.matcher("192.168.28.200:2181");
        assertTrue(matcher.matches());

        matcher = K8SDataXJobWorker.zkhost_pattern.matcher("192.168.28.200:2181/tis/cloude-1_bb");
        assertTrue(matcher.matches());

        matcher = K8SDataXJobWorker.zkhost_pattern.matcher("192.168.28.200");
        assertFalse(matcher.matches());

        matcher = K8SDataXJobWorker.zk_path_pattern.matcher("/tis/cloude");
        assertTrue(matcher.matches());

        matcher = K8SDataXJobWorker.zk_path_pattern.matcher("/0tis");
        assertTrue(matcher.matches());

        matcher = K8SDataXJobWorker.zk_path_pattern.matcher("0tis");
        assertFalse(matcher.matches());

        matcher = K8SDataXJobWorker.zk_path_pattern.matcher("/tis*_");
        assertFalse(matcher.matches());

        matcher = K8SDataXJobWorker.zk_path_pattern.matcher("/tis/");
        assertFalse(matcher.matches());
    }

    public void testDescGenerate() {

        PluginDesc.testDescGenerate(K8SDataXJobWorker.class, "k8s-datax-job-worker-descriptor.json");
    }

//    public void testGetRCDeployment() {
//        PluginStore<DataXJobWorker> jobWorkerStore = TIS.getPluginStore(DataXJobWorker.class);
//        DataXJobWorker dataxJobWorker = jobWorkerStore.getPlugin();
//        assertNotNull(dataxJobWorker);
//
//        RcDeployment rcMeta = dataxJobWorker.getRCDeployment();
//        assertNotNull(rcMeta);
//    }
}
