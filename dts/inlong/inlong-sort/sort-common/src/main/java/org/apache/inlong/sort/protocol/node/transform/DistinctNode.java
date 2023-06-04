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

package org.apache.inlong.sort.protocol.node.transform;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.OrderDirection;

import java.util.List;

/**
 * TimeWindowDistinctNode class is a distinct node based time window
 * It implements distinct operation by ROW_NUMBER such as:
 * SELECT f1,f2,f3,f4,ts,rownum
 * FROM (
 * SELECT f1, f2, f3,f4,ts,
 * ROW_NUMBER() OVER (PARTITION BY f2 ORDER BY ts desc) AS row_num -- desc use the latest one,
 * FROM distinct_table)
 * WHERE rownum=1
 * ————————————————
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("distinct")
@NoArgsConstructor
@Data
public class DistinctNode extends TransformNode {

    private static final long serialVersionUID = 5007120031895569715L;
    @JsonProperty("distinctFields")
    private List<FieldInfo> distinctFields;
    @JsonProperty("orderField")
    private FieldInfo orderField;
    @JsonProperty("orderDirection")
    private OrderDirection orderDirection = OrderDirection.ASC;

    /**
     * TimeWindowDistinctNode constructor
     *
     * @param id node id
     * @param name node name
     * @param fields The fields used to describe node schema
     * @param fieldRelations field relations used to describe the relation between fields
     * @param filters The filters used for data filter
     * @param distinctFields The distinct fields used for partition
     * @param orderField the order field used for sorting in partition
     * @param orderDirection The orderDirection used for sorting after partition,
     *         support [ASC|DESC] default ASC
     */
    @JsonCreator
    public DistinctNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @JsonProperty("distinctFields") List<FieldInfo> distinctFields,
            @JsonProperty("orderField") FieldInfo orderField,
            @JsonProperty("orderDirection") OrderDirection orderDirection) {
        super(id, name, fields, fieldRelations, filters, filterStrategy);
        this.distinctFields = Preconditions.checkNotNull(distinctFields, "distinctFields is null");
        Preconditions.checkState(!distinctFields.isEmpty(), "distinct fields is empty");
        this.orderField = Preconditions.checkNotNull(orderField, "orderField is null");
        this.orderDirection = orderDirection != null ? orderDirection : OrderDirection.ASC;
    }

    @Override
    public String genTableName() {
        return "distinct_" + super.getId();
    }

}
