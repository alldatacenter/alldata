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

package org.apache.inlong.manager.pojo.sort.node.provider;

import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.fieldtype.strategy.FieldTypeMappingStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.MongoDBFieldTypeStrategy;
import org.apache.inlong.manager.pojo.sort.node.base.ExtractNodeProvider;
import org.apache.inlong.manager.pojo.source.mongodb.MongoDBSource;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.extract.MongoExtractNode;

import java.util.List;
import java.util.Map;

/**
 * The Provider for creating MongoDB extract nodes.
 */
public class MongoDBProvider implements ExtractNodeProvider {

    private static final FieldTypeMappingStrategy FIELD_TYPE_MAPPING_STRATEGY = new MongoDBFieldTypeStrategy();

    @Override
    public Boolean accept(String sourceType) {
        return SourceType.MONGODB.equals(sourceType);
    }

    @Override
    public ExtractNode createExtractNode(StreamNode streamNodeInfo) {
        MongoDBSource source = (MongoDBSource) streamNodeInfo;
        List<FieldInfo> fieldInfos = parseStreamFieldInfos(source.getFieldList(), source.getSourceName(),
                FIELD_TYPE_MAPPING_STRATEGY);
        Map<String, String> properties = parseProperties(source.getProperties());

        return new MongoExtractNode(
                source.getSourceName(),
                source.getSourceName(),
                fieldInfos,
                null,
                properties,
                source.getCollection(),
                source.getHosts(),
                source.getUsername(),
                source.getPassword(),
                source.getDatabase());
    }
}