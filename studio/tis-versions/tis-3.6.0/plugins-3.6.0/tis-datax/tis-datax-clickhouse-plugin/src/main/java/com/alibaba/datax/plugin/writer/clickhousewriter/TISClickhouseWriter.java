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

package com.alibaba.datax.plugin.writer.clickhousewriter;

import com.alibaba.datax.common.util.Configuration;
import com.qlangtech.tis.plugin.datax.common.RdbmsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 这个扩展是想实现Clickhouse的自动建表
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-10 09:10
 * @see com.qlangtech.tis.plugin.datax.DataXClickhouseWriter
 **/
public class TISClickhouseWriter extends com.alibaba.datax.plugin.writer.clickhousewriter.ClickhouseWriter {
    private static final Logger logger = LoggerFactory.getLogger(TISClickhouseWriter.class);

    public static class Job extends ClickhouseWriter.Job {

        @Override
        public void init() {
            Configuration cfg = super.getPluginJobConf();
            // 判断表是否存在，如果不存在则创建表
            try {
                RdbmsWriter.initWriterTable(cfg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            super.init();
        }

//        @Override
//        public void prepare() {
//            super.prepare();
//        }

    }

    public static class Task extends ClickhouseWriter.Task {

    }
}
