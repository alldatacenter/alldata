/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import datart.core.base.PageInfo;
import datart.core.common.JacksonSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseData<T> {

    @JsonInclude()
    private boolean success;

    @JsonInclude()
    private int errCode;

    @JsonInclude()
    private String message;

    private List<String> warnings;

    @JsonInclude()
    private T data;

    private PageInfo pageInfo;

    @JsonSerialize(using = JacksonSerializer.ExceptionSerialize.class)
    private Exception exception;

    public static <E> ResponseData<E> success(E data) {
        return (ResponseData<E>) ResponseData.builder()
                .data(data)
                .success(true)
                .build();
    }

    public static <E> ResponseData<E> failure(String message) {
        return (ResponseData<E>) ResponseData.builder()
                .success(false)
                .message(message)
                .build();
    }

}