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
package com.qlangtech.tis.realtime.transfer;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年4月9日
 */
public interface IIncreaseCounter {

    String SOLR_CONSUME_COUNT = "solrConsume";

    String TABLE_CONSUME_COUNT = "tableConsumeCount";

    MonitorSysTagMarker getMonitorTagMarker();

    // /**
    // * 是否需要被监控系统收集
    // *
    // * @return
    // */
    // boolean shallCollectByMonitorSystem();
    /**
     * 可用于監控項目打標籤 ,格式例如:<br>
     * "tags": "idc=lg,loc=beijing",
     *
     * @return
     */
    // String getTags();
    long getIncreasePastLast();

    /**
     * 历史累加值
     *
     * @return
     */
    long getAccumulation();
}
