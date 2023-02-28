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

package com.qlangtech.tis.plugin.datax;

import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.cassandra.CassandraDatasourceFactory;
import com.qlangtech.tis.plugin.ds.cassandra.TestCassandraDatasourceFactory;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import junit.framework.TestCase;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXCassandraWriter extends TestCase {
    public void testGetDftTemplate() {
        String dftTemplate = DataXCassandraWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXCassandraWriter.class);
        assertTrue(extraProps.isPresent());
    }


    public void testDescriptorsJSONGenerate() {
        DataXCassandraWriter writer = new DataXCassandraWriter();
        DescriptorsJSON descJson = new DescriptorsJSON(writer.getDescriptor());

        JsonUtil.assertJSONEqual(DataXCassandraWriter.class, "cassandra-datax-writer-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });
    }

    public void testTemplateGenerate() throws Exception {
        CassandraDatasourceFactory dsFactory = TestCassandraDatasourceFactory.getDS();
        DataXCassandraWriter writer = new DataXCassandraWriter() {
            public CassandraDatasourceFactory getDataSourceFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXCassandraWriter.class;
            }
        };
        writer.template = DataXCassandraWriter.getDftTemplate();
        writer.batchSize = 22;
        writer.consistancyLevel = "ALL";
        writer.connectionsPerHost = 99;
        writer.maxPendingPerConnection = 33;
        List<CMeta> sourceCols = Lists.newArrayList();
        IDataxProcessor.TableMap tableMap = new IDataxProcessor.TableMap(sourceCols);
        tableMap.setFrom("application");
        tableMap.setTo("application");

        CMeta colMeta = null;
        colMeta = new CMeta();
        colMeta.setName("user_id");
        colMeta.setType(DataXReaderColType.Long.dataType);
        sourceCols.add(colMeta);

        colMeta = new CMeta();
        colMeta.setName("user_name");
        colMeta.setType(DataXReaderColType.STRING.dataType);
        sourceCols.add(colMeta);

        colMeta = new CMeta();
        colMeta.setName("bron_date");
        colMeta.setType(DataXReaderColType.Date.dataType);
        sourceCols.add(colMeta);

        // tableMap.setSourceCols(sourceCols);

        WriterTemplate.valiateCfgGenerate("cassandra-datax-writer-assert.json", writer, tableMap);
    }
}
