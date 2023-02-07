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
import org.apache.inlong.sort.protocol.transformation.operator.AndOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EmptyOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.InOperator;
import org.apache.inlong.sort.protocol.transformation.operator.IsNotNullOperator;
import org.apache.inlong.sort.protocol.transformation.operator.IsNullOperator;
import org.apache.inlong.sort.protocol.transformation.operator.LessThanOperator;
import org.apache.inlong.sort.protocol.transformation.operator.LessThanOrEqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.MoreThanOperator;
import org.apache.inlong.sort.protocol.transformation.operator.MoreThanOrEqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.NotEqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.NotInOperator;
import org.apache.inlong.sort.protocol.transformation.operator.OrOperator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AndOperator.class, name = "and"),
        @JsonSubTypes.Type(value = OrOperator.class, name = "or"),
        @JsonSubTypes.Type(value = EmptyOperator.class, name = "empty"),
        @JsonSubTypes.Type(value = EqualOperator.class, name = "equal"),
        @JsonSubTypes.Type(value = NotEqualOperator.class, name = "notEqual"),
        @JsonSubTypes.Type(value = IsNotNullOperator.class, name = "isNotNull"),
        @JsonSubTypes.Type(value = IsNullOperator.class, name = "isNull"),
        @JsonSubTypes.Type(value = LessThanOperator.class, name = "lessThan"),
        @JsonSubTypes.Type(value = LessThanOrEqualOperator.class, name = "lessThanOrEqual"),
        @JsonSubTypes.Type(value = MoreThanOperator.class, name = "moreThan"),
        @JsonSubTypes.Type(value = MoreThanOrEqualOperator.class, name = "moreThanOrEqual"),
        @JsonSubTypes.Type(value = InOperator.class, name = "in"),
        @JsonSubTypes.Type(value = NotInOperator.class, name = "notIn")
})
public interface Operator extends FunctionParam {

    @Override
    default String getName() {
        return "operator";
    }

    @Override
    String format();

}
