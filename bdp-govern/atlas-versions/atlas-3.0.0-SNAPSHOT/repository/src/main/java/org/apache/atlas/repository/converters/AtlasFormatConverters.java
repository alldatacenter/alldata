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
import org.apache.atlas.type.AtlasTypeRegistry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
@Component
public class AtlasFormatConverters {

    private final Map<TypeCategory, AtlasFormatConverter> registry = new HashMap<>();

    @Inject
    public AtlasFormatConverters(AtlasTypeRegistry typeRegistry) {
        registerConverter(new AtlasPrimitiveFormatConverter(this, typeRegistry));
        registerConverter(new AtlasEnumFormatConverter(this, typeRegistry));
        registerConverter(new AtlasStructFormatConverter(this, typeRegistry));
        registerConverter(new AtlasClassificationFormatConverter(this, typeRegistry));
        registerConverter(new AtlasEntityFormatConverter(this, typeRegistry));
        registerConverter(new AtlasArrayFormatConverter(this, typeRegistry));
        registerConverter(new AtlasMapFormatConverter(this, typeRegistry));
        registerConverter(new AtlasObjectIdConverter(this, typeRegistry));
    }

    private void registerConverter(AtlasFormatConverter converter) {
        registry.put(converter.getTypeCategory(), converter);

        if (converter.getTypeCategory() == TypeCategory.ENTITY) {
            registry.put(TypeCategory.OBJECT_ID_TYPE, converter);
        }
    }

    public AtlasFormatConverter getConverter(TypeCategory typeCategory) throws AtlasBaseException {
        AtlasFormatConverter ret = registry.get(typeCategory);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.INTERNAL_ERROR,
                                         "Could not find the converter for this type " + typeCategory);
        }

        return ret;
    }
}
