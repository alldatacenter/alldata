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

package org.apache.inlong.manager.pojo.sink.doris;

import io.swagger.annotations.ApiModelProperty;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.AESUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

/**
 * Sink info of Doris
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DorisSinkDTO {

    @ApiModelProperty("Doris FE http address")
    private String feNodes;

    @ApiModelProperty("Username for doris accessing")
    private String username;

    @ApiModelProperty("Password for doris accessing")
    private String password;

    @ApiModelProperty("Doris table name, such as: db.tbl")
    private String tableIdentifier;

    @ApiModelProperty("Label prefix for stream loading. Used for guaranteeing Flink EOS semantics, as global unique is "
            + "needed in 2pc.")
    private String labelPrefix;

    @ApiModelProperty("The primary key of sink table")
    private String primaryKey;

    @ApiModelProperty("The multiple enable of sink")
    private Boolean sinkMultipleEnable = false;

    @ApiModelProperty("The multiple format of sink")
    private String sinkMultipleFormat;

    @ApiModelProperty("The multiple database-pattern of sink")
    private String databasePattern;

    @ApiModelProperty("The multiple table-pattern of sink")
    private String tablePattern;

    @ApiModelProperty("Password encrypt version")
    private Integer encryptVersion;

    @ApiModelProperty("Properties for doris")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static DorisSinkDTO getFromRequest(DorisSinkRequest request) throws Exception {
        Integer encryptVersion = AESUtils.getCurrentVersion(null);
        String passwd = null;
        if (StringUtils.isNotEmpty(request.getPassword())) {
            passwd = AESUtils.encryptToString(request.getPassword().getBytes(StandardCharsets.UTF_8),
                    encryptVersion);
        }
        return DorisSinkDTO.builder()
                .feNodes(request.getFeNodes())
                .username(request.getUsername())
                .password(passwd)
                .tableIdentifier(request.getTableIdentifier())
                .sinkMultipleEnable(request.getSinkMultipleEnable())
                .sinkMultipleFormat(request.getSinkMultipleFormat())
                .databasePattern(request.getDatabasePattern())
                .tablePattern(request.getTablePattern())
                .labelPrefix(request.getLabelPrefix())
                .encryptVersion(encryptVersion)
                .properties(request.getProperties())
                .build();
    }

    public static DorisSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, DorisSinkDTO.class).decryptPassword();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    private DorisSinkDTO decryptPassword() throws Exception {
        if (StringUtils.isNotEmpty(this.password)) {
            byte[] passwordBytes = AESUtils.decryptAsString(this.password, this.encryptVersion);
            this.password = new String(passwordBytes, StandardCharsets.UTF_8);
        }
        return this;
    }

}
