/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.tis.zookeeper;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年3月2日
 */
public class ZkPathUtils {

    public static final String INDEX_BACKFLOW_SIGNAL_PATH_SEQNODE_NAME = "task";

    /**
     * 索引回流的时候在,需要在zk上写一个标记位
     *
     * @param indexName
     * @return
     */
    public static String getIndexBackflowSignalPath(String indexName) {
        final String zkBackIndexSignalPath = "/tis-lock/dumpindex/index-back-" + indexName;
        return zkBackIndexSignalPath;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
    }
}
