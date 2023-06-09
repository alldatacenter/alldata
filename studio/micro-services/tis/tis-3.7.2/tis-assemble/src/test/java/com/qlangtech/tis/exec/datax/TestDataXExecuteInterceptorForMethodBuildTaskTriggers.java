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
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.indexbuild.RemoteTaskTriggers;
import com.qlangtech.tis.order.center.TestIndexSwapTaskflowLauncherWithDataXTrigger;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.ds.DBIdentity;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.TableInDB;
import com.qlangtech.tis.sql.parser.DAGSessionSpec;
import com.tis.hadoop.rpc.ITISRpcService;
import com.tis.hadoop.rpc.RpcServiceReference;
import com.tis.hadoop.rpc.StatusRpcClient;
import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-21 13:00
 **/
public class TestDataXExecuteInterceptorForMethodBuildTaskTriggers extends BasicDataXExecuteInterceptor {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testBuildTaskTriggers() throws Exception {
        // DataXExecuteInterceptor dataXInterceptor = new DataXExecuteInterceptor();

        IDataxProcessor dataXProcessor = this.mock("dataXProcessor", IDataxProcessor.class);
        EasyMock.expect(dataXProcessor.getResType()).andReturn(StoreResourceType.DataApp).anyTimes();


        BatchPostDataXWriter batchWriter = new BatchPostDataXWriter(null);

        EasyMock.expect(dataXProcessor.getWriter(null, true)).andReturn(batchWriter);

        File dataxCfgDir = folder.newFolder("cfgDir");

        DataXCfgGenerator.GenerateCfgs genCfgs = mockGenerateCfgs(dataxCfgDir);

        EasyMock.expect(dataXProcessor.getDataxCfgFileNames(null)).andReturn(genCfgs);

        IExecChainContext chainContext = this.mockExecChainContext(dataXProcessor);
        //  List<IDataxReader> readers
        DataxReader dataXReader = this.mockDataXReader();
        TableInDB tabInDB = TableInDB.create(DBIdentity.parseId(dbName));
        EasyMock.expect(dataXReader.getTablesInDB()).andReturn(tabInDB);

        EasyMock.expect(chainContext.getAttribute(EasyMock.eq(DataXJobSubmit.KEY_DATAX_READERS), EasyMock.anyObject()))
                .andReturn(Lists.newArrayList(dataXReader));

        IRemoteTaskTrigger dumpTrigger = this.mockDataXExecTaskTrigger();

        DataXJobSubmit submit = new TestIndexSwapTaskflowLauncherWithDataXTrigger.MockDataXJobSubmit(dumpTrigger);
        AtomicReference<ITISRpcService> ref = new AtomicReference<>();
        ref.set(StatusRpcClient.AssembleSvcCompsite.MOCK_PRC);
        RpcServiceReference statusRpc = new RpcServiceReference(ref, () -> {
        });


        ISelectedTab tab = new TestSelectedTab(tableName);

        DAGSessionSpec dagSessionSpec = new DAGSessionSpec();
        replay();

        RemoteTaskTriggers triggers = new RemoteTaskTriggers(null);

        DataXExecuteInterceptor.buildTaskTriggers(triggers, chainContext, dataXProcessor, submit, statusRpc, tab, tab.getName(), dagSessionSpec);


        Assert.assertEquals("->prep_customer_order_relation prep_customer_order_relation->customer_order_relation_1.json customer_order_relation_1.json->post_customer_order_relation->customer_order_relation"
                , StringUtils.trim(dagSessionSpec.buildSpec().toString()));

        verifyAll();
    }

}
