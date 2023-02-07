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

package org.apache.inlong.manager.pojo.transform.splitter;

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
 * A class to define operation to split fields according to SplitRule defined.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonTypeDefine(value = TransformConstants.SPLITTER)
public class SplitterDefinition extends TransformDefinition {

    /**
     * Split rules for transform;
     */
    private List<SplitRule> splitRules;

    public SplitterDefinition(List<SplitRule> splitRules) {
        this.transformType = TransformType.SPLITTER;
        this.splitRules = splitRules;
    }

    /**
     * SplitterRule is aim to define a splitter action below:
     * SourceField will be split to targetFields by separator
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitRule {

        /**
         * Field to split
         */
        private StreamField sourceField;

        /**
         * String separator to split sourceField
         */
        private String separator;

        /**
         * Fields generated when sourceField is split.
         * Use sourceName_0, sourceName_1, sourceName_2 if not set
         */
        private List<String> targetFields;
    }
}
