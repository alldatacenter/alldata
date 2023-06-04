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

package org.apache.inlong.sort.protocol.transformation.function;

import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.transformation.Function;
import org.apache.inlong.sort.protocol.transformation.FunctionBaseTest;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;

import java.util.Arrays;

/**
 * Test for {@link CascadeFunctionWrapper}
 */
public class CascadeFunctionWrapperTest extends FunctionBaseTest {

    @Override
    public Function getTestObject() {
        return new CascadeFunctionWrapper(
                Arrays.asList(
                        new RegexpReplaceFirstFunction(new FieldInfo("replace_field", new StringFormatInfo()),
                                new StringConstantParam("replace_str"),
                                new StringConstantParam("target_str")),
                        new RegexpReplaceFunction(new FieldInfo("replace_field", new StringFormatInfo()),
                                new StringConstantParam("replace_str"),
                                new StringConstantParam("target_str"))));
    }

    @Override
    public String getExpectFormat() {
        return "REGEXP_REPLACE(REGEXP_REPLACE_FIRST(`replace_field`, 'replace_str', 'target_str'), "
                + "'replace_str', 'target_str')";
    }
}
