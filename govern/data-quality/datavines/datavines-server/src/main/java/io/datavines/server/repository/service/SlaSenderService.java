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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import io.datavines.server.api.dto.bo.sla.SlaSenderCreate;
import io.datavines.server.api.dto.bo.sla.SlaSenderUpdate;
import io.datavines.server.api.dto.vo.SlaSenderVO;
import io.datavines.server.repository.entity.SlaSender;

import java.util.List;

public interface SlaSenderService extends IService<SlaSender> {

    IPage<SlaSenderVO> pageListSender(Long workspaceId, String searchVal, Integer pageNumber, Integer pageSize);

    List<SlaSenderVO> listSenders(Long workspaceId, String searchVal, String type);

    SlaSender createSender(SlaSenderCreate create);

    boolean updateSender(SlaSenderUpdate update);
}
