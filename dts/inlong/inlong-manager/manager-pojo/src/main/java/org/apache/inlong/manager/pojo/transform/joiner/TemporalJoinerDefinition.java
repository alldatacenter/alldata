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

package org.apache.inlong.manager.pojo.transform.joiner;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.manager.pojo.transform.TransformDefinition;

import java.util.List;

/**
 * A class to define operations to join two streamNode in one with relation defined.
 * Temporal joins allow joining against a versioned table
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class TemporalJoinerDefinition extends TransformDefinition {

    public TemporalJoinerDefinition(TemporalJoinWay temporalJoinWay,
            StreamNode leftNode,
            String leftTimeColumn,
            String rightTimeColumn,
            StreamNode rightNode,
            List<StreamField> leftJoinFields,
            List<StreamField> rightJoinFields,
            JoinerDefinition.JoinMode joinMode) {
        this.transformType = TransformType.TEMPORAL_JOINER;
        this.temporalJoinWay = temporalJoinWay;
        this.leftNode = leftNode;
        this.leftTimeColumn = leftTimeColumn;
        this.rightTimeColumn = rightTimeColumn;
        this.rightNode = rightNode;
        this.leftJoinFields = leftJoinFields;
        this.rightJoinFields = rightJoinFields;
        this.joinMode = joinMode;
    }

    /**
     * Temporal join time attribute
     * (Event Time Temporal Join/Processing Time Temporal Join )
     */
    private TemporalJoinWay temporalJoinWay;

    /**
     * Left node for join
     */
    private StreamNode leftNode;

    /**
     * Left table time column
     */
    private String leftTimeColumn;

    /**
     * Right table time column
     */
    private String rightTimeColumn;

    /**
     * Right node for join
     */
    private StreamNode rightNode;

    /**
     * Join streamFields from left node
     */
    private List<StreamField> leftJoinFields;

    /**
     * Join streamFields from right node
     */
    private List<StreamField> rightJoinFields;

    @JsonFormat
    public enum JoinMode {
        LEFT_JOIN, INNER_JOIN
    }

    /**
     * Join mode for join transform
     */
    private JoinerDefinition.JoinMode joinMode;

    /**
     * The TimeUnit class defines an enumeration of time units
     */
    public enum TemporalJoinWay {
        /**
         * Temporal joins allow joining against a versioned table.
         * This means a table can be enriched with changing metadata and retrieve its value at a certain point in time.
         */
        EVENT,
        /**
         * A processing time temporal table join uses a processing-time attribute to
         * correlate rows to the latest version of a key in an external versioned table.
         */
        PROCESSING
    }

}
