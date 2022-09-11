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

package org.apache.inlong.manager.dao.entity;

import lombok.Data;

import java.util.Date;

/**
 * Workflow process entity
 */
@Data
public class WorkflowProcessEntity {

    private Integer id;

    /**
     * Process name
     */
    private String name;

    /**
     * Process display name
     */
    private String displayName;

    /**
     * Process classification
     */
    private String type;

    /**
     * Process title
     */
    private String title;

    /**
     * InLong group ID
     */
    private String inlongGroupId;

    /**
     * Applicant name
     */
    private String applicant;

    /**
     * Process status
     */
    private String status;

    /**
     * Form information
     */
    private String formData;

    /**
     * Application time
     */
    private Date startTime;

    /**
     * End time
     */
    private Date endTime;

    /**
     * Extended params
     */
    private String extParams;

    /**
     * Whether to hide
     */
    private Integer hidden;

}
