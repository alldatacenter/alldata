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

package com.qlangtech.tis.plugin.incr;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.IRCController;
import com.qlangtech.tis.coredefine.module.action.Specification;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.coredefine.module.action.impl.RcDeployment;
import com.qlangtech.tis.plugin.BaiscPluginTest;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.PluginStore;

/**
 * 跑这个单元测试需要事先部署k8s集群
 *
 * @author: baisui 百岁
 * @create: 2020-08-11 11:05
 **/
public class TestDefaultIncrK8sConfig extends BaiscPluginTest {

    private static final String s4totalpay = "search4totalpay";
    private IncrStreamFactory incrFactory;

    @Override
    public void setUp() throws Exception {
        //super.setUp();
        IPluginStore<IncrStreamFactory> s4totalpayIncr = TIS.getPluginStore(s4totalpay, IncrStreamFactory.class);
        incrFactory = s4totalpayIncr.getPlugin();
        assertNotNull(incrFactory);
    }

    public void testCreateIncrDeployment() throws Exception {

        IRCController incr = incrFactory.getIncrSync();
        assertNotNull(incr);
        assertFalse(s4totalpay + " shall have not deploy incr instance in k8s"
                , incr.getRCDeployment(new TargetResName(s4totalpay)) != null);

        RcDeployment incrSpec = new RcDeployment();
        incrSpec.setCpuLimit(Specification.parse("1"));
        incrSpec.setCpuRequest(Specification.parse("500m"));
        incrSpec.setMemoryLimit(Specification.parse("1G"));
        incrSpec.setMemoryRequest(Specification.parse("500M"));
        incrSpec.setReplicaCount(1);

        long timestamp = 20190820171040l;

        try {
            incr.deploy(new TargetResName(s4totalpay), incrSpec, timestamp);
        } catch (Exception e) {
            throw e;
        }

    }


    public void testDeleteIncrDeployment() throws Exception {
        try {
            incrFactory.getIncrSync().removeInstance(new TargetResName(s4totalpay));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
