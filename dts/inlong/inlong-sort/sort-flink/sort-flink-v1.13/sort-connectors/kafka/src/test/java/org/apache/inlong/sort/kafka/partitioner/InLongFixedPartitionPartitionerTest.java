/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.kafka.partitioner;

import org.apache.inlong.sort.base.format.AbstractDynamicSchemaFormat;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * The unit tests for {@link
 * org.apache.inlong.sort.kafka.partitioner.InLongFixedPartitionPartitioner}.
 */
public class InLongFixedPartitionPartitionerTest {

    private String databasePattern = "${database}";
    private String tablePattern = "${table}";
    private AbstractDynamicSchemaFormat dynamicSchemaFormat = new TestDynamicSchemaFormat();
    class TestDynamicSchemaFormat extends AbstractDynamicSchemaFormat {

        @Override
        public String extract(Object data, String key) {
            return null;
        }

        @Override
        public List<String> extractPrimaryKeyNames(Object data) {
            return null;
        }

        @Override
        public boolean extractDDLFlag(Object data) {
            return false;
        }

        @Override
        public String extractDDL(Object data) {
            return null;
        }

        @Override
        public Object extractOperation(Object data) {
            return null;
        }

        @Override
        public List<RowData> extractRowData(Object data, RowType rowType) {
            return null;
        }

        @Override
        public Object deserialize(byte[] message) throws IOException {
            return null;
        }

        @Override
        public String parse(Object data, String pattern) throws IOException {
            if (databasePattern.equals(pattern))
                return "db1";
            if (tablePattern.equals(pattern))
                return "tb1";
            return null;
        }

        @Override
        public RowType extractSchema(Object data, List pkNames) {
            return null;
        }
    }

    @Test
    public void testFixedPartitionPartitioner() {
        String patternPartitionMapConfig = "^db.*&^tb.*:1,DEFAULT_PARTITION:2";
        String partitionPattern = databasePattern + "_" + tablePattern;
        InLongFixedPartitionPartitioner inLongFixedPartitionPartitioner =
                new InLongFixedPartitionPartitioner(patternPartitionMapConfig, partitionPattern);
        inLongFixedPartitionPartitioner.setDynamicSchemaFormat(dynamicSchemaFormat);
        int partition = inLongFixedPartitionPartitioner.partition(null, null,
                null, null, new int[]{0, 1, 2, 3});
        Assert.assertEquals(1, partition);
    }
}
