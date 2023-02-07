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

package org.apache.inlong.manager.pojo.transform.filter;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
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
 * A class to define operation to filter stream records by different modes.
 * Rule mode is more recommended than script mode.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonTypeDefine(value = TransformConstants.FILTER)
public class FilterDefinition extends TransformDefinition {

    /**
     * Strategy for Filter transform
     */
    private FilterStrategy filterStrategy;
    /**
     * Mode for Filter transform
     */
    private FilterMode filterMode;
    private List<FilterRule> filterRules;
    private ScriptBase scriptBase;

    public FilterDefinition(FilterStrategy filterStrategy, List<FilterRule> filterRules) {
        this.transformType = TransformType.FILTER;
        this.filterStrategy = filterStrategy;
        this.filterMode = FilterMode.RULE;
        this.filterRules = filterRules;
    }

    public FilterDefinition(FilterStrategy filterStrategy, ScriptBase scriptBase) {
        this.transformType = TransformType.FILTER;
        this.filterStrategy = filterStrategy;
        this.filterMode = FilterMode.SCRIPT;
        this.scriptBase = scriptBase;
    }

    @JsonFormat
    public enum FilterStrategy {
        RETAIN, REMOVE
    }

    @JsonFormat
    public enum FilterMode {
        RULE, SCRIPT
    }

    @Data
    @AllArgsConstructor
    public static class TargetValue {

        /**
         * If target value is constant, set targetConstant, or set targetField if not;
         */
        private boolean constant;

        private StreamField targetField;

        private String targetConstant;
    }

    /**
     * Filter rule is about relation between sourceField and targetValue.
     * Such as 'a >= b' or 'a were not null'
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterRule {

        private StreamField sourceField;

        private OperationType operationType;

        private TargetValue targetValue;

        private RuleRelation relationWithPost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptBase {

        private ScriptType scriptType;

        private String script;
    }
}
