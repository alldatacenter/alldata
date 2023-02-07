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

package org.apache.inlong.tubemq.manager.controller;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.tubemq.manager.enums.ErrorCode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TubeMQResult {

    @Builder.Default
    private String errMsg = "";
    @Builder.Default
    private int errCode = 0;
    @Builder.Default
    private boolean result = true;
    private Object data;

    public static final int ERR_CODE = -1;

    private static Gson json = new Gson();

    public static TubeMQResult errorResult(String errorMsg) {
        return TubeMQResult.builder().errCode(-1)
                .errMsg(errorMsg).result(false).data("").build();
    }

    public static TubeMQResult errorResult(ErrorCode errorCode) {
        return TubeMQResult.builder().errCode(errorCode.getCode())
                .errMsg(errorCode.getMessage()).result(false).data("").build();
    }

    public static TubeMQResult errorResult(String errorMsg, Integer errorCode) {
        return TubeMQResult.builder().errCode(errorCode)
                .errMsg(errorMsg).result(false).data("").build();
    }

    public static TubeMQResult successResult() {
        return TubeMQResult.builder().errCode(0)
                .result(true).data("").build();
    }

    public static TubeMQResult successResult(Object data) {
        return TubeMQResult.builder().errCode(0)
                .result(true).data(data).build();
    }

    public boolean isError() {
        return ERR_CODE == errCode;
    }

}
