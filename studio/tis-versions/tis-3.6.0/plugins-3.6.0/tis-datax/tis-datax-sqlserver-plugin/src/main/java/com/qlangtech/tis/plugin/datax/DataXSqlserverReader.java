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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.datax.common.RdbmsReaderContext;
import com.qlangtech.tis.plugin.ds.IDataSourceDumper;
import com.qlangtech.tis.plugin.ds.sqlserver.SqlServerDatasourceFactory;

/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.alibaba.datax.plugin.reader.sqlserverreader.SqlServerReader
 **/
@Public
public class DataXSqlserverReader extends BasicDataXRdbmsReader<SqlServerDatasourceFactory> {
    public static final String DATAX_NAME = "SqlServer";

    @FormField(ordinal = 5, type = FormFieldType.INPUTTEXT, validate = {})
    public Boolean splitPk;

//    @FormField(ordinal = 8, type = FormFieldType.INPUTTEXT, validate = {})
//    public Integer fetchSize;


    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXSqlserverReader.class, "DataXSqlserverReader-tpl.json");
    }

    @Override
    protected RdbmsReaderContext createDataXReaderContext(String jobName, SelectedTab tab, IDataSourceDumper dumper) {
        SqlServerReaderContext readerContext = new SqlServerReaderContext(jobName, tab.getName(), dumper, this);
        return readerContext;
    }


    @TISExtension()
    public static class DefaultDescriptor extends BasicDataXRdbmsReaderDescriptor {
        public DefaultDescriptor() {
            super();
        }

        @Override
        public boolean isSupportIncr() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return EndType.SqlServer;
        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }
}
