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

import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.pojo.sink.redis.RedisSink;
import org.apache.inlong.manager.pojo.sort.node.base.ExtractNodeProvider;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.source.redis.RedisLookupOptions;
import org.apache.inlong.manager.pojo.source.redis.RedisSource;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.LookupOptions;
import org.apache.inlong.sort.protocol.enums.RedisCommand;
import org.apache.inlong.sort.protocol.enums.RedisMode;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.extract.RedisExtractNode;
import org.apache.inlong.sort.protocol.node.format.AvroFormat;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.DebeziumJsonFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.format.InLongMsgFormat;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.format.RawFormat;
import org.apache.inlong.sort.protocol.node.load.RedisLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * The Provider for creating Redis extract or load nodes.
 */
public class RedisProvider implements ExtractNodeProvider, LoadNodeProvider {

    @Override
    public Boolean accept(String sourceType) {
        return SourceType.REDIS.equals(sourceType);
    }

    @Override
    public ExtractNode createExtractNode(StreamNode streamNodeInfo) {
        RedisSource source = (RedisSource) streamNodeInfo;
        List<FieldInfo> fieldInfos = parseStreamFieldInfos(source.getFieldList(), source.getSourceName());
        Map<String, String> properties = parseProperties(source.getProperties());

        RedisMode redisMode = RedisMode.forName(source.getRedisMode());
        switch (redisMode) {
            case STANDALONE:
                return new RedisExtractNode(
                        source.getSourceName(),
                        source.getSourceName(),
                        fieldInfos,
                        null,
                        properties,
                        source.getPrimaryKey(),
                        RedisCommand.forName(source.getCommand()),
                        source.getHost(),
                        source.getPort(),
                        source.getPassword(),
                        source.getAdditionalKey(),
                        source.getDatabase(),
                        source.getTimeout(),
                        source.getSoTimeout(),
                        source.getMaxTotal(),
                        source.getMaxIdle(),
                        source.getMinIdle(),
                        parseLookupOptions(source.getLookupOptions()));
            case SENTINEL:
                return new RedisExtractNode(
                        source.getSourceName(),
                        source.getSourceName(),
                        fieldInfos,
                        null,
                        properties,
                        source.getPrimaryKey(),
                        RedisCommand.forName(source.getCommand()),
                        source.getMasterName(),
                        source.getSentinelsInfo(),
                        source.getPassword(),
                        source.getAdditionalKey(),
                        source.getDatabase(),
                        source.getTimeout(),
                        source.getSoTimeout(),
                        source.getMaxTotal(),
                        source.getMaxIdle(),
                        source.getMinIdle(),
                        parseLookupOptions(source.getLookupOptions()));
            case CLUSTER:
                return new RedisExtractNode(
                        source.getSourceName(),
                        source.getSourceName(),
                        fieldInfos,
                        null,
                        properties,
                        source.getPrimaryKey(),
                        RedisCommand.forName(source.getCommand()),
                        source.getClusterNodes(),
                        source.getPassword(),
                        source.getAdditionalKey(),
                        source.getDatabase(),
                        source.getTimeout(),
                        source.getSoTimeout(),
                        source.getMaxTotal(),
                        source.getMaxIdle(),
                        source.getMinIdle(),
                        parseLookupOptions(source.getLookupOptions()));
            default:
                throw new IllegalArgumentException(String.format("Unsupported redis-mode=%s for Inlong", redisMode));
        }
    }

    /**
     * Parse LookupOptions
     *
     * @param options RedisLookupOptions
     * @return LookupOptions
     */
    private static LookupOptions parseLookupOptions(RedisLookupOptions options) {
        if (options == null) {
            return null;
        }
        return new LookupOptions(options.getLookupCacheMaxRows(), options.getLookupCacheTtl(),
                options.getLookupMaxRetries(), options.getLookupAsync());
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        RedisSink redisSink = (RedisSink) nodeInfo;
        Map<String, String> properties = parseProperties(redisSink.getProperties());
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(redisSink.getSinkFieldList(), redisSink.getSinkName());
        List<FieldRelation> fieldRelations = parseSinkFields(redisSink.getSinkFieldList(), constantFieldMap);

        String clusterMode = redisSink.getClusterMode();
        String dataType = redisSink.getDataType();
        String schemaMapMode = redisSink.getSchemaMapMode();
        String host = redisSink.getHost();
        Integer port = redisSink.getPort();
        String clusterNodes = redisSink.getClusterNodes();
        String masterName = redisSink.getMasterName();
        String sentinelsInfo = redisSink.getSentinelsInfo();
        Integer database = redisSink.getDatabase();
        String password = redisSink.getPassword();
        Integer ttl = redisSink.getTtl();
        Integer timeout = redisSink.getTimeout();
        Integer soTimeout = redisSink.getSoTimeout();
        Integer maxTotal = redisSink.getMaxTotal();
        Integer maxIdle = redisSink.getMaxIdle();
        Integer minIdle = redisSink.getMinIdle();
        Integer maxRetries = redisSink.getMaxRetries();

        Format format = parsingDataFormat(
                redisSink.getFormatDataType(),
                false,
                redisSink.getFormatDataSeparator(),
                false);

        return new RedisLoadNode(
                redisSink.getSinkName(),
                redisSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                clusterMode,
                dataType,
                schemaMapMode,
                host,
                port,
                clusterNodes,
                masterName,
                sentinelsInfo,
                database,
                password,
                ttl,
                format,
                timeout,
                soTimeout,
                maxTotal,
                maxIdle,
                minIdle,
                maxRetries);
    }

    /**
     * Parse format
     *
     * @param formatName data serialization, support: csv, json, canal, avro, etc
     * @param wrapWithInlongMsg whether wrap content with {@link InLongMsgFormat}
     * @param separatorStr the separator of data content
     * @param ignoreParseErrors whether ignore deserialization error data
     * @return the format for serialized content
     */
    private Format parsingDataFormat(
            String formatName,
            boolean wrapWithInlongMsg,
            String separatorStr,
            boolean ignoreParseErrors) {
        Format format;
        DataTypeEnum dataType = DataTypeEnum.forType(formatName);
        switch (dataType) {
            case CSV:
                if (StringUtils.isNumeric(separatorStr)) {
                    char dataSeparator = (char) Integer.parseInt(separatorStr);
                    separatorStr = Character.toString(dataSeparator);
                }
                CsvFormat csvFormat = new CsvFormat(separatorStr);
                csvFormat.setIgnoreParseErrors(ignoreParseErrors);
                format = csvFormat;
                break;
            case AVRO:
                format = new AvroFormat();
                break;
            case JSON:
                JsonFormat jsonFormat = new JsonFormat();
                jsonFormat.setIgnoreParseErrors(ignoreParseErrors);
                format = jsonFormat;
                break;
            case CANAL:
                format = new CanalJsonFormat();
                break;
            case DEBEZIUM_JSON:
                DebeziumJsonFormat debeziumJsonFormat = new DebeziumJsonFormat();
                debeziumJsonFormat.setIgnoreParseErrors(ignoreParseErrors);
                format = debeziumJsonFormat;
                break;
            case RAW:
                format = new RawFormat();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported dataType=%s", dataType));
        }
        if (wrapWithInlongMsg) {
            Format innerFormat = format;
            format = new InLongMsgFormat(innerFormat, false);
        }
        return format;
    }
}