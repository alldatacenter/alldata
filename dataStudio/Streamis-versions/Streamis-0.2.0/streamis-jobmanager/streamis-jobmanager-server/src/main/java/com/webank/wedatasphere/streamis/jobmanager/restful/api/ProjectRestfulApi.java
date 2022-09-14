/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.restful.api;

import com.webank.wedatasphere.streamis.jobmanager.exception.ProjectException;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.vo.TaskCoreNumVo;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.StreamJobService;
import org.apache.commons.lang.StringUtils;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RequestMapping(path = "/streamis/streamJobManager/project")
@RestController
public class ProjectRestfulApi {

    @Autowired
    private StreamJobService streamJobService;

    @RequestMapping(path = "/core/target", method = RequestMethod.GET)
    public Message getView(HttpServletRequest req, @RequestParam(value= "projectName",required = false) String projectName) throws ProjectException {
        if(StringUtils.isBlank(projectName)){
            throw new ProjectException("params cannot be empty!");
        }
        String username = SecurityFilter.getLoginUsername(req);
        TaskCoreNumVo taskCoreNumVO = streamJobService.countByCores(projectName,username);
        return Message.ok().data("taskCore",taskCoreNumVO);
    }
}
