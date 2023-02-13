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

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-08 11:22
 **/
public interface IEndTypeGetter {
    public static void main(String[] args) {
        for (EndType value : EndType.values()) {
            System.out.print(value.val + ",");
        }
    }

    /**
     * 是否支持增量执行
     *
     * @return
     */
    default boolean isSupportIncr() {
        throw new UnsupportedOperationException();
    }

    /**
     * 取得数据端类型
     *
     * @return
     */
    EndType getEndType();

    /**
     * 供应商
     *
     * @return
     */
    PluginVender getVender();


    enum PluginVender {
        FLINK_CDC("FlinkCDC", "flink-cdc", "https://ververica.github.io/flink-cdc-connectors") //
        , CHUNJUN("Chunjun", "chunjun", "https://dtstack.github.io/chunjun") //
        , TIS("TIS", "tis", "https://github.com/qlangtech/tis") //
        , DATAX("DataX", "datax", "https://github.com/alibaba/DataX");
        final String name;
        final String tokenId;
        final String url;

        private PluginVender(String name, String tokenId, String url) {
            this.name = name;
            this.tokenId = tokenId;
            this.url = url;
        }

        public String getTokenId() {
            return this.tokenId;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }

    /**
     * 端类型
     */
    enum EndType {
        Greenplum("greenplum"), MySQL("mysql"), Postgres("pg"), Oracle("oracle") //
        , ElasticSearch("es"), MongoDB("mongoDB"), StarRocks("starRocks"), Doris("doris") //
        , Clickhouse("clickhouse"), Hudi("hudi"), AliyunOSS("aliyunOSS"), FTP("ftp") //
        , Cassandra("cassandra"), HDFS("hdfs"), SqlServer("sqlServer"), TiDB("TiDB") //
        , RocketMQ("rocketMq"), Kafka("kafka");
        private final String val;

        EndType(String val) {
            this.val = val;
        }

        public String getVal() {
            return this.val;
        }
    }
}
