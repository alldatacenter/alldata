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
 * @date 2021-04-12 12:21
 */
public class DataxUtils {
    public static final String DATAX_NAME = "dataxName";
    // 用于保存DB对应的 tables
    public static final String DATAX_DB_NAME = "dataxDB";
    public static final String EXEC_TIMESTAMP = "execTimeStamp";

    public static String getDumpTimeStamp() {
        return getDumpTimeStamp(true);
    }

    public static String getDumpTimeStamp(boolean validateNull) {
        String dumpTimeStamp = System.getProperty(DataxUtils.EXEC_TIMESTAMP);
        if (validateNull && StringUtils.isEmpty(dumpTimeStamp)) {
            throw new IllegalStateException("dumpTimeStamp can not be empty");
        }

        return dumpTimeStamp;
    }
}
