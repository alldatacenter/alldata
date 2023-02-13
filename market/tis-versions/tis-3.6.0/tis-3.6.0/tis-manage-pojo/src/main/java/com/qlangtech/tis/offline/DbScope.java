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
package com.qlangtech.tis.offline;

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public enum DbScope {

    // 用于Dump数据库表的
    DETAILED(StringUtils.EMPTY, "detailed"),
    // 用于增量任务执行
    FACADE("_facade", "facade");

    private final String type;
    private final String token;

    /**
     *
     * @param val
     * @return
     */
    public static DbScope parse(String val) {
        if (StringUtils.isEmpty(val)) {
            throw new IllegalArgumentException("param 'val' can not be null");
        }
        for (DbScope type : DbScope.values()) {
            if (type.token.equals(val)) {
                return type;
            }
        }

        throw new IllegalStateException("illegal val:" + val);
    }

    private DbScope(String type, String token) {
        this.type = type;
        this.token = token;
    }

    public String getDBType() {
        return this.type;
    }
}
