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

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.DataSourceFactoryPluginStore;
import com.qlangtech.tis.plugin.ds.PostedDSProp;
import com.qlangtech.tis.plugin.ds.cassandra.CassandraDatasourceFactory;

import java.util.Optional;

/**
 * @see com.alibaba.datax.plugin.writer.cassandrawriter.CassandraWriter
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 **/
@Public
public class DataXCassandraWriter extends DataxWriter {
    //private static final String DATAX_NAME = "Cassandra";

    @FormField(ordinal = 0, type = FormFieldType.ENUM, validate = {Validator.require})
    public String dbName;

    //    @FormField(ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String host;
//    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String port;
//    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {})
//    public String username;
//    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, validate = {})
//    public String password;
//    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {})
//    public String useSSL;
    @FormField(ordinal = 5, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer connectionsPerHost;
    @FormField(ordinal = 6, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer maxPendingPerConnection;
    //    @FormField(ordinal = 7, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String keyspace;
//    @FormField(ordinal = 8, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String table;
//    @FormField(ordinal = 9, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String column;
    @FormField(ordinal = 10, type = FormFieldType.ENUM, validate = {})
    public String consistancyLevel;
    @FormField(ordinal = 11, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer batchSize;

    @FormField(ordinal = 12, type = FormFieldType.TEXTAREA,advance = false , validate = {Validator.require})
    public String template;

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXCassandraWriter.class, "DataXCassandraWriter-tpl.json");
    }


    @Override
    public String getTemplate() {
        return this.template;
    }

    @Override
    public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
        if (!tableMap.isPresent()) {
            throw new IllegalArgumentException("param tableMap shall be present");
        }
        return new CassandraWriterContext(this, tableMap.get());
    }

    public  CassandraDatasourceFactory getDataSourceFactory() {
        return  TIS.getDataBasePlugin( PostedDSProp.parse(this.dbName));
      //  return (CassandraDatasourceFactory) dsStore.getPlugin();
    }


    @TISExtension()
    public static class DefaultDescriptor extends BaseDataxWriterDescriptor {
        public DefaultDescriptor() {
            super();
        }

        @Override
        public boolean isRdbms() {
            return true;
        }

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
            return DataXCassandraReader.DATAX_NAME;
        }
    }
}
