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

package org.apache.inlong.manager.pojo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

/**
 * User info
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("User info")
public class UserInfo {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Username")
    private String name;

    @ApiModelProperty(value = "User password")
    private String password;

    @ApiModelProperty("Secret key")
    private String secretKey;

    @ApiModelProperty("Public key")
    private String publicKey;

    @ApiModelProperty("Private key")
    private String privateKey;

    @ApiModelProperty("Encryption key version")
    private Integer encryptVersion;

    @ApiModelProperty(value = "Account type: 0 - manager, 1 - operator", required = true)
    private Integer accountType;

    @Min(1)
    @NotNull(message = "validDays cannot be null")
    @ApiModelProperty(value = "Valid days", required = true)
    private Integer validDays;

    @ApiModelProperty(value = "Name of creator")
    private String creator;

    @ApiModelProperty(value = "Name of modifier")
    private String modifier;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date modifyTime;

    @ApiModelProperty(value = "User status, valid or not")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueDate;

    @ApiModelProperty(value = "User roles")
    private Set<String> roles;

    @ApiModelProperty(value = "Version number")
    private Integer version;

    @ApiModelProperty(value = "Extension json info")
    private String extParams;
}
