/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.pojo.stream;

import org.apache.inlong.manager.common.tool.excel.validator.ExcelCellValidator;

import java.util.ArrayList;
import java.util.List;

import static org.apache.inlong.manager.common.consts.InlongConstants.STREAM_FIELD_TYPES;

/**
 * This class is used to validate the stream field type in the business product.
 */
public class StreamFieldTypeCellValidator implements ExcelCellValidator<String> {

    public StreamFieldTypeCellValidator() {
        // do nothing
    }

    private final String invalidateTip = String.format("StreamField type must be one of %s", STREAM_FIELD_TYPES);

    @Override
    public List<String> constraint() {
        return new ArrayList<>(STREAM_FIELD_TYPES);
    }

    @Override
    public boolean validate(String value) {
        return STREAM_FIELD_TYPES.contains(value);
    }

    @Override
    public String getInvalidTip() {
        return invalidateTip;
    }

}
