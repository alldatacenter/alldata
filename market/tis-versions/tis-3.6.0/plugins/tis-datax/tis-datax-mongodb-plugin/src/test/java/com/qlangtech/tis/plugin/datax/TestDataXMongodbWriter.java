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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.mangodb.MangoDBDataSourceFactory;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import junit.framework.TestCase;
import org.easymock.EasyMock;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXMongodbWriter extends TestCase {
    public void testGetDftTemplate() {
        String dftTemplate = DataXMongodbWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXMongodbWriter.class);
        assertTrue(extraProps.isPresent());
    }

    public void testDescriptorsJSONGenerate() {
        DataxReader dataxReader = EasyMock.createMock("dataxReader", DataxReader.class);

        List<ISelectedTab> selectedTabs = TestSelectedTabs.createSelectedTabs(1).stream().map((t) -> t).collect(Collectors.toList());

        for (ISelectedTab tab : selectedTabs) {
            for (CMeta cm : tab.getCols()) {
                cm.setType(DataXReaderColType.STRING.dataType);
            }
        }
        EasyMock.expect(dataxReader.getSelectedTabs()).andReturn(selectedTabs).times(4);
        DataxReader.dataxReaderThreadLocal.set(dataxReader);
        EasyMock.replay(dataxReader);
        DataXMongodbWriter writer = new DataXMongodbWriter();
        assertTrue(writer instanceof IDataSourceFactoryGetter);
        DescriptorsJSON descJson = new DescriptorsJSON(writer.getDescriptor());

        JsonUtil.assertJSONEqual(DataXMongodbWriter.class, "mongdodb-datax-writer-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });

        JsonUtil.assertJSONEqual(DataXMongodbWriter.class, "mongdodb-datax-writer-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });
        EasyMock.verify(dataxReader);
    }

    public void testTemplateGenerate() throws Exception {


        MangoDBDataSourceFactory dsFactory = TestDataXMongodbReader.getDataSourceFactory();
        DataXMongodbWriter writer = new DataXMongodbWriter() {
            @Override
            public MangoDBDataSourceFactory getDsFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXMongodbWriter.class;
            }
        };
        writer.collectionName = "employee";

        writer.column = IOUtils.loadResourceFromClasspath(this.getClass(), "mongodb-reader-column.json");
        writer.template = DataXMongodbWriter.getDftTemplate();
        writer.dbName = "order1";
        writer.dataXName = "mongodb_doris";
        writer.upsertInfo = "{\"isUpsert\":true,\"upsertKey\":\"user_id\"}";
        // IDataxProcessor.TableMap tableMap = new IDataxProcessor.TableMap();
        WriterTemplate.valiateCfgGenerate(
                "mongodb-datax-writer-assert.json", writer, null);

        dsFactory.username = null;
        dsFactory.password = null;
        writer.upsertInfo = null;

        WriterTemplate.valiateCfgGenerate(
                "mongodb-datax-writer-assert-without-option.json", writer, null);


    }
}
