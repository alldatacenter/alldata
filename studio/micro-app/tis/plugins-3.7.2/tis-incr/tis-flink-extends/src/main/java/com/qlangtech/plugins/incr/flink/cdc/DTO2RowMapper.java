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

package com.qlangtech.plugins.incr.flink.cdc;

import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;

import java.util.List;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-18 12:04
 **/
public final class DTO2RowMapper implements MapFunction<DTO, Row> {
    private final List<FlinkCol> cols;

    public DTO2RowMapper(List<FlinkCol> cols) {
        this.cols = cols;
    }

    @Override
    public Row map(DTO dto) throws Exception {

//        File file = new File("full_types_dto.xml");
//        XmlFile tabStore = new XmlFile(file.getAbsoluteFile(), "test");
//        tabStore.write(dto, Sets.newHashSet());

        Row row = new Row(getKind(dto), cols.size());
        int index = 0;
        Map<String, Object> vals
                = (dto.getEventType() == DTO.EventType.DELETE ? dto.getBefore() : dto.getAfter());
        Object val = null;
        for (FlinkCol col : cols) {
            val = vals.get(col.name);
            //col.type
            row.setField(index++, (val == null) ? null : col.rowProcess.apply(val));
        }
        return row;
    }

    public static RowKind getKind(DTO dto) {
        switch (dto.getEvent()) {
            case DELETE:
                return RowKind.DELETE;
            case UPDATE_AFTER:
                return RowKind.UPDATE_AFTER;
            case UPDATE_BEFORE:
                return RowKind.UPDATE_BEFORE;
            case ADD:
                return RowKind.INSERT;
            default:
                throw new IllegalStateException("invalid event type:" + dto.getEvent());
        }

    }
}
