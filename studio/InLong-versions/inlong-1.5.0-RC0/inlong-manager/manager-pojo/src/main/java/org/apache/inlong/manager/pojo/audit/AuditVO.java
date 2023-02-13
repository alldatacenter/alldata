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

package org.apache.inlong.manager.pojo.audit;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

/**
 * The VO of audit.
 */
@Data
public class AuditVO {

    @ApiModelProperty(value = "Audit id")
    private String auditId;
    @ApiModelProperty(value = "Audit set")
    private List<AuditInfo> auditSet;
    @ApiModelProperty(value = "Node type")
    private String nodeType;

    public AuditVO() {
    }

    public AuditVO(String auditId, List<AuditInfo> auditSet, String nodeType) {
        this.auditId = auditId;
        this.auditSet = auditSet;
        this.nodeType = nodeType;
    }
}
