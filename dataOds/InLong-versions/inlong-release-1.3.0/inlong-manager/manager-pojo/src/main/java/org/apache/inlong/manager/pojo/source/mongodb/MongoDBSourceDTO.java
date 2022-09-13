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

package org.apache.inlong.manager.pojo.source.mongodb;

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
import java.util.Map;

/**
 * MongoDB source info
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MongoDBSourceDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Hosts of the MongoDB server")
    private String hosts;

    @ApiModelProperty("Username of the MongoDB server")
    private String username;

    @ApiModelProperty("Password of the MongoDB server")
    private String password;

    @ApiModelProperty("MongoDB database name")
    private String database;

    @ApiModelProperty("MongoDB collection name")
    private String collection;

    @ApiModelProperty("Primary key must be shared by all tables")
    private String primaryKey;

    @ApiModelProperty("Properties for MongoDB")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static MongoDBSourceDTO getFromRequest(MongoDBSourceRequest request) {
        return MongoDBSourceDTO.builder()
                .primaryKey(request.getPrimaryKey())
                .hosts(request.getHosts())
                .username(request.getUsername())
                .password(request.getPassword())
                .database(request.getDatabase())
                .collection(request.getCollection())
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get the dto instance from the JSON string
     */
    public static MongoDBSourceDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, MongoDBSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

}
