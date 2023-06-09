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

package com.qlangtech.tis.plugin;

import java.util.Set;

/**
 * 端类型
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-05 10:13
 **/
public interface IEndTypeGetter {

    String KEY_END_TYPE = "endType";

    public static void main(String[] args) {
        for (EndType value : EndType.values()) {
            System.out.print(value.val + ",");
        }
    }

    /**
     * 取得数据端类型
     *
     * @return
     */
    IDataXEndTypeGetter.EndType getEndType();

    /**
     * 端类型
     */
    enum EndType {
        Greenplum("greenplum"), MySQL("mysql"), Postgres("pg"), Oracle("oracle") //
        , ElasticSearch("es"), MongoDB("mongoDB"), StarRocks("starRocks"), Doris("doris") //
        , Clickhouse("clickhouse"), Hudi("hudi"), AliyunOSS("aliyunOSS"), FTP("ftp") //
        , Cassandra("cassandra"), HDFS("hdfs"), SqlServer("sqlServer"), TiDB("TiDB") //
        , RocketMQ("rocketMq"), Kafka("kafka"), DataFlow("dataflow") //
        , AliyunODPS("aliyunOdps"), HiveMetaStore("hms"), RabbitMQ("rabbitmq");
        private final String val;

        public static EndType parse(String endType) {
            for (EndType end : EndType.values()) {
                if (end.val.equals(endType)) {
                    return end;
                }
            }
            throw new IllegalStateException("illegal endType:" + endType);
        }


        EndType(String val) {
            this.val = val;
        }

        public String getVal() {
            return this.val;
        }

        public boolean containIn(Set<String> endTypes) {
            return endTypes.contains(this.getVal());
        }
    }
}
