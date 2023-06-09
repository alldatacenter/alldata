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

package com.qlangtech.tis.plugin.datax.common;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.job.IJobContainerContext;
import com.alibaba.datax.plugin.rdbms.writer.Constant;
import com.alibaba.datax.plugin.rdbms.writer.Key;
import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.impl.DataxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-06 22:28
 **/
public class RdbmsWriter {
    private static final Logger logger = LoggerFactory.getLogger(RdbmsWriter.class);


    /**
     * 适用于这种类型的配置
     * <pre>
     * "connection":[
     *   {
     *    "jdbcUrl":"jdbc:clickhouse://192.168.28.201:8123/tis",
     *    "table":[
     *      "instancedetail"
     *    ]
     *   }
     * ]
     * <pre>
     * @param cfg
     * @throws Exception
     */
    public static void initWriterTable(IJobContainerContext containerContext, Configuration cfg) {
        Objects.requireNonNull(containerContext,"containerContext can not be null");
        String dataXName = containerContext.getTISDataXName();

        String tableName = cfg.getNecessaryValue(Constant.CONN_MARK + "[0]." + Key.TABLE + "[0]"
                , RdbmsWriterErrorCode.REQUIRED_TABLE_NAME_PARAM_ERROR);
        List<String> jdbcUrls = Lists.newArrayList();
        List<Object> connections = cfg.getList(Constant.CONN_MARK, Object.class);
        for (int i = 0, len = connections.size(); i < len; i++) {
            Configuration connConf = Configuration.from(String.valueOf(connections.get(i)));
            String jdbcUrl = connConf.getString(Key.JDBC_URL);
            jdbcUrls.add(jdbcUrl);
        }

        try {
            DataxWriter.process(dataXName, tableName, jdbcUrls);
        } catch (Exception e) {
            throw DataXException.asDataXException(RdbmsWriterErrorCode.INITIALIZE_TABLE_ERROR, tableName, e);
        }
    }


}
