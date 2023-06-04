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

package org.apache.inlong.sort.protocol;

import lombok.Getter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.inlong.common.enums.MetaField;

/**
 * Meta field info.
 */
@Getter
public class MetaFieldInfo extends FieldInfo {

    private static final long serialVersionUID = -3436204467879205139L;

    @JsonProperty("metaField")
    private final MetaField metaField;

    @JsonCreator
    public MetaFieldInfo(
            @JsonProperty("name") String name,
            @JsonProperty("nodeId") String nodeId,
            @JsonProperty("metaField") MetaField metaField) {
        super(name, nodeId, null);
        this.metaField = metaField;
    }

    public MetaFieldInfo(
            @JsonProperty("name") String name,
            @JsonProperty("metaField") MetaField metaField) {
        super(name);
        this.metaField = metaField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MetaFieldInfo that = (MetaFieldInfo) o;
        return metaField == that.metaField
                && super.equals(that);
    }
}
