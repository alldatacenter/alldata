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

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.Function;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * SplitIndexFunction class is the logic encapsulation of String delimiting
 */
@JsonTypeName("splitIndex")
@EqualsAndHashCode(callSuper = false)
@Data
public class SplitIndexFunction implements Function, Serializable {

    private static final long serialVersionUID = 460250096378706646L;

    @JsonProperty("field")
    private FieldInfo field;
    @JsonProperty("separator")
    private StringConstantParam separator;
    @JsonProperty("index")
    private ConstantParam index;

    /**
     * SplitIndexFunction constructor
     *
     * @param field it is character to be splitted
     * @param separator the delimiting expression
     * @param index which value to take after delimitted
     */
    public SplitIndexFunction(@JsonProperty("field") FieldInfo field,
            @JsonProperty("separator") StringConstantParam separator,
            @JsonProperty("index") ConstantParam index) {
        this.field = Preconditions.checkNotNull(field, "field is null");
        this.separator = Preconditions.checkNotNull(separator, "separator is null");
        this.index = Preconditions.checkNotNull(index, "index is null");
    }

    @Override
    public String getName() {
        return "SPLIT_INDEX";
    }

    @Override
    public List<FunctionParam> getParams() {
        return Arrays.asList(field, separator, index);
    }

    @Override
    public String format() {
        return String.format("%s(%s, %s, %s)", getName(), field.format(), separator.format(), index.format());
    }
}
