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
package com.qlangtech.tis.fullbuild.taskflow.impl;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qlangtech.tis.fullbuild.taskflow.BasicTask;

/**
 * 开始节点
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年11月30日
 */
public class StartTask extends BasicTask {

    public static final String NAME = "start";

    private static final Logger logger = LoggerFactory.getLogger(StartTask.class);

    public StartTask() {
        super();
        // this.setName(NAME);
        this.name = NAME;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public void exexute(Map<String, Object> params) {
        logger.info("start");
        System.out.println("start execute");
    }
}
