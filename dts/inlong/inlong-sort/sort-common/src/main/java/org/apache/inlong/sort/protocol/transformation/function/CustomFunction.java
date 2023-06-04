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

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.transformation.Function;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;

/**
 * CustomFunction class uses the content of field as a function
 */
@Data
@JsonTypeName("customFunction")
@EqualsAndHashCode(callSuper = false)
public class CustomFunction implements Function {

    private final String content;

    @JsonCreator
    public CustomFunction(@JsonProperty("content") String content) {
        this.content = content;
    }

    @Override
    public List<FunctionParam> getParams() {
        return Collections.singletonList(new StringConstantParam(content));
    }

    /**
     * Function param name
     *
     * @return The name of this function param
     */
    @Override
    public String getName() {
        throw new UnsupportedOperationException("Custom function is used to pass the function script that the user "
                + "has organized, using content as the real function content, so there is no specific function name.");
    }

    /**
     * Format used for content
     *
     * @return The format value in content
     */
    @Override
    public String format() {
        return content;
    }
}
