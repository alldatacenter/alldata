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
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AtlasArrayFormatConverter extends AtlasAbstractFormatConverter {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasArrayFormatConverter.class);

    public AtlasArrayFormatConverter(AtlasFormatConverters registry, AtlasTypeRegistry typeRegistry) {
        super(registry, typeRegistry, TypeCategory.ARRAY);
    }

    @Override
    public boolean isValidValueV1(Object v1Obj, AtlasType type) {
        boolean ret = false;

        if (v1Obj == null) {
            return true;
        } if (type instanceof AtlasArrayType) {
            AtlasArrayType       arrType       = (AtlasArrayType) type;
            AtlasType            elemType      = arrType.getElementType();
            AtlasFormatConverter elemConverter = null;

            try {
                elemConverter = converterRegistry.getConverter(elemType.getTypeCategory());
            } catch (AtlasBaseException excp) {
                LOG.warn("failed to get element converter. type={}", type.getTypeName(), excp);

                ret = false;
            }

            if (elemConverter != null) {
                if (v1Obj instanceof Collection) {
                    ret = true; // for empty array

                    for (Object v1Elem : (Collection) v1Obj) {
                        ret = elemConverter.isValidValueV1(v1Elem, elemType);

                        if (!ret) {
                            break;
                        }
                    }
                } else {
                    ret = elemConverter.isValidValueV1(v1Obj, elemType);
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("AtlasArrayFormatConverter.isValidValueV1(type={}, value={}): {}", (v1Obj != null ? v1Obj.getClass().getCanonicalName() : null), v1Obj, ret);
        }

        return ret;
    }

    @Override
    public Collection fromV1ToV2(Object v1Obj, AtlasType type, ConverterContext ctx) throws AtlasBaseException {
        Collection ret = null;

        if (v1Obj != null) {
            if (v1Obj instanceof Set) {
                ret = new LinkedHashSet();
            } else {
                ret = new ArrayList();
            }

            AtlasArrayType       arrType       = (AtlasArrayType) type;
            AtlasType            elemType      = arrType.getElementType();
            AtlasFormatConverter elemConverter = converterRegistry.getConverter(elemType.getTypeCategory());

            if (v1Obj instanceof Collection) {
                Collection v1List = (Collection) v1Obj;

                for (Object v1Elem : v1List) {
                    Object convertedVal = elemConverter.fromV1ToV2(v1Elem, elemType, ctx);

                    ret.add(convertedVal);
                }
            } else {
                Object convertedVal = elemConverter.fromV1ToV2(v1Obj, elemType, ctx);

                ret.add(convertedVal);
            }
        }

        return ret;
    }

    @Override
    public Collection fromV2ToV1(Object v2Obj, AtlasType type, ConverterContext ctx) throws AtlasBaseException {
        Collection ret = null;

        if (v2Obj != null) {
            if (v2Obj instanceof List) {
                ret = new ArrayList();
            } else if (v2Obj instanceof Set) {
                ret = new LinkedHashSet();
            } else {
                throw new AtlasBaseException(AtlasErrorCode.UNEXPECTED_TYPE, "List or Set",
                                             v2Obj.getClass().getCanonicalName());
            }

            AtlasArrayType       arrType       = (AtlasArrayType) type;
            AtlasType            elemType      = arrType.getElementType();
            AtlasFormatConverter elemConverter = converterRegistry.getConverter(elemType.getTypeCategory());
            Collection           v2List        = (Collection) v2Obj;

            for (Object v2Elem : v2List) {
                Object convertedVal = elemConverter.fromV2ToV1(v2Elem, elemType, ctx);

                ret.add(convertedVal);
            }
        }

        return ret;
    }
}

