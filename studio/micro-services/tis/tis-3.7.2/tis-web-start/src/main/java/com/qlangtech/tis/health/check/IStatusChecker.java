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
package com.qlangtech.tis.health.check;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public interface IStatusChecker {

    /**
     * 初始化
     */
    void init();

    /**
     * 自己在StatusChecker 列表的排序位置，越靠前越小<br>
     * 从1开始
     *
     * @return
     */
    int order();

    /**
     * StatusChecker支持的模型
     *
     * @return
     * @see
     */
    Mode mode();

    /**
     * 具体业务实现类，实现监控检查
     *
     * @return
     */
    StatusModel check();
}
