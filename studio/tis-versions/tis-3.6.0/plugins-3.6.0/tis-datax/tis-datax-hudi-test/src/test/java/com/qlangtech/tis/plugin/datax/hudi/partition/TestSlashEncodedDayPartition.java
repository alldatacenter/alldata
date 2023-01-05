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

import com.qlangtech.plugins.org.apache.hudi.keygen.TimestampBasedAvroKeyGenerator;
import com.qlangtech.tis.plugin.datax.hudi.keygenerator.impl.TimeStampKeyGenerator;
import org.junit.Test;

import java.util.TimeZone;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-13 20:32
 **/
public class TestSlashEncodedDayPartition extends BasicPartitionTest {

    @Test
    public void testPropsBuild() throws Exception {

//        HudiTimestampBasedKeyGenerator keyGenerator = new HudiTimestampBasedKeyGenerator();
//        keyGenerator.timezone =

        String partitionField = "testFiled";
        TimeStampKeyGenerator timeStampKeyGenerator = new TimeStampKeyGenerator();
        timeStampKeyGenerator.recordField = "user_id";
        timeStampKeyGenerator.partitionPathField = partitionField;
        timeStampKeyGenerator.timezone = TimeZone.getDefault().getID();
        timeStampKeyGenerator.timestampType = TimestampBasedAvroKeyGenerator.TimestampType.EPOCHMILLISECONDS.name();
        timeStampKeyGenerator.inputDateformat = "yyyy-MM-dd HH:mm:ss";
        timeStampKeyGenerator.outputDateformat = "yyyyMMdd";

        SlashEncodedDayPartition partition = new SlashEncodedDayPartition();

        timeStampKeyGenerator.setPartition(partition);


        //partition.partitionPathField = partitionField;
        // partition.keyGenerator = keyGenerator;
        String propContentExpect
                =
                "hoodie.datasource.write.keygenerator.type=SIMPLE\n" +
                        "hoodie.datasource.write.partitionpath.field=testFiled\n" +
                        "hoodie.datasource.hive_sync.partition_fields=pt\n" +
                        "hoodie.datasource.hive_sync.partition_extractor_class=org.apache.hudi.hive.SlashEncodedDayPartitionValueExtractor\n"
                        + "hoodie.datasource.write.keygenerator.type=TIMESTAMP\n" +
                        "hoodie.deltastreamer.keygen.timebased.timestamp.type=EPOCHMILLISECONDS\n" +
                        "hoodie.deltastreamer.keygen.timebased.input.dateformat=yyyy-MM-dd HH:mm:ss\n" +
                        "hoodie.deltastreamer.keygen.timebased.output.dateformat=yyyyMMdd\n" +
                        "hoodie.deltastreamer.keygen.timebased.timezone=Asia/Shanghai";
        verifyPartitionPropsBuild(timeStampKeyGenerator, propContentExpect);

        verifySQLDDLOfPartition(partition);
    }
}
