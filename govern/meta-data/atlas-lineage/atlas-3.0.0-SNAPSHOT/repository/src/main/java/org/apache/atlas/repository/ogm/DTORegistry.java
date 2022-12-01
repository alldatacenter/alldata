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
package org.apache.atlas.repository.ogm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class DTORegistry {
    private static final Logger LOG = LoggerFactory.getLogger(DTORegistry.class);

    private final Map<Class, DataTransferObject> typeDTOMap = new HashMap<>();

    @Inject
    public DTORegistry(Set<DataTransferObject> availableDTOs) {
        for (DataTransferObject availableDTO : availableDTOs) {
            LOG.info("Registering DTO: {}", availableDTO.getClass().getSimpleName());
            registerDTO(availableDTO);
        }
    }

    public <T extends DataTransferObject> DataTransferObject get(Class t) {
        return typeDTOMap.get(t);
    }

    private void registerDTO(DataTransferObject dto) {
        typeDTOMap.put(dto.getObjectType(), dto);
    }
}
