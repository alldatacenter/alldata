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

import static org.apache.inlong.manager.common.consts.InlongConstants.LEFT_BRACKET;

/**
 * The postgresql field type mapping strategy
 */
public class PostgreSQLFieldTypeStrategy implements FieldTypeMappingStrategy {

    private final FieldTypeMappingReader reader;

    public PostgreSQLFieldTypeStrategy() {
        this.reader = new FieldTypeMappingReader(DataNodeType.POSTGRESQL);
    }

    @Override
    public String getFieldTypeMapping(String sourceType) {
        String dataType = StringUtils.substringBefore(sourceType, LEFT_BRACKET).toUpperCase();
        return reader.getFIELD_TYPE_MAPPING_MAP().getOrDefault(dataType, sourceType.toUpperCase());
    }
}
