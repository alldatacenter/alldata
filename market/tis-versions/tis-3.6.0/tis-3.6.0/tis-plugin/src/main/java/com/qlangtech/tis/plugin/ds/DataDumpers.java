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
package com.qlangtech.tis.plugin.ds;


import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract the dataSource modal
 *
 * @author: baisui 百岁
 * @create: 2020-11-24 10:40
 **/
public class DataDumpers {
    public final int splitCount;
    public final Iterator<IDataSourceDumper> dumpers;

    public static DataDumpers create(List<String> jdbcUrls, TISTable table) {
        if (jdbcUrls == null || jdbcUrls.isEmpty()) {
            throw new IllegalArgumentException("param jdbcUrls can not be empty");
        }
        final int length = jdbcUrls.size();
        final AtomicInteger index = new AtomicInteger();
        Iterator<IDataSourceDumper> dsIt = new Iterator<IDataSourceDumper>() {
            @Override
            public boolean hasNext() {
                return index.get() < length;
            }

            @Override
            public IDataSourceDumper next() {
                final String jdbcUrl = jdbcUrls.get(index.getAndIncrement());
                return new DataDumpers.DefaultDumper(jdbcUrl, table);
            }
        };

        DataDumpers dumpers = new DataDumpers(length, dsIt);
        return dumpers;
    }

    public DataDumpers(int splitCount, Iterator<IDataSourceDumper> dumpers) {
        this.splitCount = splitCount;
        this.dumpers = dumpers;
    }

    private static class DefaultDumper implements IDataSourceDumper {

        public final TISTable table;
        private final String jdbcUrl;

        public DefaultDumper(String jdbcUrl, TISTable table) {
            this.table = table;
            if (StringUtils.isEmpty(jdbcUrl)) {
                throw new IllegalArgumentException("param jdbcUrl can not be null");
            }
            this.jdbcUrl = jdbcUrl;
        }

        @Override
        public void closeResource() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getRowSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ColumnMetaData> getMetaData() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Map<String, Object>> startDump() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDbHost() {
            return this.jdbcUrl;
        }
    }
}
