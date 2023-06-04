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

package org.apache.inlong.sort.protocol.transformation.operator;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.transformation.SingleValueCompareOperator;

import java.io.Serializable;

@JsonTypeName("isNotNull")
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IsNotNullOperator implements SingleValueCompareOperator, Serializable {

    private static final long serialVersionUID = -4726896495529204485L;

    private static final IsNotNullOperator INSTANCE = new IsNotNullOperator();

    public static IsNotNullOperator getInstance() {
        return INSTANCE;
    }

    @Override
    public String format() {
        return "IS NOT NULL";
    }
}
