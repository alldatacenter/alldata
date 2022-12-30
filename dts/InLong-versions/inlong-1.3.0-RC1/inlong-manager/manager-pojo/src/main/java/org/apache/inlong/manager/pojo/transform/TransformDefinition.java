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

package org.apache.inlong.manager.pojo.transform;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.inlong.manager.common.enums.TransformType;

/**
 * A class to define operation to transform.
 */
@Data
public abstract class TransformDefinition {

    protected TransformType transformType;

    @JsonFormat
    public enum OperationType {
        lt, le, eq, ne, ge, gt, is_null, not_null
    }

    @JsonFormat
    public enum ScriptType {
        PYTHON, JAVA
    }

    @JsonFormat
    public enum RuleRelation {
        AND, OR
    }
}
