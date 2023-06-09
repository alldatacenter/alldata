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
package com.qlangtech.tis.sql.parser.meta;

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ColumnTransfer {

    private String colKey;

    private String transfer;

    private String param;

    public ColumnTransfer() {
    }

    @Override
    public String toString() {
        return "ColumnTransfer{" + "colKey='" + colKey + '\'' + ", transfer='" + transfer + '\'' + ", param='" + param + '\'' + '}';
    }

    public ColumnTransfer(String colKey, String transfer, String param) {
        if (StringUtils.isEmpty(colKey)) {
            throw new IllegalArgumentException("param colKey can not be null");
        }
        if (StringUtils.isEmpty(transfer)) {
            throw new IllegalArgumentException("param transfer can not be null");
        }
        if (StringUtils.isEmpty(param)) {
            throw new IllegalArgumentException("param param can not be null");
        }
        this.colKey = colKey;
        this.transfer = transfer;
        this.param = param;
    }

    public String getColKey() {
        return colKey;
    }

    public void setColKey(String colKey) {
        this.colKey = colKey;
    }

    public String getTransfer() {
        return transfer;
    }

    public void setTransfer(String transfer) {
        this.transfer = transfer;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}
