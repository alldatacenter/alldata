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

package org.apache.inlong.manager.common.fieldtype.strategy;

import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.fieldtype.FieldTypeMappingReader;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.inlong.manager.common.consts.InlongConstants.LEFT_BRACKET;

/**
 * The ClickHouse field type mapping strategy
 */
public class ClickHouseFieldTypeStrategy implements FieldTypeMappingStrategy {

    private final FieldTypeMappingReader reader;

    private final static String NULLABLE_PATTERN = "^NULLABLE\\((.*)\\)$";

    private static final Pattern PATTERN = Pattern.compile(NULLABLE_PATTERN);

    public ClickHouseFieldTypeStrategy() {
        this.reader = new FieldTypeMappingReader(DataNodeType.CLICKHOUSE);
    }

    @Override
    public String getFieldTypeMapping(String sourceType) {
        // support clickHouse field type special modifier Nullable
        if (StringUtils.isNotBlank(sourceType)) {
            Matcher matcher = PATTERN.matcher(sourceType.toUpperCase());
            if (matcher.matches()) {
                // obtain the field type modified by Nullable, for example, uint8(12) in Nullable(uint8(12))
                sourceType = matcher.group(1);
            }
        }
        String dataType = StringUtils.substringBefore(sourceType, LEFT_BRACKET).toUpperCase();
        return reader.getFIELD_TYPE_MAPPING_MAP().getOrDefault(dataType, sourceType.toUpperCase());
    }
}
