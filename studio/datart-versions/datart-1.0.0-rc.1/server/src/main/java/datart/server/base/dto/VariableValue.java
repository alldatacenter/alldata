/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.base.dto;

import lombok.Data;

import java.util.Set;

@Data
public class VariableValue {

    private String relId;

    private String variableId;

    private String orgId;

    private String viewId;

    private String sourceId;

    private String format;

    private String name;

    private String type;

    private String valueType;

    private String label;

    private String subjectId;

    private String subjectType;

    private Set<String> values;

    private String defaultValue;

    private boolean expression;

}
