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

import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import com.qlangtech.tis.plugin.ds.oracle.OracleDataSourceFactory;
import com.qlangtech.tis.plugin.ds.oracle.impl.SIDConnEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXOracleReader {
    String dataXName = "dataXName";

    @Test
    public void testGetDftTemplate() {
        String dftTemplate = DataXOracleReader.getDftTemplate();
        Assert.assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    @Test
    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXOracleReader.class);
        Assert.assertTrue(extraProps.isPresent());
    }

    @Test
    public void testDescGenerate() throws Exception {

        PluginDesc.testDescGenerate(DataXOracleReader.class, "oracle-datax-reader-descriptor.json");
    }

    private static OracleDataSourceFactory createOracleDataSourceFactory() {

        OracleDataSourceFactory dsFactory = new OracleDataSourceFactory();
        dsFactory.name = "xe";
        // dsFactory.dbName = "xe";
        dsFactory.userName = "system";
        dsFactory.password = "oracle";
        dsFactory.nodeDesc = "192.168.28.201";
        dsFactory.port = 1521;

        SIDConnEntity connEntity = new SIDConnEntity();
        connEntity.sid = "xe";
        dsFactory.connEntity = connEntity;
        return dsFactory;
    }

    @Test
    public void testTemplateGenerate() throws Exception {

        final OracleDataSourceFactory dsFactory = createOracleDataSourceFactory(); // TestOracleDataSourceFactory.createOracleDataSourceFactory();

        DataXOracleReader reader = new DataXOracleReader() {
            @Override
            public OracleDataSourceFactory getDataSourceFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXOracleReader.class;
            }
        };
        reader.dataXName = dataXName;
        reader.splitPk = true;
        reader.fetchSize = 666;
        reader.template = DataXOracleReader.getDftTemplate();
        reader.session = "[\n" +
                "              \"alter session set NLS_DATE_FORMAT='yyyy-mm-dd hh24:mi:ss'\",\n" +
                "              \"alter session set NLS_TIMESTAMP_FORMAT='yyyy-mm-dd hh24:mi:ss'\",\n" +
                "              \"alter session set NLS_TIMESTAMP_TZ_FORMAT='yyyy-mm-dd hh24:mi:ss'\",\n" +
                "              \"alter session set TIME_ZONE='US/Pacific'\"\n" +
                "            ]";
        reader.dbName = "order1";
        reader.selectedTabs = TestSelectedTabs.createSelectedTabs(1);


        ReaderTemplate.validateDataXReader("oracle-datax-reader-template-assert.json", dataXName, reader);
    }


}
