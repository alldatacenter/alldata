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
import com.qlangtech.tis.plugin.datax.hudi.DataXHudiWriter;
import com.qlangtech.tis.plugin.datax.hudi.keygenerator.impl.NonePartitionKeyGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-13 18:23
 **/
public class TestOffPartition extends BasicPartitionTest {

    @Test
    public void testPropsBuild() throws Exception {

        NonePartitionKeyGenerator nonePartitionKeyGenerator = new NonePartitionKeyGenerator();
        nonePartitionKeyGenerator.recordFields = Lists.newArrayList("user_id","class_id");
        OffPartition offPartition = new OffPartition();
        nonePartitionKeyGenerator.setPartition(offPartition);

        verifyPartitionPropsBuild(nonePartitionKeyGenerator
                , "hoodie.datasource.write.keygenerator.type=NON_PARTITION\n" +
                        "hoodie.datasource.hive_sync.partition_fields=\n" +
                        "hoodie.datasource.hive_sync.partition_extractor_class=org.apache.hudi.hive.NonPartitionedExtractor");

        Assert.assertFalse(offPartition.isSupportPartition());

    }


}
