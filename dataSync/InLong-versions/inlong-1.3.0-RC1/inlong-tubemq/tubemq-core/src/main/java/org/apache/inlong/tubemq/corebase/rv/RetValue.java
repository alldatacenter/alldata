/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.rv;

import org.apache.inlong.tubemq.corebase.TErrCodeConstants;

public class RetValue {
    private boolean success = true;
    private int errCode = TErrCodeConstants.SUCCESS;
    private String errMsg = "";

    public RetValue() {

    }

    public RetValue(RetValue other) {
        this.success = other.success;
        this.errCode = other.errCode;
        this.errMsg = other.errMsg;
    }

    public RetValue(int errCode, String errMsg) {
        this.success = false;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public RetValue(boolean isSuccess, int errCode, String errMsg) {
        this.success = isSuccess;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public void setFullInfo(boolean isSuccess, int errCode, String errMsg) {
        this.success = isSuccess;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public void setFailResult(int errCode, final String errMsg) {
        this.success = false;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public void setFailResult(final String errMsg) {
        this.success = false;
        this.errCode = TErrCodeConstants.BAD_REQUEST;
        this.errMsg = errMsg;
    }

    public void setSuccResult() {
        this.success = true;
        this.errMsg = "Ok!";
        this.errCode = TErrCodeConstants.SUCCESS;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getErrCode() {
        return errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void clear() {
        this.success = true;
        this.errCode = TErrCodeConstants.SUCCESS;
        this.errMsg = "";
    }
}
