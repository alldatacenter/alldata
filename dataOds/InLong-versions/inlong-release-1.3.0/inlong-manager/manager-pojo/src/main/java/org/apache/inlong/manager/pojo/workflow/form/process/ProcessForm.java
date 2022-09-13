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

package org.apache.inlong.manager.pojo.workflow.form.process;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.inlong.manager.pojo.workflow.form.Form;

import java.util.Map;

/**
 * WorkflowProcess main form
 */
public interface ProcessForm extends Form {

    /**
     * Get inlong group id.
     *
     * @return inlong group id.
     */
    @JsonIgnore
    String getInlongGroupId();

    /**
     * Get form title.
     */
    @JsonIgnore
    default String getTitle() {
        return null;
    }

    /**
     * Field data displayed in the process list.
     */
    default Map<String, Object> showInList() {
        return null;
    }

}
