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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.datax.common.RdbmsReaderContext;
import com.qlangtech.tis.plugin.ds.IDataSourceDumper;
import com.qlangtech.tis.plugin.ds.mysql.MySQLDataSourceFactory;

/**
 * https://github.com/alibaba/DataX/blob/master/mysqlreader/doc/mysqlreader.md <br>
 * https://blog.csdn.net/Shadow_Light/article/details/100749537
 *
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.alibaba.datax.plugin.reader.mysqlreader.MysqlReader
 **/
@Public
public class DataxMySQLReader extends BasicDataXRdbmsReader<MySQLDataSourceFactory> {
    public static final String DATAX_NAME = "MySQL";

    @FormField(ordinal = 1, type = FormFieldType.ENUM, validate = {Validator.require, Validator.identity})
    public Boolean splitPk;


    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataxMySQLReader.class, "mysql-reader-tpl.vm");
    }

    @Override
    protected RdbmsReaderContext createDataXReaderContext(
            String jobName, SelectedTab tab, IDataSourceDumper dumper) {
        MySQLDataSourceFactory dsFactory = this.getDataSourceFactory();

        RdbmsDataxContext rdbms = new RdbmsDataxContext(this.dataXName);
        rdbms.setJdbcUrl(dumper.getDbHost());
        rdbms.setUsername(dsFactory.getUserName());
        rdbms.setPassword(dsFactory.getPassword());
        return new MySQLDataXReaderContext(jobName, tab.getName(), rdbms, this);
    }

    @TISExtension()
    public static class DefaultDescriptor extends BasicDataXRdbmsReaderDescriptor {
        public DefaultDescriptor() {
            super();
        }

        @Override
        public boolean isSupportIncr() {
            return true;
        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }

        @Override
        public EndType getEndType() {
            return EndType.MySQL;
        }
    }
}
