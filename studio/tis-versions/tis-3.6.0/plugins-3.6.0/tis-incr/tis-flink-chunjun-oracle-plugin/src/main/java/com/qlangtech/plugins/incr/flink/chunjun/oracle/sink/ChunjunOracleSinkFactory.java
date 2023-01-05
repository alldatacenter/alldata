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

package com.qlangtech.plugins.incr.flink.chunjun.oracle.sink;

import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcOutputFormat;
import com.google.common.collect.Sets;
import com.qlangtech.plugins.incr.flink.chunjun.oracle.dialect.TISOracleDialect;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.common.ColMetaUtils;
import com.qlangtech.tis.plugins.incr.flink.chunjun.sink.TISJdbcOutputFormat;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;

import java.util.Map;

/**
 * https://dtstack.github.io/chunjun/documents/ChunJun%E8%BF%9E%E6%8E%A5%E5%99%A8@oracle@oracle-sink
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-14 21:44
 * @see com.dtstack.chunjun.connector.oracle.sink.OracleSinkFactory
 **/
public class ChunjunOracleSinkFactory extends ChunjunSinkFactory {

    @Override
    protected Class<? extends JdbcDialect> getJdbcDialectClass() {
        return TISOracleDialect.class;
    }

    @Override
    protected boolean supportUpsetDML() {
        return true;
    }

    @Override
    protected JdbcOutputFormat createChunjunOutputFormat(DataSourceFactory dsFactory, JdbcConf conf) {

        return new TISJdbcOutputFormat(dsFactory, ColMetaUtils.getColMetasMap(this, conf));
    }

    @Override
    protected void setSchema(Map<String, Object> conn, String dbName, BasicDataSourceFactory dsFactory) {
        //super.setSchema(conn, dbName, dsFactory);
    }

    @Override
    protected void initChunjunJdbcConf(JdbcConf jdbcConf) {

    }

//    @Override
//    protected String parseType(CMeta cm) {
//        return typeMap(cm);
//    }

//    public static String typeMap(CMeta cm) {
//        return cm.getType().getS();
//        return cm.getType().accept(new DataType.TypeVisitor<String>() {
//            @Override
//            public String bigInt(DataType type) {
//                return "INTEGER";
//            }
//
//            @Override
//            public String doubleType(DataType type) {
//                return "BINARY_DOUBLE";
//            }
//
//            @Override
//            public String dateType(DataType type) {
//                return "DATE";
//            }
//
//            @Override
//            public String timestampType(DataType type) {
//                return "TIMESTAMP";
//            }
//
//            @Override
//            public String bitType(DataType type) {
//                return "NUMBER";
//            }
//
//            @Override
//            public String blobType(DataType type) {
//                return "BLOB";
//            }
//
//            @Override
//            public String varcharType(DataType type) {
//                return "VARCHAR";
//            }
//
//            @Override
//            public String intType(DataType type) {
//                return "INTEGER";
//            }
//
//            @Override
//            public String floatType(DataType type) {
//                return "BINARY_FLOAT";
//            }
//
//            @Override
//            public String decimalType(DataType type) {
//                return "DECIMAL";
//            }
//
//            @Override
//            public String timeType(DataType type) {
//                return "TIMESTAMP";
//            }
//
//            @Override
//            public String tinyIntType(DataType dataType) {
//                return "SMALLINT";
//            }
//
//            @Override
//            public String smallIntType(DataType dataType) {
//                return this.tinyIntType(dataType);
//            }
//        });
    // }

    @Override
    public ICompileAndPackage getCompileAndPackageManager() {
        return new CompileAndPackage(Sets.newHashSet(ChunjunOracleSinkFactory.class));
    }

    @TISExtension
    public static class DftDesc extends BasicChunjunSinkDescriptor {
        @Override
        protected IEndTypeGetter.EndType getTargetType() {
            return IEndTypeGetter.EndType.Oracle;
        }
    }
}
