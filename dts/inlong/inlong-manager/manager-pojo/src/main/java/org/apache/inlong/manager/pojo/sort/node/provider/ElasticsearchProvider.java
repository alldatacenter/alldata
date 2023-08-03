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

import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSink;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.load.ElasticsearchLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import java.util.List;
import java.util.Map;

/**
 * The Provider for creating Elasticsearch load nodes.
 */
public class ElasticsearchProvider implements LoadNodeProvider {

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.ELASTICSEARCH.equals(sinkType);
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        ElasticsearchSink elasticsearchSink = (ElasticsearchSink) nodeInfo;
        Map<String, String> properties = parseProperties(elasticsearchSink.getProperties());
        List<SinkField> sinkFieldList = elasticsearchSink.getSinkFieldList();
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(sinkFieldList, elasticsearchSink.getSinkName());
        List<FieldRelation> fieldRelations = parseSinkFields(sinkFieldList, constantFieldMap);
        Format format = parsingSinkMultipleFormat(elasticsearchSink.getSinkMultipleEnable(),
                elasticsearchSink.getSinkMultipleFormat());

        return new ElasticsearchLoadNode(
                elasticsearchSink.getSinkName(),
                elasticsearchSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                elasticsearchSink.getIndexName(),
                elasticsearchSink.getHosts(),
                elasticsearchSink.getUsername(),
                elasticsearchSink.getPassword(),
                elasticsearchSink.getDocumentType(),
                elasticsearchSink.getPrimaryKey(),
                elasticsearchSink.getEsVersion(),
                elasticsearchSink.getSinkMultipleEnable(),
                format,
                elasticsearchSink.getIndexPattern());
    }
}