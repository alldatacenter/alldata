/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.hbase.option;

import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.ReaderOptions;

import com.alibaba.fastjson.TypeReference;

import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;
import static com.bytedance.bitsail.common.option.ReaderOptions.READER_PREFIX;

public  interface HBaseReaderOptions extends ReaderOptions.BaseReaderOptions {

  /**
   * HBase configurations for creating connections.
   * For example: <br/>
   * {
   * "hbase.zookeeper.property.clientPort": "2181",
   * "hbase.rootdir": "hdfs://ns1/hbase",
   * "hbase.cluster.distributed": "true",
   * "hbase.zookeeper.quorum": "node01,node02,node03",
   * "zookeeper.znode.parent": "/hbase"
   * }
   */
  ConfigOption<Map<String, Object>> HBASE_CONF =
      key(READER_PREFIX + "hbase_conf")
          .onlyReference(new TypeReference<Map<String, Object>>(){});

  /**
   * Table to read.
   */
  ConfigOption<String> TABLE =
      key(READER_PREFIX + "table")
          .noDefaultValue(String.class);

  /**
   * Support UTF-8, UTF-16, ISO-8859-1 and <i>etc.</i>.<br/>
   * Default encoding is UTF-8.
   */
  ConfigOption<String> ENCODING =
      key(READER_PREFIX + "encoding")
          .defaultValue("UTF-8");
}
