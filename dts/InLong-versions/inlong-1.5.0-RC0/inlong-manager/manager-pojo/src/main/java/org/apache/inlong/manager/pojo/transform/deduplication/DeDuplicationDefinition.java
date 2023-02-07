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

package org.apache.inlong.manager.pojo.transform.deduplication;

import com.fasterxml.jackson.annotation.JsonFormat;
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
import java.util.concurrent.TimeUnit;

/**
 * A class to define operation to duplicate message in a time duration.
 * DupFields is needed to judge whether stream records is duplicate.
 * TimingField is eventTime of stream records.
 * Interval and timeunit is required to modify a time interval,
 * during which duplicate records is operated;
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@JsonTypeDefine(value = TransformConstants.DE_DUPLICATION)
public class DeDuplicationDefinition extends TransformDefinition {

    public DeDuplicationDefinition(List<StreamField> dupFields,
            StreamField timingField,
            long interval,
            TimeUnit timeUnit,
            DeDuplicationStrategy deDuplicationStrategy) {
        this.transformType = TransformType.DE_DUPLICATION;
        this.dupFields = dupFields;
        this.timingField = timingField;
        this.interval = interval;
        this.timeUnit = timeUnit;
        this.deDuplicationStrategy = deDuplicationStrategy;
    }

    /**
     * Duplicate fields for de_duplication transform
     */
    private List<StreamField> dupFields;

    /**
     * Event time field for de_duplication transform
     */
    private StreamField timingField;

    /**
     * Time interval for de_duplication transform
     */
    private long interval;

    /**
     * TimeUnit for de_duplication transform
     */
    private TimeUnit timeUnit;

    @JsonFormat
    public enum DeDuplicationStrategy {
        REMOVE_ALL, RESERVE_FIRST, RESERVE_LAST
    }

    /**
     * Strategy for de_duplication operation
     */
    private DeDuplicationStrategy deDuplicationStrategy;
}
