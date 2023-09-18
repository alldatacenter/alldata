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

package com.qlangtech.tis.realtime;

import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-27 10:38
 **/
public class SourceProcessFunction extends ProcessFunction<DTO, DTO> {
    private final Map<String, OutputTag<DTO>> tab2OutputTag;

    public SourceProcessFunction(Map<String, OutputTag<DTO>> tab2OutputTag) {
        this.tab2OutputTag = tab2OutputTag;
    }

    @Override
    public void processElement(DTO in, Context ctx, Collector<DTO> out) throws Exception {
        //side_output: https://ci.apache.org/projects/flink/flink-docs-stable/dev/stream/side_output.html
        final String tabName = in.getTableName();
        OutputTag<DTO> outputTag = tab2OutputTag.get(tabName);
        if (outputTag == null) {
            throw new IllegalStateException("target table:" + tabName + " can not find relevant in tab2OutputTag");
        }
        ctx.output(outputTag, in);
    }
}
