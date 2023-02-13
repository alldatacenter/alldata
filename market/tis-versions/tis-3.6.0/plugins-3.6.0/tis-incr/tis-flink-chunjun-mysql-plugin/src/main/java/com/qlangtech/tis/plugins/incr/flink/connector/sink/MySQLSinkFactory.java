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

package com.qlangtech.tis.plugins.incr.flink.connector.sink;

import com.alibaba.citrus.turbine.Context;
import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.connector.jdbc.util.JdbcUtil;
import com.google.common.collect.Sets;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.common.ColMetaUtils;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.dialect.TISMysqlDialect;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-05-07 20:11
 **/
public class MySQLSinkFactory extends ChunjunSinkFactory {
    @Override
    protected boolean supportUpsetDML() {
        return true;
    }

    @Override
    protected void initChunjunJdbcConf(JdbcConf jdbcConf) {
        JdbcUtil.putExtParam(jdbcConf);
    }

    @Override
    protected Class<? extends JdbcDialect> getJdbcDialectClass() {
        return TISMysqlDialect.class;
    }


    /**
     * ==========================================================
     * End impl: IStreamTableCreator
     * ===========================================================
     */


    @Override
    public ICompileAndPackage getCompileAndPackageManager() {
        return new CompileAndPackage(Sets.newHashSet(
                //  "tis-sink-hudi-plugin"
                MySQLSinkFactory.class
                // "tis-datax-hudi-plugin"
                // , "com.alibaba.datax.plugin.writer.hudi.HudiConfig"
        ));
    }

    @Override
    protected TISMysqlOutputFormat createChunjunOutputFormat(DataSourceFactory dsFactory, JdbcConf jdbcConf) {

        TISMysqlOutputFormat outputFormat = new TISMysqlOutputFormat(dsFactory, ColMetaUtils.getColMetasMap(this, jdbcConf));

        return outputFormat;
    }


    @TISExtension
    public static class DefaultDescriptor extends BasicChunjunSinkDescriptor {
        @Override
        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            return super.validateAll(msgHandler, context, postFormVals);
        }

        @Override
        protected IEndTypeGetter.EndType getTargetType() {
            return IEndTypeGetter.EndType.MySQL;
        }
    }
}
