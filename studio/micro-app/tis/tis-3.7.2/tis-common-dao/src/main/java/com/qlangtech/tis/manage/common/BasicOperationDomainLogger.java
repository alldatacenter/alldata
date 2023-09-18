/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.common;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-2-5
 */
public abstract class BasicOperationDomainLogger implements OperationDomainLogger {

    @JSONField(serialize = false)
    private OperationDomainLogger logger;

    @JSONField(serialize = false)
    public String getOperationLogAppName() {
        return logger.getOperationLogAppName();
    }

    @JSONField(serialize = false)
    public String getOperationLogMemo() {
        return logger.getOperationLogMemo();
    }

    @Override
    @JSONField(serialize = false)
    public String getOpDesc() {
        return logger.getOpDesc();
    }

    @JSONField(serialize = false)
    public Short getOperationLogRuntime() {
        return logger.getOperationLogRuntime();
    }

    public void setLogger(OperationDomainLogger logger) {
        this.logger = logger;
    }

    @Override
    @JSONField(serialize = false)
    public boolean isLogHasBeenSet() {
        return this.logger != null;
    }
}
