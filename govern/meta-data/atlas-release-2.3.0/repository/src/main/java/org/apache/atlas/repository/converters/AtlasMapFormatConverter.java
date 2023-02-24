/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.converters;


import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.type.AtlasArrayType;
import org.apache.atlas.type.AtlasMapType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AtlasMapFormatConverter extends AtlasAbstractFormatConverter {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasMapFormatConverter.class);


    public AtlasMapFormatConverter(AtlasFormatConverters registry, AtlasTypeRegistry typeRegistry) {
        super(registry, typeRegistry, TypeCategory.MAP);
    }

    @Override
    public boolean isValidValueV1(Object v1Obj, AtlasType type) {
        boolean ret = false;

        if (v1Obj == null) {
            return true;
        } if (type instanceof AtlasMapType && v1Obj instanceof Map) {
            AtlasMapType         mapType        = (AtlasMapType) type;
            AtlasType            keyType        = mapType.getKeyType();
            AtlasType            valueType      = mapType.getValueType();
            AtlasFormatConverter keyConverter   = null;
            AtlasFormatConverter valueConverter = null;
            Map                  v1Map          = (Map)v1Obj;

            try {
                keyConverter   = converterRegistry.getConverter(keyType.getTypeCategory());
                valueConverter = converterRegistry.getConverter(valueType.getTypeCategory());
            } catch (AtlasBaseException excp) {
                LOG.warn("failed to get key/value converter. type={}", type.getTypeName(), excp);

                ret = false;
            }

            if (keyConverter != null && valueConverter != null) {
                ret = true; // for empty map

                for (Object key : v1Map.keySet()) {
                    Object value = v1Map.get(key);

                    ret = keyConverter.isValidValueV1(key, keyType) && valueConverter.isValidValueV1(value, valueType);

                    if (!ret) {
                        break;
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("AtlasArrayFormatConverter.isValidValueV1(type={}, value={}): {}", (v1Obj != null ? v1Obj.getClass().getCanonicalName() : null), v1Obj, ret);
        }

        return ret;
    }

    @Override
    public Map fromV1ToV2(Object v1Obj, AtlasType type, ConverterContext ctx) throws AtlasBaseException {
        Map ret = null;

        if (v1Obj != null) {
            if (v1Obj instanceof Map) {
                AtlasMapType         mapType        = (AtlasMapType)type;
                AtlasType            keyType        = mapType.getKeyType();
                AtlasType            valueType      = mapType.getValueType();
                AtlasFormatConverter keyConverter   = converterRegistry.getConverter(keyType.getTypeCategory());
                AtlasFormatConverter valueConverter = converterRegistry.getConverter(valueType.getTypeCategory());
                Map                  v1Map          = (Map)v1Obj;

                ret = new HashMap<>();

                for (Object key : v1Map.keySet()) {
                    Object value = v1Map.get(key);

                    Object v2Key   = keyConverter.fromV1ToV2(key, keyType, ctx);
                    Object v2Value = valueConverter.fromV1ToV2(value, valueType, ctx);

                    ret.put(v2Key, v2Value);
                }
            } else {
                throw new AtlasBaseException(AtlasErrorCode.UNEXPECTED_TYPE, "Map", v1Obj.getClass().getCanonicalName());
            }

        }

        return ret;
    }

    @Override
    public Map fromV2ToV1(Object v2Obj, AtlasType type, ConverterContext ctx) throws AtlasBaseException {
        Map ret = null;

        if (v2Obj != null) {
            if (v2Obj instanceof Map) {
                AtlasMapType         mapType        = (AtlasMapType)type;
                AtlasType            keyType        = mapType.getKeyType();
                AtlasType            valueType      = mapType.getValueType();
                AtlasFormatConverter keyConverter   = converterRegistry.getConverter(keyType.getTypeCategory());
                AtlasFormatConverter valueConverter = converterRegistry.getConverter(valueType.getTypeCategory());
                Map                  v2Map          = (Map)v2Obj;

                ret = new HashMap<>();

                for (Object key : v2Map.keySet()) {
                    Object value = v2Map.get(key);

                    Object v2Key   = keyConverter.fromV2ToV1(key, keyType, ctx);
                    Object v2Value = valueConverter.fromV2ToV1(value, valueType, ctx);

                    ret.put(v2Key, v2Value);
                }
            } else {
                throw new AtlasBaseException(AtlasErrorCode.UNEXPECTED_TYPE, "Map", v2Obj.getClass().getCanonicalName());
            }
        }

        return ret;
    }
}

