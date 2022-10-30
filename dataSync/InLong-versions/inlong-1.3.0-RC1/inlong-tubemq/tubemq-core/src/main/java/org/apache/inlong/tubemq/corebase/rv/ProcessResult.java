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

public class ProcessResult extends RetValue {

    private Object retData1 = null;

    public ProcessResult() {
        super();
    }

    public ProcessResult(ProcessResult other) {
        super(other);
        this.retData1 = other.retData1;
    }

    public ProcessResult(Object retData) {
        super();
        this.retData1 = retData;
    }

    public ProcessResult(int errCode, String errInfo) {
        super(errCode, errInfo);
    }

    public void setFailResult(int errCode, final String errMsg) {
        super.setFailResult(errCode, errMsg);
        this.retData1 = null;
    }

    public void setFailResult(final String errMsg) {
        super.setFailResult(errMsg);
        this.retData1 = null;
    }

    public void setSuccResult() {
        super.setSuccResult();
        this.retData1 = null;
    }

    public void setSuccResult(Object retData) {
        super.setSuccResult();
        this.retData1 = retData;
    }

    public Object getRetData() {
        return retData1;
    }

    public void clear() {
        super.clear();
        this.retData1 = null;
    }
}
