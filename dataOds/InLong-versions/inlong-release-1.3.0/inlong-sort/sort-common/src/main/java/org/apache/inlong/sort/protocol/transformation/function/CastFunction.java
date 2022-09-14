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
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.Function;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;

import java.util.Arrays;
import java.util.List;

@JsonTypeName("cast")
@Data
@NoArgsConstructor
public class CastFunction implements Function {

    @JsonProperty("field")
    private FunctionParam field;

    private String type;

    @JsonCreator
    public CastFunction(@JsonProperty("field") FunctionParam field,
            @JsonProperty("type") String type) {
        this.field = Preconditions.checkNotNull(field, "field is null");
        this.type = Preconditions.checkNotNull(type, "type is null");
    }

    @Override
    public List<FunctionParam> getParams() {
        return Arrays.asList(field, new ConstantParam(type));
    }

    @Override
    public String getName() {
        return "CAST";
    }

    @Override
    public String format() {
        return String.format("%s(%s AS %s)", getName(), field.format(), type);
    }
}
