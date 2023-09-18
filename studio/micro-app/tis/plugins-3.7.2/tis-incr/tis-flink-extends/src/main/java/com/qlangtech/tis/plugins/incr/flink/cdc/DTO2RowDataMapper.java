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

package com.qlangtech.tis.plugins.incr.flink.cdc;

import com.qlangtech.plugins.incr.flink.cdc.DTO2RowMapper;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-19 14:34
 **/
public final class DTO2RowDataMapper extends AbstractRowDataMapper {
    // private static final Logger logger = LoggerFactory.getLogger(DTO2RowDataMapper.class);

    public DTO2RowDataMapper(List<FlinkCol> cols) {
        super(cols);
    }

    @Override
    public RowData map(DTO dto) throws Exception {
        return super.map(dto);
    }

    @Override
    protected void setRowDataVal(int index, RowData row, Object value) {
        GenericRowData rowData = (GenericRowData) row;
        //logger.info("index:" + index + ",val:" + value + ",type:" + value.getClass().getSimpleName());
        rowData.setField(index, value);
    }

    @Override
    protected GenericRowData createRowData(DTO dto) {
        return new GenericRowData(DTO2RowMapper.getKind(dto), cols.size());
    }
}
