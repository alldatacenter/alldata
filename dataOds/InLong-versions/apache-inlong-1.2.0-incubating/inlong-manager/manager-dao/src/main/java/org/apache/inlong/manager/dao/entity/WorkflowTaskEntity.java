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
 * Workflow task entity
 */
@Data
public class WorkflowTaskEntity {

    public static final String APPROVERS_DELIMITER = ",";

    public static final String EXT_TRANSFER_USER_KEY = "transferToUsers";

    /**
     * Task ID
     */
    private Integer id;

    /**
     * Task type
     */
    private String type;

    /**
     * Task name
     */
    private String name;

    /**
     * Display name of the task
     */
    private String displayName;

    /**
     * Application form ID
     */
    private Integer processId;

    /**
     * Process name
     */
    private String processName;

    /**
     * Process display name
     */
    private String processDisplayName;

    /**
     * Applicant
     */
    private String applicant;

    /**
     * Approver
     */
    private String approvers;

    /**
     * Task status
     */
    private String status;

    /**
     * Task operator
     */
    private String operator;

    /**
     * Remarks information
     */
    private String remark;

    /**
     * Form information
     */
    private String formData;

    /**
     * Start time
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

}
