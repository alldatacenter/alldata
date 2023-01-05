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

import com.qlangtech.tis.coredefine.module.action.IDeploymentDetail;
import com.qlangtech.tis.coredefine.module.action.IRCController;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.manage.common.Config;
import junit.framework.TestCase;

/**
 * @author: baisui 百岁
 * @create: 2020-09-02 15:06
 **/
public class TestK8sIncrSync extends TestCase {


    public void testGetRCDeployment() {
        IRCController incrSync = TestDefaultIncrK8sConfig.getIncrSync();
        IDeploymentDetail rcDeployment = incrSync.getRCDeployment(new TargetResName(TestDefaultIncrK8sConfig.totalpay));
        assertNotNull(rcDeployment);
    }


    public void testDeleteDeployment() throws Exception {
        IRCController incrSync = TestDefaultIncrK8sConfig.getIncrSync();

        incrSync.removeInstance(new TargetResName(Config.S4TOTALPAY));
    }

    public void testLaunchIncrProcess() throws Exception {
        IRCController incrSync = TestDefaultIncrK8sConfig.getIncrSync();

        incrSync.relaunch(new TargetResName(Config.S4TOTALPAY));

    }
}
