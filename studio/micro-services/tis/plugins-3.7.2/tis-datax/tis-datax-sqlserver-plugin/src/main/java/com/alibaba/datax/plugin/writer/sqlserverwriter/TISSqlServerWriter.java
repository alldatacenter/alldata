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

package com.alibaba.datax.plugin.writer.sqlserverwriter;

import com.alibaba.datax.common.util.Configuration;
import com.qlangtech.tis.plugin.datax.common.RdbmsWriter;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-18 14:40
 **/
public class TISSqlServerWriter extends SqlServerWriter {

    public static class Job extends SqlServerWriter.Job {

        @Override
        public void init() {

            Configuration cfg = super.getPluginJobConf();
            // 判断表是否存在，如果不存在则创建表
            try {
                RdbmsWriter.initWriterTable(this.containerContext, cfg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            super.init();
        }
    }

    public static class Task extends SqlServerWriter.Task {

    }
}
