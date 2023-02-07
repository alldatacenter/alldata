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
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.transformation.Function;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * The function for add
 */
@JsonTypeName("addFunction")
@Data
public class AddFunction implements Function {

    @Nonnull
    @JsonProperty("leftField")
    private final FunctionParam leftField;
    @Nonnull
    @JsonProperty("rightField")
    private final FunctionParam rightField;

    @JsonCreator
    public AddFunction(@Nonnull @JsonProperty("leftField") FunctionParam leftField,
            @Nonnull @JsonProperty("rightField") FunctionParam rightField) {
        this.leftField = Preconditions.checkNotNull(leftField, "leftField is null");
        this.rightField = Preconditions.checkNotNull(rightField, "rightField is null");
    }

    @Override
    public List<FunctionParam> getParams() {
        return Arrays.asList(leftField, rightField);
    }

    @Override
    public String getName() {
        return "+";
    }

    @Override
    public String format() {
        return String.format("%s %s %s", leftField.format(), getName(), rightField.format());
    }
}
