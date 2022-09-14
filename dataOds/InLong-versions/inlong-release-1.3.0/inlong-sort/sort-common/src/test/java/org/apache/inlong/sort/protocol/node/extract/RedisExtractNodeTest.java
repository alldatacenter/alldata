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

package org.apache.inlong.sort.protocol.node.extract;

import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.LookupOptions;
import org.apache.inlong.sort.protocol.enums.RedisCommand;
import org.apache.inlong.sort.protocol.enums.RedisMode;

import java.util.Arrays;
import java.util.List;

/**
 * Test for {@link RedisExtractNode}
 */
public class RedisExtractNodeTest extends SerializeBaseTest<RedisExtractNode> {

    @Override
    public RedisExtractNode getTestObject() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()));
        return new RedisExtractNode("1", "redis_input", fields, null, null,
                null, RedisMode.STANDALONE, RedisCommand.HGET, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null,
                new LookupOptions(1L, 10000L, 1, false));
    }
}
