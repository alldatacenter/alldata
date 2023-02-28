/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.datax.common;

import com.alibaba.datax.common.spi.ErrorCode;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-06 22:40
 **/
public enum RdbmsWriterErrorCode implements ErrorCode {
    REQUIRED_DATAX_PARAM_ERROR("RdbmsWriter-01", "config param 'dataxName' is required"),
    REQUIRED_TABLE_NAME_PARAM_ERROR("RdbmsWriter-02", "config param 'tableName' is required"),
    INITIALIZE_TABLE_ERROR("RdbmsWriter-03", "initialize 'tableName' faild");

    private final String code;
    private final String description;

    private RdbmsWriterErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s].", this.code, this.description);
    }
}
