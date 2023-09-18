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

import com.alibaba.datax.common.util.Configuration;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.plugin.PluginStubUtils;
import com.qlangtech.tis.test.TISTestCase;
import com.tis.hadoop.rpc.ITISRpcService;
import com.tis.hadoop.rpc.RpcServiceReference;
import com.tis.hadoop.rpc.StatusRpcClient;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-03 16:25
 **/
public abstract class BasicDataXExecutorTestCase extends TISTestCase implements IExecutorContext {
    protected DataxExecutor executor;
    protected RpcServiceReference statusRpc;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.clearProperty(Config.DEFAULT_DATA_DIR);
        setDataDir();
        PluginStubUtils.setTISField();
        AtomicReference<ITISRpcService> ref = new AtomicReference<>();
        ref.set(StatusRpcClient.AssembleSvcCompsite.MOCK_PRC);
        statusRpc = new RpcServiceReference(ref, () -> {
        });

        executor = createExecutor();
    }

    protected void setDataDir() {
        Config.setDataDir(Config.DEFAULT_DATA_DIR);
    }

    protected DataxExecutor createExecutor() {
        return new DataxExecutor(statusRpc, DataXJobSubmit.InstanceType.LOCAL, 300) {
            @Override
            protected void startEngine(Pair<Configuration, IDataXNameAware> cfg, DataXJobArgs args, DataXJobInfo jobName) {
                //  make skip the ex
                assertNotNull(cfg.getKey());
            }
        };
    }

}
