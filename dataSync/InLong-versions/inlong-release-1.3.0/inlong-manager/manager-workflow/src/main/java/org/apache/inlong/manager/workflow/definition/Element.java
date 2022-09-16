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

package org.apache.inlong.manager.workflow.definition;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.util.Preconditions;

/**
 * Workflow components
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class Element implements Cloneable {

    private String name;
    private String displayName;

    public Element() {
    }

    public void validate() {
        Preconditions.checkTrue(StringUtils.isNotBlank(this.name), "process name cannot be empty");
        Preconditions.checkTrue(StringUtils.isNotBlank(this.displayName), "process display name cannot be empty");
    }

    @Override
    public Element clone() throws CloneNotSupportedException {
        return (Element) super.clone();
    }
}
