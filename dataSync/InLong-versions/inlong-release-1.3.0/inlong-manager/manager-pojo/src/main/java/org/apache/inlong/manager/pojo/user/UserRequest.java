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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.pojo.common.PageRequest;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.validation.InEnumInt;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * User info request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel("User info request")
public class UserRequest extends PageRequest {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @NotBlank(message = "User name cannot be blank")
    @ApiModelProperty(value = "User name", required = true)
    private String name;

    @ApiModelProperty(value = "Keyword, can be user name")
    private String keyword;

    @ApiModelProperty(value = "User password")
    private String password;

    @ApiModelProperty(value = "New password, is required if needs updated")
    private String newPassword;

    @ApiModelProperty("Secret key")
    private String secretKey;

    @ApiModelProperty("Public key")
    private String publicKey;

    @ApiModelProperty("Private key")
    private String privateKey;

    @ApiModelProperty("Encryption key version")
    private Integer encryptVersion;

    @NotNull(message = "accountType cannot be null")
    @InEnumInt(UserTypeEnum.class)
    @ApiModelProperty(value = "Account type: 0 - manager, 1 - operator", required = true)
    private Integer accountType;

    @Min(1)
    @NotNull(message = "validDays cannot be null")
    @ApiModelProperty(value = "Valid days", required = true)
    private Integer validDays;

    @ApiModelProperty(value = "Version number")
    private Integer version;

    @ApiModelProperty(value = "Extension json info")
    private String extParams;

}
