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

package com.qlangtech.tis.exec.datax;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.IDataxGlobalCfg;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.ds.DBIdentity;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.TableInDB;
import com.qlangtech.tis.test.TISTestCase;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-21 13:06
 **/
public abstract class BasicDataXExecuteInterceptor extends TISTestCase {

    protected static final String AP_NAME = "testDataxProcessor";
    protected static File dataxCfgDir;
    protected static final String dbName = "order2";
    protected static final String tableName = "customer_order_relation";
    protected static final String dataCfgTaskName = tableName + "_1";
    protected static final String dataCfgFileName
            = dataCfgTaskName + IDataxProcessor.DATAX_CREATE_DATAX_CFG_FILE_NAME_SUFFIX;
    static final int testTaskId = 999;


    protected DataxReader mockDataXReader() {
        //testDataxProcessorDataXReader
        DataxReader dataxReader = mock(AP_NAME + "DataXReader", DataxReader.class);
        TableInDB tabInDB = TableInDB.create(DBIdentity.parseId(dbName));
        EasyMock.expect(dataxReader.getTablesInDB()).andReturn(tabInDB);
        ISelectedTab tab = new TestSelectedTab(tableName);
        EasyMock.expect(dataxReader.getSelectedTabs()).andReturn(Collections.singletonList(tab)).anyTimes();

        DataxReader.dataxReaderGetter = (name) -> {
            return dataxReader;
        };
        return dataxReader;
    }

    protected IRemoteTaskTrigger mockDataXExecTaskTrigger() {
        IRemoteTaskTrigger jobTrigger = mock("remoteJobTrigger", IRemoteTaskTrigger.class);
        EasyMock.expect(jobTrigger.getTaskName()).andReturn(dataCfgFileName).anyTimes();

        //
        EasyMock.expect(jobTrigger.isAsyn()).andReturn(false).anyTimes();
        jobTrigger.run();
        EasyMock.expectLastCall().andThrow(new RuntimeException("throw a exception")).anyTimes();
        return jobTrigger;
    }


    protected IExecChainContext mockExecChainContext(IDataxProcessor dataxProcessor) {
        IExecChainContext execChainContext = mock("execChainContext", IExecChainContext.class);
        execChainContext.rebindLoggingMDCParams();
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(execChainContext.getPartitionTimestampWithMillis()).andReturn(TimeFormat.getCurrentTimeStamp()).anyTimes();
        EasyMock.expect(execChainContext.hasIndexName()).andReturn(true).anyTimes();
        EasyMock.expect(execChainContext.getIndexName()).andReturn(AP_NAME).anyTimes();
        EasyMock.expect(execChainContext.loadPhaseStatusFromLatest()).andReturn(null);
        EasyMock.expect(execChainContext.getTaskId()).andReturn(testTaskId).anyTimes();
        //  getTaskId

        EasyMock.expect(execChainContext.isDryRun()).andReturn(false).anyTimes();

        EasyMock.expect(execChainContext.getProcessor()).andReturn(dataxProcessor).anyTimes();

        EasyMock.expect(execChainContext.getAttribute(EasyMock.eq(DataXJobSubmit.KEY_DATAX_READERS), EasyMock.anyObject()))
                .andReturn(dataxProcessor.getReaders(null)).anyTimes();

        return execChainContext;
    }

    protected DataXCfgGenerator.GenerateCfgs mockGenerateCfgs(File dataxCfgDir) throws IOException {
        DataXCfgGenerator.GenerateCfgs genCfg = new DataXCfgGenerator.GenerateCfgs(dataxCfgDir);
        genCfg.setGenTime(System.currentTimeMillis());
        Map<String, List<DataXCfgGenerator.DBDataXChildTask>> groupedChildTask = Maps.newHashMap();
        groupedChildTask.put(tableName, Lists.newArrayList(
                new DataXCfgGenerator.DBDataXChildTask(DataXJobSubmit.TableDataXEntity.TEST_JDBC_URL, dbName, dataCfgTaskName)));
        genCfg.setGroupedChildTask(groupedChildTask);
        genCfg.write2GenFile(dataxCfgDir);

        try (InputStream res = TestDataXExecuteInterceptor.class.getResourceAsStream(dataCfgFileName)) {
            Objects.requireNonNull(res, dataCfgFileName + " can not be null");
            FileUtils.copyInputStreamToFile(res, new File(dataxCfgDir, dbName + "/" + dataCfgFileName));
        }
        return genCfg;
    }

    protected static class MockDataxProcessor extends DataxProcessor {
        @Override
        public Application buildApp() {
            return null;
        }

        @Override
        public String identityValue() {
            return TestDataXExecuteInterceptor.AP_NAME;
        }

        @Override
        public StoreResourceType getResType() {
           // throw new UnsupportedOperationException();
            return StoreResourceType.DataApp;
        }

        @Override
        public IDataxGlobalCfg getDataXGlobalCfg() {
            return null;
        }
    }
}
