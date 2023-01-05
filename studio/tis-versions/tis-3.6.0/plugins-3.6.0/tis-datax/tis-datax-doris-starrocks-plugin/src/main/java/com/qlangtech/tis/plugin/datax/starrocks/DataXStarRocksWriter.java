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

package com.qlangtech.tis.plugin.datax.starrocks;

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.datax.BasicDorisStarRocksWriter;
import com.qlangtech.tis.plugin.ds.starrocks.StarRocksSourceFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-29 10:08
 **/
@Public
public class DataXStarRocksWriter extends BasicDorisStarRocksWriter<StarRocksSourceFactory> {

    public static String getDftLoadProps() {
        return "{\n" +
                "    \"" + Separator.COL_SEPARATOR + "\": \"" + Separator.COL_SEPARATOR_DEFAULT + "\",\n" +
                "    \"" + Separator.ROW_DELIMITER + "\": \"" + Separator.ROW_DELIMITER_DEFAULT + "\"\n" +
                "}";
    }

    @Override
    public Separator getSeparator() {
        JSONObject props = getLoadProps();
        return new Separator() {
            @Override
            public String getColumnSeparator() {
                return StringUtils.defaultIfBlank(props.getString(Separator.COL_SEPARATOR), COL_SEPARATOR_DEFAULT);
            }

            @Override
            public String getRowDelimiter() {
                return StringUtils.defaultIfBlank(props.getString(Separator.ROW_DELIMITER), ROW_DELIMITER_DEFAULT);
            }
        };
    }

    @Override
    protected BasicCreateTableSqlBuilder createSQLDDLBuilder(IDataxProcessor.TableMap tableMapper) {
        return new BasicCreateTableSqlBuilder(tableMapper, this.getDataSourceFactory()) {
            @Override
            protected String getUniqueKeyToken() {
                return "PRIMARY KEY";
            }
        };
    }

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXStarRocksWriter.class, "writer-tpl.json");
    }

    @TISExtension()
    public static class DefaultDescriptor extends BaseDescriptor {
        @Override
        public String getDisplayName() {
            return StarRocksSourceFactory.DISPLAY_NAME;
        }

        @Override
        protected String getRowDelimiterKey() {
            return Separator.ROW_DELIMITER;
        }
        @Override
        protected String getColSeparatorKey() {
            return Separator.COL_SEPARATOR;
        }

        @Override
        public EndType getEndType() {
            return EndType.StarRocks;
        }
    }
}
