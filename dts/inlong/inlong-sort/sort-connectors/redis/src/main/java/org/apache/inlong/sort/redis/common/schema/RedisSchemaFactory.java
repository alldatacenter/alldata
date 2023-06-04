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

package org.apache.inlong.sort.redis.common.schema;

import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.inlong.sort.redis.common.config.RedisDataType;
import org.apache.inlong.sort.redis.common.config.SchemaMappingMode;
import org.apache.inlong.sort.redis.common.schema.impl.BitmapStaticKvPairSchema;
import org.apache.inlong.sort.redis.common.schema.impl.HashDynamicSchema;
import org.apache.inlong.sort.redis.common.schema.impl.HashStaticKvPairSchema;
import org.apache.inlong.sort.redis.common.schema.impl.HashStaticPrefixMatchSchema;
import org.apache.inlong.sort.redis.common.schema.impl.PlainPrefixMatchSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The factory for creating RedisSchema.
 */
public class RedisSchemaFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RedisSchemaFactory.class);

    /**
     * Create a new {@link RedisSchema}
     */
    public static RedisSchema<?> createRedisSchema(
            RedisDataType dataType,
            SchemaMappingMode schemaMappingMode,
            ResolvedSchema resolvedSchema) {

        if (dataType == RedisDataType.BITMAP) {
            if (schemaMappingMode == SchemaMappingMode.STATIC_KV_PAIR) {
                return new BitmapStaticKvPairSchema(resolvedSchema);
            } else {
                String message = "Unsupported '" + schemaMappingMode + "' in Bitmap dataType currently !";
                LOG.error(message);
                throw new UnsupportedOperationException(message);
            }
        }

        if (dataType == RedisDataType.HASH) {
            switch (schemaMappingMode) {
                case DYNAMIC:
                    return new HashDynamicSchema(resolvedSchema);
                case STATIC_KV_PAIR:
                    return new HashStaticKvPairSchema(resolvedSchema);
                case STATIC_PREFIX_MATCH:
                    return new HashStaticPrefixMatchSchema(resolvedSchema);
                default:
                    String message = "Unsupported '" + schemaMappingMode + "' in HASH dataType currently !";
                    LOG.error(message);
                    throw new UnsupportedOperationException(message);
            }
        }

        if (dataType == RedisDataType.PLAIN) {
            if (schemaMappingMode == SchemaMappingMode.STATIC_PREFIX_MATCH) {
                return new PlainPrefixMatchSchema(resolvedSchema);
            } else {
                String message = "Unsupported '" + schemaMappingMode + "' in PLAIN dataType currently !";
                LOG.error(message);
                throw new UnsupportedOperationException(message);
            }
        }
        throw new UnsupportedOperationException("Unsupported dataType: '" + dataType + "' !");
    }
}
