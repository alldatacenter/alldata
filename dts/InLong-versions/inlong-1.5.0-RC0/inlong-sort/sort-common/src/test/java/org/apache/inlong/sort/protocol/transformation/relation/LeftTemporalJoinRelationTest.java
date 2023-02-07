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

package org.apache.inlong.sort.protocol.transformation.relation;

import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.function.SingleValueFilterFunction;
import org.apache.inlong.sort.protocol.transformation.operator.AndOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EmptyOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.NotEqualOperator;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Tests for {@link LeftOuterTemporalJoinRelation}
 */
public class LeftTemporalJoinRelationTest extends SerializeBaseTest<LeftOuterTemporalJoinRelation> {

    @Override
    public LeftOuterTemporalJoinRelation getTestObject() {
        LinkedHashMap<String, List<FilterFunction>> joinConditionMap = new LinkedHashMap<>();
        joinConditionMap.put("2", Arrays.asList(
                new SingleValueFilterFunction(EmptyOperator.getInstance(),
                        new FieldInfo("name", "1", new StringFormatInfo()),
                        EqualOperator.getInstance(), new FieldInfo("name", "2",
                                new StringFormatInfo())),
                new SingleValueFilterFunction(AndOperator.getInstance(),
                        new FieldInfo("name", "1", new StringFormatInfo()),
                        NotEqualOperator.getInstance(), new ConstantParam("test"))));
        return new LeftOuterTemporalJoinRelation(Arrays.asList("1", "2", "3"),
                Collections.singletonList("4"), joinConditionMap, new FieldInfo("ts", new TimestampFormatInfo()));
    }
}
