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

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * Operation log entity, including operation type, request url, etc.
 */
@Data
public class OperationLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer id;
    private String authenticationType;
    private String operationType;
    private String httpMethod;
    private String invokeMethod;
    private String operator;
    private String proxy;
    private String requestUrl;
    private String remoteAddress;
    private Long costTime;
    private Boolean status;
    private Date requestTime;
    private String body;
    private String param;
    private String errMsg;

}
