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

package org.apache.inlong.tubemq.server.common.aaaserver;

import org.apache.inlong.tubemq.corebase.TErrCodeConstants;

public class CertifiedResult {
    public boolean result = false;
    public int errCode = TErrCodeConstants.BAD_REQUEST;
    public String errInfo = "Not authenticate!";
    public String authorizedToken = "";
    public boolean reAuth = false;
    public String userName = "";

    public CertifiedResult() {

    }

    public void setFailureResult(int errCode, final String resultInfo) {
        this.result = false;
        this.errCode = errCode;
        this.errInfo = resultInfo;
    }

    public void setSuccessResult(final String userName, final String authorizedToken) {
        this.result = true;
        this.errCode = TErrCodeConstants.SUCCESS;
        this.errInfo = "Ok!";
        this.userName = userName;
        this.authorizedToken = authorizedToken;
    }

    public void setSuccessResult(final String userName, final String authorizedToken, boolean reAuth) {
        this.result = true;
        this.errCode = TErrCodeConstants.SUCCESS;
        this.errInfo = "Ok!";
        this.userName = userName;
        this.authorizedToken = authorizedToken;
        this.reAuth = reAuth;
    }

}
