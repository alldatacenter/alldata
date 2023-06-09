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

package com.qlangtech.plugins.incr.flink.sink;

import com.google.common.collect.Lists;
import com.qlangtech.plugins.incr.flink.chunjun.doris.sink.TestFlinkSinkExecutor;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugins.incr.flink.chunjun.sink.SinkTabPropsExtends;
import com.qlangtech.tis.plugins.incr.flink.connector.UpdateMode;
import com.qlangtech.tis.plugins.incr.flink.connector.impl.UpsertType;
import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-28 14:45
 **/
public abstract class TestFlinkSinkExecutorByMySQLFullTypes extends TestFlinkSinkExecutor {

    //    @Rule
//    public TemporaryFolder tmpFolder = new TemporaryFolder();
    SelectedTab tabFullType;

    /**
     * 通过 TestFlinkCDCMySQLSourceFactory.testFullTypesConsume()的表创建而得到
     *
     * @return
     */
    @Override
    protected SelectedTab createSelectedTab() {
        if (tabFullType != null) {
            return this.tabFullType;
        }

        String fullTypeStoreFile = "full_types.xml";
        this.tabFullType = load(fullTypeStoreFile);
        SinkTabPropsExtends sinkExt = new SinkTabPropsExtends();
        sinkExt.tabName = this.tabFullType.getName();
        UpdateMode upsert = createIncrMode();
        // EasyMock.expect(sinkExt.getIncrMode()).andReturn(updateMode);
        sinkExt.incrMode = upsert;
        sinkExt.uniqueKey = getUniqueKey();
        this.tabFullType.setIncrSinkProps(sinkExt);

        List<CMeta> cols = this.tabFullType.getCols();
        Assert.assertTrue(CollectionUtils.isNotEmpty(cols));
        return this.tabFullType;
    }

    @Override
    protected ArrayList<String> getUniqueKey() {
        return Lists.newArrayList(colId);
    }

    //    @Override
//    protected UpdateMode createIncrMode() {
//        UpdateType updateMode = new UpdateType();
//        //  updateMode.updateKey = Lists.newArrayList(colId);
//        return updateMode;
//    }
    @Override
    protected UpdateMode createIncrMode() {
        UpsertType upsert = new UpsertType();
        //  updateMode.updateKey = Lists.newArrayList(colId, updateTime);
        return upsert;
    }


    private <T> T load(String sf) {
        //  String fullTypeStoreFile = "full_types.xml";
        try {
            File dir = folder.newFolder();
            File storeFile = new File(dir, sf);
            FileUtils.writeStringToFile(storeFile
                    , IOUtils.loadResourceFromClasspath(TestFlinkSinkExecutorByMySQLFullTypes.class, sf), TisUTF8.get());
            XmlFile tabStore = new XmlFile(storeFile, "test");
            return (T) tabStore.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


//    @Override
//    protected void assertResultSetFromStore(ResultSet resultSet) throws SQLException {
//        super.assertResultSetFromStore(resultSet);
//    }

    @Override
    protected DTO[] createTestDTO() {
        return new DTO[]{load("full_types_dto.xml")};
    }

//    @Override
//    protected BasicDataSourceFactory getDsFactory() {
//        return null;
//    }
//
//    @Override
//    protected ChunjunSinkFactory getSinkFactory() {
//        return null;
//    }
//
//    @Override
//    protected BasicDataXRdbmsWriter createDataXWriter() {
//        return null;
//    }
}
