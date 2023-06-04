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

package org.apache.inlong.manager.pojo.sink.starrocks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.sink.SinkField;

import javax.validation.constraints.NotNull;

/**
 * StarRocks column info.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeDefine(value = SinkType.STARROCKS)
public class StarRocksColumnInfo extends SinkField {

    private Boolean isSortKey = false;

    private Boolean isDistributed = false;

    /**
     * Get the dto instance from the request
     */
    public static StarRocksColumnInfo getFromRequest(SinkField sinkField) {
        return CommonBeanUtils.copyProperties(sinkField, StarRocksColumnInfo::new, true);
    }

    /**
     * Get the extra param from the Json
     */
    public static StarRocksColumnInfo getFromJson(@NotNull String extParams) {
        if (StringUtils.isEmpty(extParams)) {
            return new StarRocksColumnInfo();
        }
        try {
            return JsonUtils.parseObject(extParams, StarRocksColumnInfo.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    String.format("Failed to parse extParams for StarRocks ColumnInfo: %s", e.getMessage()));
        }
    }
}
