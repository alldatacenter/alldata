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

package com.qlangtech.tis.plugin.datax.hudi.partition;

import com.alibaba.datax.plugin.writer.hudi.TypedPropertiesBuilder;
import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.plugin.datax.CreateTableSqlBuilder;
import com.qlangtech.tis.plugin.datax.hudi.DataXHudiWriter;
import com.qlangtech.tis.plugin.datax.hudi.keygenerator.HudiKeyGenerator;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-13 20:31
 **/
public class BasicPartitionTest {

    public void verifySQLDDLOfPartition(HudiTablePartition partition) {
        Assert.assertTrue(partition.isSupportPartition());
        DataSourceMeta sourceMeta = new DataSourceMeta() {
            @Override
            public void refresh() {
            }
        };
        CreateTableSqlBuilder createTableSqlBuilder = new CreateTableSqlBuilder(new IDataxProcessor.TableMap(Lists.newArrayList()), sourceMeta) {
            @Override
            protected ColWrapper createColWrapper(CMeta c) {
                return null;
            }
        };
        partition.addPartitionsOnSQLDDL(Collections.singletonList("pt"), createTableSqlBuilder);
        Assert.assertEquals("\n\t,`pt` VARCHAR(30)\n", createTableSqlBuilder.script.toString());
    }

    public void verifyPartitionPropsBuild(
            HudiKeyGenerator keyGenerator, String propContentExpect) throws IOException {
        TypedPropertiesBuilder propBuilder = new TypedPropertiesBuilder();
        DataXHudiWriter hudiWriter = new DataXHudiWriter();
        hudiWriter.partitionedBy = "pt";

        keyGenerator.setProps(propBuilder, hudiWriter);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            propBuilder.store(out);

            String propContent = new String(out.toByteArray());
            // System.out.println(propContent);

            Assert.assertEquals(
                    propContentExpect,
                    propContent);

        }
    }
}
