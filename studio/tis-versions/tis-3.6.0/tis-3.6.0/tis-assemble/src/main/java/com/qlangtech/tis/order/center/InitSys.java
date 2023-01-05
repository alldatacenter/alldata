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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 安装系统的时候需要进行初始化
 *
 * @author: baisui 百岁
 * @create: 2020-10-20 10:12
 **/
public class InitSys {
    private static final Logger logger = LoggerFactory.getLogger(InitSys.class);

    public static void main(String[] args) throws Exception {
        IndexSwapTaskflowLauncher launcher = new IndexSwapTaskflowLauncher();
        launcher.afterPropertiesSet();
        logger.info("zk initialize has successful");
    }
}
