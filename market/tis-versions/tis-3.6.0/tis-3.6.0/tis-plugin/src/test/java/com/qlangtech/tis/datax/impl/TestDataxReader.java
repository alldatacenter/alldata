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

package com.qlangtech.tis.datax.impl;

import com.qlangtech.tis.BasicTestCase;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.util.IPluginContext;
import org.easymock.EasyMock;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-12-25 09:30
 **/
public class TestDataxReader extends BasicTestCase {

    String dataXName = "mysql_startrock2";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TIS.dataXReaderPluginStore.clear();
    }

//    public void testUpdateDataxReader() {
//        KeyedPluginStore<DataxReader> readerStore = DataxReader.getPluginStore(null, dataXName);
//
//        DataxReader dataxReader = readerStore.getPlugin();
//        assertNotNull(dataxReader);
//
//
//        SuFormProperties props = EasyMock.createMock("subformProp", SuFormProperties.class);
//
//        EasyMock.expect(props.getSubFormFieldName()).andReturn("selectedTabs");
//
//        DataxReader.SubFieldFormAppKey<DataxReader> subFieldKey
//                = new DataxReader.SubFieldFormAppKey<>(null, false, dataXName, props, DataxReader.class);
//        KeyedPluginStore<DataxReader> subFieldStore = KeyedPluginStore.getPluginStore(subFieldKey);
//
//        List<Descriptor.ParseDescribable<DataxReader>> dlist = Lists.newArrayList();
//        DataxReader subformReader =
//        dlist.add(new Descriptor.ParseDescribable());
//        subFieldStore.setPlugins(null, Optional.empty(), dlist);
//
//    }


    public void testGetDataxReader() {

        KeyedPluginStore<DataxReader> readerStore = DataxReader.getPluginStore(null, dataXName);
        DataxReader dataxReader = readerStore.getPlugin();
        assertNotNull("dataxReader can not be null", dataxReader);

        List<ISelectedTab> selectedTabs = dataxReader.getSelectedTabs();
        assertNotNull(selectedTabs);
        assertTrue(selectedTabs.size() > 0);


        IPluginContext pluginContext = EasyMock.createMock("pluginContext", IPluginContext.class);
        //
        String execId = "7b069200-9845-d60b-cef0-408c6940ffda";
        EasyMock.expect(pluginContext.getExecId()).andReturn(execId).times(2);
        EasyMock.expect(pluginContext.getRequestHeader(DataxReader.HEAD_KEY_REFERER)).andReturn("/x/" + dataXName + "/update").times(2);
        EasyMock.expect(pluginContext.isCollectionAware()).andReturn(true).times(2);

        EasyMock.replay(pluginContext);
        readerStore = DataxReader.getPluginStore(pluginContext, dataXName);
        assertNotNull("readerStore can not be null", readerStore);

        dataxReader = readerStore.getPlugin();
        assertNotNull("dataxReader can not be null", dataxReader);

        selectedTabs = dataxReader.getSelectedTabs();
        assertNotNull(selectedTabs);
        assertTrue(selectedTabs.size() > 0);
        for (ISelectedTab tab : selectedTabs) {
            assertTrue(tab.getCols().size() > 0);
            for (CMeta col : tab.getCols()) {
                assertNotNull("tab:" + tab.getName() + ",col:"
                        + col.getName() + " can not be null", col.getType());
            }
        }
        EasyMock.verify(pluginContext);
    }
}
