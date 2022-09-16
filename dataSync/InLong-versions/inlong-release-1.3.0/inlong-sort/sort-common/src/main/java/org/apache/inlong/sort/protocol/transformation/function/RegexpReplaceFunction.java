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
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.transformation.CascadeFunction;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * RegexpReplaceFunction class is the logic encapsulation of String replace by regexp
 */
@JsonTypeName("regexpReplace")
@EqualsAndHashCode(callSuper = false)
@Data
public class RegexpReplaceFunction implements CascadeFunction, Serializable {

    private static final long serialVersionUID = -2701547146694616429L;

    @JsonProperty("field")
    private FieldInfo field;
    @JsonProperty("regex")
    private StringConstantParam regex;
    @JsonProperty("replacement")
    private StringConstantParam replacement;

    /**
     * RegexpReplaceFunction constructor
     *
     * @param field it is character to be replaced
     * @param regex the regex expression of replacing
     * @param replacement the value that to be replaced
     */
    @JsonCreator
    public RegexpReplaceFunction(@JsonProperty("field") FieldInfo field,
            @JsonProperty("regex") StringConstantParam regex,
            @JsonProperty("replacement") StringConstantParam replacement) {
        this.field = Preconditions.checkNotNull(field, "field is null");
        this.regex = Preconditions.checkNotNull(regex, "regex is null");
        this.replacement = Preconditions.checkNotNull(replacement, "replacement is null");
    }

    @Override
    public String getName() {
        return "REGEXP_REPLACE";
    }

    @Override
    public List<FunctionParam> getParams() {
        return Arrays.asList(field, regex, replacement);
    }

    @Override
    public String format() {
        return String.format("%s(%s, %s, %s)", getName(), field.format(), regex.format(), replacement.format());
    }

    @Override
    public ConstantParam apply(ConstantParam constantParam) {
        return new ConstantParam(String.format("%s(%s, %s, %s)", getName(),
                constantParam.format(), regex.format(), replacement.format()));
    }

}
