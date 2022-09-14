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

import com.webank.wedatasphere.streamis.jobmanager.manager.util.CookieUtils;
import com.webank.wedatasphere.streamis.jobmanager.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;


@RequestMapping(path = "/streamis/streamJobManager/config")
@RestController
public class JobConfExtRestfulApi {
    private static final Logger LOG = LoggerFactory.getLogger(JobConfExtRestfulApi.class);

    @Autowired
    UserService userService;

    @RequestMapping(path = "/getWorkspaceUsers", method = RequestMethod.GET)
    public Message getWorkspaceUsers(HttpServletRequest req) {
        //获取工作空间
        List<String> userList = new ArrayList<>();
        String workspaceId = CookieUtils.getCookieWorkspaceId(req);
        if (StringUtils.isNotBlank(workspaceId)) {
            String userName = SecurityFilter.getLoginUsername(req);
            userList.addAll(userService.workspaceUserQuery(req, workspaceId));
        } else {
            LOG.warn("Cannot find the workspaceID from DSS，perhaps the cookie value has been lost in request from: {}", req.getLocalAddr());
        }
        return Message.ok().data("users", userList);
    }

}
