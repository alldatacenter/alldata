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
package com.qlangtech.tis.offline;

import com.qlangtech.tis.datax.TimeFormat;
import org.apache.commons.lang.StringUtils;

import java.util.function.Supplier;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-12 12:21
 */
public class DataxUtils {
    public static final String DATAX_NAME = "dataxName";
    // 用于保存DB对应的 tables
    public static final String DATAX_DB_NAME = "dataxDB";
    public static final String EXEC_TIMESTAMP = "execTimeStamp";

    public static final String DATASOURCE_FACTORY_IDENTITY = "dataSourceFactoryId";

    public static long getDumpTimeStamp() {
        return getDumpTimeStamp(true, () -> {
            throw new UnsupportedOperationException();
        });
    }

    public static long currentTimeStamp() {
        return DataxUtils.getDumpTimeStamp(false, () -> TimeFormat.getCurrentTimeStamp());
    }

    private static long getDumpTimeStamp(boolean validateNull, Supplier<Long> dftGetter) {
        String dumpTimeStamp = System.getProperty(DataxUtils.EXEC_TIMESTAMP);
        boolean empty = false;
        if ((empty = StringUtils.isEmpty(dumpTimeStamp)) && validateNull) {
            throw new IllegalStateException("dumpTimeStamp can not be empty");
        }
        return empty ? dftGetter.get() : Long.parseLong(dumpTimeStamp);
    }
}
