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

package com.qlangtech.tis.realtime;

import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.realtime.dto.DTOStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;

import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-18 12:19
 **/
public abstract class RowDataFlinkSourceHandle extends BasicFlinkSourceHandle<RowData> {

    /**
     * 处理各个表对应的数据流
     *
     * @param
     */
    @Override
    protected final void processTableStream(StreamExecutionEnvironment env
            , Tab2OutputTag<DTOStream> tab2OutputTag, SinkFuncs<RowData> sinkFunction) {
        this.processTableStream(tab2OutputTag.getSinkMapper(), sinkFunction);
    }

    /**
     * 处理各个表对应的数据流
     *
     * @param
     */
    protected abstract void processTableStream(Map<String, DTOStream> streamMap, SinkFuncs<RowData> sinkFunction);
}
