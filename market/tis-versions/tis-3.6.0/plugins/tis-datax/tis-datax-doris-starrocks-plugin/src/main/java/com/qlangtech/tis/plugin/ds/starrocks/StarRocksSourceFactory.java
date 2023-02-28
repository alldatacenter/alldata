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

package com.qlangtech.tis.plugin.ds.starrocks;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.ds.doris.DorisSourceFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-29 10:01
 **/
@Public
public class StarRocksSourceFactory extends DorisSourceFactory {

    public static final String DISPLAY_NAME = "StarRocks";

    @Override
    public Connection getConnection(String jdbcUrl) throws SQLException {
        return super.getConnection(jdbcUrl);
    }

    @Override
    public String getEscapeChar() {
        return "`";
    }

    @TISExtension
    public static class DefaultDescriptor extends DorisSourceFactory.DefaultDescriptor {
        @Override
        protected String getDataSourceName() {
            return DISPLAY_NAME;
        }
    }
}
