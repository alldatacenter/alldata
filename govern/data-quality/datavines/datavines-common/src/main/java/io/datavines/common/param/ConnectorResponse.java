/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.param;

import lombok.Builder;

@Builder
public class ConnectorResponse {

    private Status status;

    private Object result;

    private String errorMsg;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public static enum Status {
        /**
         *
         */
        IN_PROGRESS(0),
        SUCCESS(1),
        ERROR(-1);

        private final int status;

        Status(int status) {
            this.status = status;
        }

        static Status getStatus(int status) {
            switch(status) {
                case -1:
                    return ERROR;
                case 0:
                    return IN_PROGRESS;
                case 1:
                    return SUCCESS;
                default:
                    assert false : "Unknown status!";
                    return ERROR;
            }
        }

        int status() {
            return this.status;
        }

        public boolean isSuccess() {
            return this.status == SUCCESS.status();
        }
    }
}
