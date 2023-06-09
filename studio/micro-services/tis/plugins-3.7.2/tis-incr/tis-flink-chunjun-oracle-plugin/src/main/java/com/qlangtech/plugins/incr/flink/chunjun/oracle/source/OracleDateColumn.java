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

package com.qlangtech.plugins.incr.flink.chunjun.oracle.source;

import com.dtstack.chunjun.element.column.TimestampColumn;

import java.sql.Timestamp;

/**
 * Oracle的date类型在jdbc返回的是Timestamp 类型，在flink中传输使用int序列化的
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-09 18:54
 **/
public class OracleDateColumn extends TimestampColumn {
    public OracleDateColumn(Timestamp data) {
        super(data, 0);
    }

    @Override
    public Integer asInt() {
        Timestamp date = (Timestamp) data;
        return (int) date.toLocalDateTime().toLocalDate().toEpochDay();
        //   return (int) LocalDate.of(date.getYear(), date.getMonth(), date.getDate()).toEpochDay();// date.toLocalDateTime().toLocalDate().toEpochDay();
    }
}
