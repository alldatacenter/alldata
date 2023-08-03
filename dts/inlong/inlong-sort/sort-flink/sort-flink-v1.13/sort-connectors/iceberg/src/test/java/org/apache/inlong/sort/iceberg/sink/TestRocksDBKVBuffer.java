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

package org.apache.inlong.sort.iceberg.sink;

import org.apache.inlong.sort.iceberg.sink.collections.RocksDBKVBuffer;

import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;

public class TestRocksDBKVBuffer {

    private static final Logger LOG = LoggerFactory.getLogger(TestRocksDBKVBuffer.class);

    @Test
    public void testRepeatputAndClear() {
        RocksDBKVBuffer<String, String> buffer = new RocksDBKVBuffer<>(
                StringSerializer.INSTANCE,
                StringSerializer.INSTANCE,
                System.getProperty("java.io.tmpdir") + File.separator + "mini_batch");
        for (int i = 0; i < 10; i++) {
            repeatPutAndClear(buffer);
        }
        buffer.close();
    }

    public void repeatPutAndClear(RocksDBKVBuffer<String, String> buffer) {
        LOG.info("---------------------------------------------------------------");
        long count = 1000L; // could increase it to 1000w and monitor total memory
        int beforeCompleteness = 0;
        for (int i = 1; i <= count; i++) {
            buffer.put("java" + i, UUID.randomUUID().toString());
            int completeness = (int) (i / (count * 1.0 / 100));
            if (completeness > beforeCompleteness) {
                beforeCompleteness = completeness;
                LOG.info(String.format("Current completeness %d%%, write count %d", completeness, i));
            }
        }
        buffer.clear();
        LOG.info("---------------------------------------------------------------");
    }
}
