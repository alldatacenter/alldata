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

import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;

/**
 * 由于Sink端支持upset更新方式，需要将source中update_before事件类型去除掉
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-19 10:09
 **/
public class FilterUpdateBeforeEvent {

    public static class DTOFilter implements FilterFunction<DTO> {
        @Override
        public boolean filter(DTO dto) throws Exception {
            if (dto.getEventType() == DTO.EventType.UPDATE_BEFORE) {
                return false;
            }
            return true;
        }
    }

    public static class RowDataFilter implements  FilterFunction<RowData>{
        @Override
        public boolean filter(RowData dto) throws Exception {
            if (dto.getRowKind() == RowKind.UPDATE_BEFORE) {
                return false;
            }
            return true;
        }
    }


}
