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
//package com.qlangtech.tis.plugins.incr.flink.connector.clickhouse;
//
//import com.qlangtech.tis.plugin.ds.DataType;
//
//import java.io.Serializable;
//
///**
// * @author: 百岁（baisui@qlangtech.com）
// * @create: 2021-12-02 14:11
// **/
//public class ColMeta implements Serializable {
//    private String key;
//    private DataType type;
//
//    public ColMeta(String key, DataType type) {
//        this.key = key;
//        this.type = type;
//    }
//
//    public ColMeta() {
//    }
//
//    public String getKey() {
//        return key;
//    }
//
//    public void setKey(String key) {
//        this.key = key;
//    }
//
//    public DataType getType() {
//        return type;
//    }
//
//    public void setType(DataType type) {
//        this.type = type;
//    }
//}
