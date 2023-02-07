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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.DecimalFormatInfo;
import org.apache.inlong.sort.formats.common.DoubleFormatInfo;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.constant.HudiConstant.CatalogType;

/**
 * Test for {@link HudiExtractNode}
 */
public class HudiExtractNodeTest extends SerializeBaseTest<HudiExtractNode> {

    @Override
    public HudiExtractNode getTestObject() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("dt", new StringFormatInfo()),
                new FieldInfo("id", new IntFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("price", new DecimalFormatInfo()),
                new FieldInfo("sale", new DoubleFormatInfo()));

        return new HudiExtractNode(
                "1",
                "doris_input",
                fields,
                null,
                null,
                "localhost:8030",
                "dbName",
                "tableName",
                CatalogType.HIVE,
                1,
                true,
                "-1",
                new HashMap<>(),
                new ArrayList<>());
    }
}
