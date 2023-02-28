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

import com.qlangtech.tis.plugin.datax.hudi.keygenerator.impl.SimpleKeyGenerator;
import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-13 19:22
 **/
public class TestFieldValBasedPartition extends BasicPartitionTest {

    @Test
    public void testPropsBuild() throws Exception {

//        hoodie.datasource.hive_sync.partition_fields = pt
//        hoodie.datasource.write.keygenerator.type = SIMPLE
//        hoodie.datasource.write.partitionpath.field = testFiled
//        hoodie.datasource.hive_sync.partition_extractor_class = org.apache.hudi.hive.MultiPartKeysValueExtractor
        String partitionField = "testFiled";
        SimpleKeyGenerator simpleKeyGenerator = new SimpleKeyGenerator();
        simpleKeyGenerator.recordField = "user_id";
        simpleKeyGenerator.partitionPathField = partitionField;

        FieldValBasedPartition fieldValBasedPartition = new FieldValBasedPartition();

        simpleKeyGenerator.setPartition(fieldValBasedPartition);
        // fieldValBasedPartition.partitionPathField = partitionField;
        String propContentExpect
                =
                "hoodie.datasource.write.keygenerator.type=SIMPLE\n" +
                        "hoodie.datasource.write.partitionpath.field=" + partitionField + "\n" +
                        "hoodie.datasource.hive_sync.partition_fields=pt\n" +
                        "hoodie.datasource.hive_sync.partition_extractor_class=org.apache.hudi.hive.MultiPartKeysValueExtractor";
        verifyPartitionPropsBuild(simpleKeyGenerator, propContentExpect);

        verifySQLDDLOfPartition(fieldValBasedPartition);
    }


}
