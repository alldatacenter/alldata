/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.datax;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.util.container.CoreConstant;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.ds.DBIdentity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-03 16:34
 **/
public class TestDataXExecutorWithMysql2Elastic extends BasicDataXExecutorTestCase {

    private boolean hasExecuteStartEngine;
    final Integer jobId = 123;
    final DataXJobInfo jobName = DataXJobInfo.create("instancedetail_0.json", DBIdentity.parseId(""), Collections.emptyList());
    final String dataxName = "mysql_elastic";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.hasExecuteStartEngine = false;
    }

    /**
     * 测试配置文件和plugin是否正确下载
     */
    public void testResourceSync() throws Exception {
        IDataxProcessor dataxProcessor = DataxProcessor.load(null, StoreResourceType.DataApp, dataxName);

        DataxExecutor.DataXJobArgs jobArgs
                = DataxExecutor.DataXJobArgs.createJobArgs(dataxProcessor, jobId, jobName, 0, TimeFormat.getCurrentTimeStamp());
        this.executor.exec(jobName, dataxProcessor, jobArgs);
        assertTrue("hasExecuteStartEngine", hasExecuteStartEngine);
    }

    @Override
    protected void setDataDir() {
        Config.setTestDataDir();

        // Config.setDataDir("/tmp/tis");
    }

    @Override
    protected boolean isNotFetchFromCenterRepository() {
        return false;
    }

    protected DataxExecutor createExecutor() {
        return new DataxExecutor(statusRpc, DataXJobSubmit.InstanceType.LOCAL, 300) {

            @Override
            protected void startEngine(Pair<Configuration, IDataXNameAware> cfg, DataXJobArgs args, DataXJobInfo jobName) {

                //  make skip the ex
                int jobSleepIntervalInMillSec = cfg.getLeft().getInt(
                        CoreConstant.DATAX_CORE_CONTAINER_JOB_SLEEPINTERVAL, 10000);
                assertEquals(3000, jobSleepIntervalInMillSec);
                hasExecuteStartEngine = true;
            }
        };
    }

}
