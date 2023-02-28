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

package com.qlangtech.tis.order.center;

/**
 * 可以控制DataX执行器，数据增量同步管道等的停止
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-01 15:51
 **/
public interface IAppSourcePipelineController {
    String DATAX_FULL_PIPELINE = "dataX_full_pipeline_";

    /**
     * dataX全量会直接把进程关闭，作用于增量管道只是停止不继续消费数据（进程不会kill掉），客户可以调用
     * resume() 方法继续运行
     *
     * @param appName
     */
    public boolean stop(String appName);

    public boolean resume(String appName);

    public void registerAppSubExecNodeMetrixStatus(String appName, String subExecNodeId);
}
