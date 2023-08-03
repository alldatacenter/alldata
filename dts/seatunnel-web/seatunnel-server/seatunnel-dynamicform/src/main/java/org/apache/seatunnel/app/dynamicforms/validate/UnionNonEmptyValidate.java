/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.dynamicforms.validate;

import org.apache.seatunnel.shade.com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@Data
public class UnionNonEmptyValidate extends AbstractValidate {
    private final boolean required = false;
    private List<String> fields;

    @JsonProperty("type")
    private final RequiredType requiredType = RequiredType.UNION_NON_EMPTY;

    public UnionNonEmptyValidate(@NonNull List<String> fields) {
        checkArgument(fields.size() > 0);
        this.fields = fields;
        this.withMessage(
                "parameters:"
                        + fields
                        + " if any of these parameters is set, other parameters must also be set");
    }
}
