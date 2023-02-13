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

package org.apache.inlong.manager.pojo.source.hudi;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

/**
 * Hudi source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HudiSourceDTO {

    @ApiModelProperty("The database name of hudi")
    private String dbName;

    @ApiModelProperty("The table name of hudi")
    private String tableName;

    @ApiModelProperty("The catalog uri of hudi")
    private String catalogUri;

    @ApiModelProperty("The dfs base path of hudi")
    private String warehouse;

    @ApiModelProperty("The flag indicate whether skip files in compaction")
    private boolean readStreamingSkipCompaction;

    @ApiModelProperty("The start commit id")
    private String readStartCommit;

    @ApiModelProperty("Extended properties")
    private List<HashMap<String, String>> extList;

    @ApiModelProperty("Properties for hudi")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static HudiSourceDTO getFromRequest(HudiSourceRequest request) {
        HudiSourceDTO hudiSourceDTO = new HudiSourceDTO();
        CommonBeanUtils.copyProperties(request, hudiSourceDTO);
        return hudiSourceDTO;
    }

    /**
     * Get the dto instance from the JSON string
     */
    public static HudiSourceDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, HudiSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("parse extParams of HudiSource failure: %s", e.getMessage()));
        }
    }
}
