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

package com.qlangtech.plugins.incr.flink.chunjun.greenplum.sink;

import com.dtstack.chunjun.connector.greenplum.dialect.GreenplumDialect;
import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcOutputFormat;
import com.google.common.collect.Sets;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.common.ColMetaUtils;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-14 16:20
 **/
public class ChunjunGreenplumSinkFactory extends ChunjunSinkFactory {

    @Override
    protected boolean supportUpsetDML() {
        return false;
    }

    @Override
    protected Class<? extends JdbcDialect> getJdbcDialectClass() {
        return GreenplumDialect.class;
    }

//    @Override
//    protected JdbcDialect createJdbcDialect(SyncConf syncConf) {
//        return new GreenplumDialect();
//    }

    @Override
    protected JdbcOutputFormat createChunjunOutputFormat(DataSourceFactory dsFactory, JdbcConf conf) {
        return new GreenplumOutputFormat(dsFactory, ColMetaUtils.getColMetasMap(this, conf));
    }

//    @Override
//    protected String parseType(CMeta cm) {
//        return cm.getType().accept(new DataType.TypeVisitor<String>() {
//            @Override
//            public String bigInt(DataType type) {
//                return "BIGINT";
//            }
//
//            @Override
//            public String doubleType(DataType type) {
//                return "DOUBLE";
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
//                return "BIT";
//            }
//
//            @Override
//            public String blobType(DataType type) {
//                // TINYBLOB、BLOB、MEDIUMBLOB、LONGBLOB
//                switch (type.type) {
//                    case Types.BLOB:
//                        return "BLOB";
//                    case Types.BINARY:
//                    case Types.LONGVARBINARY:
//                        return "BINARY";
//                    case Types.VARBINARY:
//                        return "VARBINARY";
//                    default:
//                        throw new IllegalStateException("illegal type:" + type.type);
//                }
//            }
//
//            @Override
//            public String varcharType(DataType type) {
//                return "VARCHAR";
//            }
//
//            @Override
//            public String intType(DataType type) {
//                return "INT";
//            }
//
//            @Override
//            public String floatType(DataType type) {
//                return "FLOAT";
//            }
//
//            @Override
//            public String decimalType(DataType type) {
//                return "DECIMAL";
//            }
//
//            @Override
//            public String timeType(DataType type) {
//                return "TIME";
//            }
//
//            @Override
//            public String tinyIntType(DataType dataType) {
//                return "TINYINT";
//            }
//
//            @Override
//            public String smallIntType(DataType dataType) {
//                return "SMALLINT";
//            }
//        });
//    }

    @Override
    public ICompileAndPackage getCompileAndPackageManager() {
        return new CompileAndPackage(Sets.newHashSet(
                //  "tis-sink-hudi-plugin"
                ChunjunGreenplumSinkFactory.class
                // "tis-datax-hudi-plugin"
                // , "com.alibaba.datax.plugin.writer.hudi.HudiConfig"
        ));
    }

    @Override
    protected void initChunjunJdbcConf(JdbcConf jdbcConf) {

    }


//    @TISExtension
//    public static class DftDesc extends BasicChunjunSinkDescriptor{
//        @Override
//        protected IEndTypeGetter.EndType getTargetType() {
//            return IEndTypeGetter.EndType.Greenplum;
//        }
//    }
}
