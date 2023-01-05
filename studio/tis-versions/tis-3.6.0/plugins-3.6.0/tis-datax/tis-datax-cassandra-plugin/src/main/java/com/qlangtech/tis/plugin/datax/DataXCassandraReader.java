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
import com.qlangtech.tis.plugin.ds.cassandra.CassandraDatasourceFactory;

/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.alibaba.datax.plugin.reader.cassandrareader.CassandraReader
 **/
@Public
public class DataXCassandraReader extends BasicDataXRdbmsReader<CassandraDatasourceFactory> {
    public static final String DATAX_NAME = "Cassandra";

    //    @FormField(ordinal = 6, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String table;
//    @FormField(ordinal = 7, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String column;
//    @FormField(ordinal = 8, type = FormFieldType.INPUTTEXT, validate = {})
//    public String where;
    @FormField(ordinal = 9, type = FormFieldType.ENUM, validate = {})
    public Boolean allowFiltering;
    @FormField(ordinal = 10, type = FormFieldType.ENUM, validate = {})
    public String consistancyLevel;

    protected boolean isFilterUnexistCol() {
        return false;
    }

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXCassandraReader.class, "DataXCassandraReader-tpl.json");
    }

    @Override
    protected RdbmsReaderContext createDataXReaderContext(String jobName, SelectedTab tab
            , IDataSourceDumper dumper) {
        CassandraReaderContext readerContext = new CassandraReaderContext(jobName, tab, dumper, this);
        return readerContext;
    }


    @TISExtension()
    public static class DefaultDescriptor extends BasicDataXRdbmsReaderDescriptor {
        @Override
        public boolean isSupportIncr() {
            return false;
        }
        @Override
        public EndType getEndType() {
            return EndType.Cassandra;
        }
        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }
}
