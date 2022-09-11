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

package org.apache.inlong.manager.service.core.impl;

import org.apache.inlong.manager.common.pojo.group.DataSchemaInfo;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.DataSchemaEntity;
import org.apache.inlong.manager.dao.mapper.DataSchemaEntityMapper;
import org.apache.inlong.manager.service.core.DataSchemaService;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Data format service layer interface implementation class for inlong group
 */
@Service
@Slf4j
public class DataSchemaServiceImpl implements DataSchemaService {

    @Autowired
    private DataSchemaEntityMapper schemaMapper;

    @Override
    public List<DataSchemaInfo> listAllDataSchema() {
        List<DataSchemaEntity> entityList = schemaMapper.selectAll();
        return CommonBeanUtils.copyListProperties(entityList, DataSchemaInfo::new);
    }
}
