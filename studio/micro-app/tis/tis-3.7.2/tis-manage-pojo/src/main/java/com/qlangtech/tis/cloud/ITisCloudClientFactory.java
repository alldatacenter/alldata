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
package com.qlangtech.tis.cloud;

/**
 * 工厂，创建 ITisCloudClient
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020-03-13 11:48
 */
public interface ITisCloudClientFactory {

    int TEST_MOCK = 3;

    int REAL_TIME_ROCKETMQ = 1;

    ITisCloudClient create();

    /**
     * 优先级
     * @return
     */
    int getTypeCode();
}
