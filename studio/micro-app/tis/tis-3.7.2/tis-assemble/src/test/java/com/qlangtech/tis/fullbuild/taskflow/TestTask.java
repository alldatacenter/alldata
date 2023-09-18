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
package com.qlangtech.tis.fullbuild.taskflow;

import java.util.Map;
import com.qlangtech.tis.fullbuild.taskflow.BasicTask;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年12月1日
 */
public class TestTask extends BasicTask {

    public TestTask(String name, String to) {
        this.setName(name);
        this.setSuccessTo(to);
    }

    @Override
    public void exexute(Map<String, Object> params) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("task " + this.getName() + " execute");
    }
}
