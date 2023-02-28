package com.qlangtech.tis.datax;

import org.apache.commons.lang.StringUtils;

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

/**
 * 表别名
 */
public class TableAlias {
    public static final String KEY_FROM_TABLE_NAME = "fromTableName";
    private String from;
    private String to;

    public TableAlias() {
    }

    public boolean hasNotUseAlias() {
        return StringUtils.equalsIgnoreCase(this.getFrom(), this.getTo());
    }

    public TableAlias(String from) {
        this.from = from;
        // 如果使用oracle的表，表名中可能出现点，所以要将它去掉
        int indexOfCommon = StringUtils.indexOf(from, ".");
        this.to = indexOfCommon > -1 ? StringUtils.substring(from, indexOfCommon + 1) : from;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return "TableMap{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                '}';
    }
}
