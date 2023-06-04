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

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

/**
 * ConstantParam class is used for the definition and encapsulation of constant param.
 * It can represent any constant, but it is simply implemented by toString() of {@link Object}
 * in the format function used for sql.
 * It contains two subclasses, one is {@link TimeUnitConstantParam} for the definition of time unit constant,
 * and the other is {@link StringConstantParam} for the definition of string constant.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConstantParam.class, name = "constant"),
        @JsonSubTypes.Type(value = TimeUnitConstantParam.class, name = "timeUnitConstant"),
        @JsonSubTypes.Type(value = StringConstantParam.class, name = "stringConstant")
})
@NoArgsConstructor
@Data
public class ConstantParam implements FunctionParam, Serializable {

    private static final long serialVersionUID = 7216146498324134122L;

    @JsonProperty("value")
    private Object value;

    /**
     * ConstantParam constructor
     *
     * @param value It is used to store constant value
     */
    @JsonCreator
    public ConstantParam(@JsonProperty("value") Object value) {
        this.value = Preconditions.checkNotNull(value, "value is null");
    }

    @Override
    public String getName() {
        return "constant";
    }

    @Override
    public String format() {
        return value.toString();
    }

}
