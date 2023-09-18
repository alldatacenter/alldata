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

package com.qlangtech.plugins.incr.flink.cdc.mongdb;

import com.google.common.collect.Lists;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.datax.DataXMongodbReader;
import com.qlangtech.tis.test.TISEasyMock;
import junit.framework.TestCase;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-02 13:15
 **/
public class TestFlinkCDCMongoDBSourceFunction extends TestCase implements TISEasyMock {

    public void testStart() throws Exception {

        FlinkCDCMongoDBSourceFactory mongoDBSourceFactory = this.mock("mongoDBSourceFactory", FlinkCDCMongoDBSourceFactory.class);

        FlinkCDCMongoDBSourceFunction mongoDBSourceFunction = new FlinkCDCMongoDBSourceFunction(mongoDBSourceFactory);

        DataXMongodbReader mongodbReader = new DataXMongodbReader();

        List<ISelectedTab> tabs = Lists.newArrayList();
        IDataxProcessor dataXProcessor = this.mock("dataxProcess", IDataxProcessor.class);

        this.replay();

        TargetResName dataXName = new TargetResName("test");
        mongoDBSourceFunction.start(dataXName,mongodbReader, tabs, dataXProcessor);

        this.verifyAll();

    }
}
