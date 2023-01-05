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

import com.alibaba.datax.core.util.container.JarLoader;
import com.qlangtech.tis.TIS;
import org.easymock.EasyMock;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-20 14:12
 */
public class TestDataxExecutor extends BasicDataXExecutorTestCase {
    final String execTimeStamp = "20220316121256";

//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        System.clearProperty(Config.DEFAULT_DATA_DIR);
//        Config.setDataDir(Config.DEFAULT_DATA_DIR);
//        PluginStubUtils.setTISField();
//        AtomicReference<ITISRpcService> ref = new AtomicReference<>();
//        ref.set(StatusRpcClient.AssembleSvcCompsite.MOCK_PRC);
//        statusRpc = new RpcServiceReference(ref);
//    }

    public void testDataxJobMysql2Hdfs() throws Exception {

        IDataxProcessor dataxProcessor = EasyMock.createMock("dataxProcessor", IDataxProcessor.class);
        String dataxNameMysql2hdfs = "mysql2hdfs";
        final String jobName = "datax_cfg.json";

        File path = new File("/opt/data/tis/cfg_repo/tis_plugin_config/ap/" + dataxNameMysql2hdfs + "/dataxCfg");
        EasyMock.expect(dataxProcessor.getDataxCfgDir(null)).andReturn(path);


        Integer jobId = 1;

        final JarLoader uberClassLoader = getJarLoader();
        EasyMock.replay(dataxProcessor);
        executor.startWork(dataxNameMysql2hdfs, jobId, jobName, dataxProcessor, uberClassLoader);

        EasyMock.verify(dataxProcessor);
    }

    private JarLoader getJarLoader() {
        return new TISJarLoader(TIS.get().getPluginManager());
//        {
//            @Override
//            protected Class<?> findClass(String name) throws ClassNotFoundException {
//                return TIS.get().getPluginManager().uberClassLoader.findClass(name);
//            }
//        };
    }

    public void testDataxJobMysql2Hive() throws Exception {
//        executor = new DataxExecutor(statusRpc, DataXJobSubmit.InstanceType.LOCAL, 300) {
//            @Override
//            protected void startEngine(Configuration configuration, Integer jobId, String jobName) {
//                //  make skip the ex
//            }
//        };

        String dataxNameMysql2hive = "mysql2hive";
        final String jobName = "datax_cfg.json";
        Path path = Paths.get("/opt/data/tis/cfg_repo/tis_plugin_config/ap/" + dataxNameMysql2hive + "/dataxCfg");
        IDataxProcessor dataxProcessor = EasyMock.createMock("dataxProcessor", IDataxProcessor.class);
        EasyMock.expect(dataxProcessor.getDataxCfgDir(null)).andReturn(path.toFile());
// tring dataxName, Integer jobId, String jobName, String jobPath
        Integer jobId = 1;

        EasyMock.replay(dataxProcessor);
        executor.startWork(dataxNameMysql2hive, jobId, jobName, dataxProcessor, getJarLoader());

        EasyMock.verify(dataxProcessor);
    }

    public void testDataxJobLaunch() throws Exception {
//        executor = new DataxExecutor(statusRpc, DataXJobSubmit.InstanceType.LOCAL, 300) {
//            @Override
//            protected void startEngine(Configuration configuration, Integer jobId, String jobName) {
//                //  make skip the ex
//            }
//        };
        final String jobName = "customer_order_relation_1.json";
        Path path = Paths.get("/opt/data/tis/cfg_repo/tis_plugin_config/ap/baisuitestTestcase/dataxCfg");

        IDataxProcessor dataxProcessor = EasyMock.createMock("dataxProcessor", IDataxProcessor.class);
        EasyMock.expect(dataxProcessor.getDataxCfgDir(null)).andReturn(path.toFile());
// tring dataxName, Integer jobId, String jobName, String jobPath
        Integer jobId = 1;
        EasyMock.replay(dataxProcessor);
        executor.startWork(dataXName, jobId, jobName, dataxProcessor, getJarLoader());
        EasyMock.verify(dataxProcessor);
    }


}
