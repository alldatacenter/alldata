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
package com.qlangtech.tis;

/**
 *  收集数据信息的种类
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年2月5日 下午10:36:28
 */
public enum RecordExecType {

    UPDATE("update"), QUERY("query"), UPDATE_ERROR("uerror"), QUERY_ERROR("qerror");

    private final String value;

    private RecordExecType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RecordExecType parse(String value) {
        if (UPDATE.value.equals(value)) {
            return UPDATE;
        }
        if (QUERY.value.equals(value)) {
            return QUERY;
        }
        if (UPDATE_ERROR.value.equals(value)) {
            return UPDATE_ERROR;
        }
        if (QUERY_ERROR.value.equals(value)) {
            return QUERY_ERROR;
        }
        throw new IllegalArgumentException("value:" + value + " is not illegal");
    }
}
