///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.qlangtech.tis.plugin.datax.common;
//
//import com.qlangtech.tis.datax.impl.DataxWriter;
//import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
//import com.qlangtech.tis.plugin.ds.IInitWriterTableExecutor;
//import org.apache.commons.lang.StringUtils;
//
//import java.util.List;
//import java.util.Objects;
//
///**
// * @author: 百岁（baisui@qlangtech.com）
// * @create: 2021-11-18 17:55
// **/
//public class InitWriterTable {
//    /**
//     * 初始化表RDBMS的表，如果表不存在就创建表
//     *
//     * @param
//     * @throws Exception
//     */
//    public static void process(String dataXName, String tableName, List<String> jdbcUrls) throws Exception {
//        if (StringUtils.isEmpty(dataXName)) {
//            throw new IllegalArgumentException("param dataXName can not be null");
//        }
//        IInitWriterTableExecutor dataXWriter
//                = (IInitWriterTableExecutor) DataxWriter.load(null, dataXName);
//
//        Objects.requireNonNull(dataXWriter, "dataXWriter can not be null,dataXName:" + dataXName);
//        dataXWriter.initWriterTable(tableName, jdbcUrls);
//    }
//}
