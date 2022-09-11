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

package org.apache.inlong.manager.common.pojo.source.file;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import javax.validation.constraints.NotNull;

/**
 * File source information data transfer object
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileSourceDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Agent IP address")
    private String ip;

    @ApiModelProperty("Path regex pattern for file, such as /a/b/*.txt")
    private String pattern;

    @ApiModelProperty("TimeOffset for collection, "
            + "'1m' means from one minute after, '-1m' means from one minute before, "
            + "'1h' means from one hour after, '-1h' means from one minute before, "
            + "'1d' means from one day after, '-1d' means from one minute before, "
            + "Null or blank means from current timestamp")
    private String timeOffset;

    public static FileSourceDTO getFromRequest(@NotNull FileSourceRequest fileSourceRequest) {
        return FileSourceDTO.builder()
                .ip(fileSourceRequest.getIp())
                .pattern(fileSourceRequest.getPattern())
                .timeOffset(fileSourceRequest.getTimeOffset())
                .build();
    }

    public static FileSourceDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, FileSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage());
        }
    }

}
