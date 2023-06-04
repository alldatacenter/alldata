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

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.inlong.sort.protocol.transformation.function.RegexpReplaceFirstFunction;
import org.apache.inlong.sort.protocol.transformation.function.RegexpReplaceFunction;

/**
 * CascadeFunction is the top-level interface abstraction for cascading function
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegexpReplaceFirstFunction.class, name = "regexpReplaceFirst"),
        @JsonSubTypes.Type(value = RegexpReplaceFunction.class, name = "regexpReplace")
})
public interface CascadeFunction extends Function {

    /**
     * apply function Act on a specific cascade function
     * It accepts the result of running a function as an input parameter
     * and returns the result of running the function
     *
     * @param constantParam is a constant param
     * @return A constant param
     */
    ConstantParam apply(ConstantParam constantParam);

}
