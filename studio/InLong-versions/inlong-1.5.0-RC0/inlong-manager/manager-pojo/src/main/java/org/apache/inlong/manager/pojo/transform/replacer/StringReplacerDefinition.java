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

package org.apache.inlong.manager.pojo.transform.replacer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.consts.TransformConstants;
import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.transform.TransformDefinition;

import java.util.List;

/**
 * A class to define operation to replace stream fields in stream records by ReplaceRule defined.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@JsonTypeDefine(value = TransformConstants.STRING_REPLACER)
public class StringReplacerDefinition extends TransformDefinition {

    public StringReplacerDefinition(List<ReplaceRule> replaceRules) {
        this.transformType = TransformType.STRING_REPLACER;
        this.replaceRules = replaceRules;
    }

    public enum ReplaceMode {
        RELACE_ALL, RELACE_FIRST
    }

    /**
     * ReplaceRule is aim to define a replace action to string fields;
     * If field value match regex, will be replaced by targetValue in REPLACE_ALL/REPLACE_FIRST mode;
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplaceRule {

        private StreamField sourceField;

        private String regex;

        private String targetValue;

        private ReplaceMode mode;

        private boolean isCaseSensitive;
    }

    private List<ReplaceRule> replaceRules;
}
