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
import io.datavines.server.api.dto.bo.sla.SlaCreate;
import io.datavines.server.api.dto.bo.sla.SlaUpdate;
import io.datavines.server.api.dto.vo.SlaConfigVO;
import io.datavines.server.api.dto.vo.SlaPageVO;
import io.datavines.server.api.dto.vo.SlaVO;
import io.datavines.server.repository.entity.Sla;

import java.util.List;
import java.util.Set;

public interface SlaService extends IService<Sla> {

    IPage<SlaPageVO> listSlas(Long workspaceId, String searchVal, Integer pageNumber, Integer pageSize);

    boolean deleteById(Long id);

    String getSenderConfigJson(String type);

    Set<String> getSupportPlugin();

    Sla createSla(SlaCreate create);

    boolean updateSla(SlaUpdate update);

    List<SlaVO> getSlaByJobId(Long jobId);

    List<SlaConfigVO> getSlaConfigByJobId(Long jobId);
}
