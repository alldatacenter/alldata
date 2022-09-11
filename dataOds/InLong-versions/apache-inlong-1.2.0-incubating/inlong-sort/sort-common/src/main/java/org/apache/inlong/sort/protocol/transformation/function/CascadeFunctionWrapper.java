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
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.transformation.CascadeFunction;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.Function;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CascadeFunctionWrapper class is a wrapper of {@link CascadeFunction}
 * It contains a list of {@link CascadeFunction} that really will be executed
 */
@JsonTypeName("cascadeFunctionWrapper")
@EqualsAndHashCode(callSuper = false)
@Data
public class CascadeFunctionWrapper implements Function, Serializable {

    private static final long serialVersionUID = 8197348412858988257L;

    @JsonProperty("functions")
    private final List<CascadeFunction> functions;

    /**
     * CascadeFunction constructor
     *
     * @param functions List of functions that cascade functions really need to execute
     */
    @JsonCreator
    public CascadeFunctionWrapper(@JsonProperty("functions") List<CascadeFunction> functions) {
        this.functions = Preconditions.checkNotNull(functions, "functions is null");
        Preconditions.checkState(!functions.isEmpty(), "functions is empty");
    }

    @Override
    public List<FunctionParam> getParams() {
        return new ArrayList<>(functions);
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("The method of getName is not support of CascadeFunction");
    }

    @Override
    public String format() {
        ConstantParam s = new ConstantParam(functions.get(0).format());
        for (int i = 1; i < functions.size(); i++) {
            s = functions.get(i).apply(s);
        }
        return s.format();
    }
}
