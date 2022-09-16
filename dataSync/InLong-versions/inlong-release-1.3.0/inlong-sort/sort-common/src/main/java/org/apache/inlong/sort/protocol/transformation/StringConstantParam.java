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

package org.apache.inlong.sort.protocol.transformation;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * StringConstantParam class is used for the definition and encapsulation of time unit constant param.
 * The difference between {@link StringConstantParam} and ConstantParam is that it only accepts string constants,
 * and it will automatically add single quotes when formatting to make it conform
 * to the standard definition of strings in sql.
 */
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonTypeName("stringConstant")
@Data
public class StringConstantParam extends ConstantParam {

    private static final long serialVersionUID = 1779799826901827567L;

    /**
     * StringConstantParam constructor
     *
     * @param value It is used to store string constant value
     */
    public StringConstantParam(@JsonProperty("value") String value) {
        super(value);
    }

    @Override
    public String format() {
        String value = getValue().toString();
        if (!value.startsWith("'") && !value.startsWith("\"")) {
            return String.format("'%s'", value);
        }
        return value;
    }

}
