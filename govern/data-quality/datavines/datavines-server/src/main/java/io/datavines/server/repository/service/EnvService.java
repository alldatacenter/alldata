/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datavines.server.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.bo.env.EnvCreate;
import io.datavines.server.api.dto.bo.env.EnvUpdate;
import io.datavines.server.repository.entity.Env;

import java.util.List;

public interface EnvService extends IService<Env> {

    long create(EnvCreate envCreate) throws DataVinesServerException;

    int deleteById(long id);

    int update(EnvUpdate envUpdate) throws DataVinesServerException;

    Env getById(long id);

    List<Env> listByWorkspaceId(long workspaceId);
}
