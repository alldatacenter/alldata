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

package org.apache.inlong.manager.common.fieldtype;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * data field type mapping file reader
 */
@Slf4j
public class FieldTypeMappingReader implements Serializable {

    /**
     * Field type mapping converter template file name.
     */
    private static final String FILE_TYPE_MAPPING_CONVERTER_TEMPLATE_NAME = "%s-field-type-mapping.yaml";

    /**
     * Source type to target type key in converter file.
     */
    private static final String SOURCE_TO_TARGET_KEY = "source.type.to.target.type.converter";

    /**
     * Field type mapping source type key
     */
    private static final String MAPPING_SOURCE_TYPE_KEY = "source.type";

    /**
     * Field type mapping target type key
     */
    private static final String MAPPING_TARGET_TYPE_KEY = "target.type";

    @Getter
    protected final String streamType;
    @Getter
    protected final Map<String, String> FIELD_TYPE_MAPPING_MAP = Maps.newHashMap();

    public FieldTypeMappingReader(String streamType) {
        this.streamType = streamType;
        log.info("Field type mapping reader stream type:{}.", streamType);
        String converterMappingFileName = String
                .format(FILE_TYPE_MAPPING_CONVERTER_TEMPLATE_NAME, streamType.toLowerCase());
        try {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(converterMappingFileName);
            if (Objects.isNull(resource)) {
                throw new IllegalArgumentException(
                        String.format("Resource for the field type mapping converter %s not found in classpath.",
                                converterMappingFileName));
            }
            Yaml yamlReader = new Yaml();
            Map<?, ?> converterConf = yamlReader.loadAs(new InputStreamReader(
                    resource.openStream()), Map.class);
            readerOption(converterConf, SOURCE_TO_TARGET_KEY, FIELD_TYPE_MAPPING_MAP);
        } catch (Exception e) {
            log.error("Yaml reader read option error", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * reader option from converter file
     *
     * @param converterConf converterConf
     * @param key key in converter file
     * @param typeMapping type mapping map
     */
    @SuppressWarnings("unchecked")
    private void readerOption(Map<?, ?> converterConf, String key, Map<String, String> typeMapping) {
        if (converterConf.containsKey(key)) {
            List<Map<String, String>> typeMappings = (List<Map<String, String>>) converterConf.get(key);
            for (Map<String, String> mapping : typeMappings) {
                String sourceType = mapping.get(MAPPING_SOURCE_TYPE_KEY);
                String targetType = mapping.get(MAPPING_TARGET_TYPE_KEY);
                if (StringUtils.isBlank(sourceType) || StringUtils.isBlank(targetType)) {
                    throw new IllegalArgumentException(
                            String.format("Reader source type: %s, target type: %s are not valid.",
                                    sourceType, targetType));
                }
                typeMapping.put(sourceType, targetType);
            }
        } else {
            log.warn("Converter field type mapping conf can't find key: {} in converter file type {}.", key,
                    streamType);
        }
    }
}
