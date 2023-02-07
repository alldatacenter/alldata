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

package org.apache.inlong.sort.protocol.node.load;

import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import java.util.Arrays;

/**
 * Test for {@link TDSQLPostgresLoadNode}
 */
public class TDSQLPostgresLoadNodeTest extends SerializeBaseTest<TDSQLPostgresLoadNode> {

    /**
     * Get test object
     *
     * @return The test object
     */
    @Override
    public TDSQLPostgresLoadNode getTestObject() {
        return new TDSQLPostgresLoadNode("1", "tdsqlPostgres_output", Arrays.asList(new FieldInfo("name",
                new StringFormatInfo())),
                Arrays.asList(new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo()))),
                null, null, 1, null,
                "jdbc:postgresql://localhost:5432/postgres",
                "postgres",
                "inlong",
                "user",
                "name");
    }
}
