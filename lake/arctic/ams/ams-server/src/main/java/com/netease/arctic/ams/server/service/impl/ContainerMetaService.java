/*
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

package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.ams.server.mapper.ContainerMetadataMapper;
import com.netease.arctic.ams.server.model.Container;
import com.netease.arctic.ams.server.service.IJDBCService;
import org.apache.ibatis.session.SqlSession;

import java.util.List;


public class ContainerMetaService extends IJDBCService {

  public List<Container> getContainers() {
    try (SqlSession sqlSession = getSqlSession(true)) {
      ContainerMetadataMapper containerMetadataMapper = getMapper(sqlSession, ContainerMetadataMapper.class);
      return containerMetadataMapper.getContainers();
    }
  }

  public Container getContainer(String name) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      ContainerMetadataMapper containerMetadataMapper = getMapper(sqlSession, ContainerMetadataMapper.class);
      return containerMetadataMapper.getContainer(name);
    }
  }

  public void insertContainer(Container container) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      ContainerMetadataMapper containerMetadataMapper = getMapper(sqlSession, ContainerMetadataMapper.class);
      containerMetadataMapper.insertContainer(container);
    }
  }

  public String getContainerType(String name) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      ContainerMetadataMapper containerMetadataMapper = getMapper(sqlSession, ContainerMetadataMapper.class);
      return containerMetadataMapper.getType(name);
    }
  }
}
