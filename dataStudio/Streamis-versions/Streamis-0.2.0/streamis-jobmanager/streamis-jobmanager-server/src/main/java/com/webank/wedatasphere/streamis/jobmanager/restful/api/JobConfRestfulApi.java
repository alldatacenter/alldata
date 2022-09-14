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

import com.fasterxml.jackson.databind.JsonNode;
import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.JobConfDefinition;
import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.vo.JobConfDefinitionVo;
import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.vo.JobConfValueSet;
import com.webank.wedatasphere.streamis.jobmanager.launcher.service.StreamJobConfService;
import com.webank.wedatasphere.streamis.jobmanager.manager.conf.JobConf;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamJob;
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.JobErrorException;
import com.webank.wedatasphere.streamis.jobmanager.manager.project.service.ProjectPrivilegeService;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.StreamJobService;
import org.apache.linkis.httpclient.dws.DWSHttpClient;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping(path = "/streamis/streamJobManager/config")
@RestController
public class JobConfRestfulApi {

    private static final Logger LOG = LoggerFactory.getLogger(JobConfRestfulApi.class);

    @Resource
    private StreamJobConfService streamJobConfService;

    @Resource
    private StreamJobService streamJobService;

    @Resource
    private ProjectPrivilegeService privilegeService;

    /**
     * Definitions
     * @return message
     */
    @RequestMapping(value = "/definitions")
    public Message definitions(){
        Message result = Message.ok("success");
        try {
            List<JobConfDefinition> definitionList = this.streamJobConfService.loadAllDefinitions();
            Map<String, JobConfDefinitionVo> definitionRelation = new HashMap<>();
            definitionList.forEach(definition -> definitionRelation.put(String.valueOf(definition.getId()),
                    new JobConfDefinitionVo(definition)));
            definitionList.forEach(definition -> {
                Long parentRef = definition.getParentRef();
                if (Objects.nonNull(parentRef)) {
                    JobConfDefinitionVo definitionVo = definitionRelation.get(String.valueOf(parentRef));
                    if (Objects.nonNull(definitionVo)) {
                        List<JobConfDefinitionVo> childDef = Optional.ofNullable(definitionVo.getChildDef()).orElse(new ArrayList<>());
                        childDef.add(definitionRelation.get(String.valueOf(definition.getId())));
                        definitionVo.setChildDef(childDef);
                    }
                }
            });

            List<JobConfDefinitionVo> def =
                    definitionRelation.values().stream().filter(definitionVo -> definitionVo.getLevel() == 0)
                            .sorted((o1, o2) -> o2.getSort() - o1.getSort()).collect(Collectors.toList());
            def.forEach(definitionVo -> {
                if (Objects.isNull(definitionVo.getChildDef())){
                    definitionVo.setChildDef(Collections.emptyList());
                }
            });
            result.data("def", def);
        }catch(Exception e){
            String message = "Fail to obtain StreamJob configuration definitions(获取任务配置定义集失败), message: " + e.getMessage();
            LOG.warn(message, e);
            result = Message.error(message);
        }
        return result;
    }

    /**
     * Query job config json
     * @return config json
     */
    @RequestMapping(value = "/json/{jobId:\\w+}", method = RequestMethod.GET)
    public Message queryConfig(@PathVariable("jobId") Long jobId, HttpServletRequest request){
        Message result = Message.ok("success");
        try {
            String userName = SecurityFilter.getLoginUsername(request);
            StreamJob streamJob = this.streamJobService.getJobById(jobId);
            if (!streamJobService.hasPermission(streamJob, userName) &&
                    !this.privilegeService.hasAccessPrivilege(request, streamJob.getProjectName())){
                throw new JobErrorException(-1, "Have no permission to view StreamJob [" + jobId + "] configuration");
            }
            result.setData(new HashMap<>(this.streamJobConfService.getJobConfig(jobId)));
        }catch(Exception e){
            String message = "Fail to view StreamJob configuration(查看任务配置失败), message: " + e.getMessage();
            LOG.warn(message, e);
            result = Message.error(message);
        }
        return result;
    }

    /**
     * Save job config json
     * @param jobId job id
     * @param configContent config content
     * @param request request
     * @return
     */
    @RequestMapping(value = "/json/{jobId:\\w+}", method = RequestMethod.POST)
    public Message saveConfig(@PathVariable("jobId") Long jobId, @RequestBody Map<String, Object> configContent,
                              HttpServletRequest request){
        Message result = Message.ok("success");
        try{
            String userName = SecurityFilter.getLoginUsername(request);
            StreamJob streamJob = this.streamJobService.getJobById(jobId);
            // Accept the developer to modify
            if (!streamJobService.isCreator(jobId, userName) &&
                    !JobConf.STREAMIS_DEVELOPER().getValue().contains(userName) &&
                    !this.privilegeService.hasEditPrivilege(request, streamJob.getProjectName())) {
                throw new JobErrorException(-1, "Have no permission to save StreamJob [" + jobId + "] configuration");
            }
            this.streamJobConfService.saveJobConfig(jobId, configContent);
        }catch(Exception e){
           String message = "Fail to save StreamJob configuration(保存/更新任务配置失败), message: " + e.getMessage();
           LOG.warn(message, e);
           result = Message.error(message);
        }
        return result;
    }

    @RequestMapping(path = "/view", method = RequestMethod.GET)
    public Message viewConfigTree(@RequestParam(value = "jobId", required = false) Long jobId,
                              HttpServletRequest req){
        Message result = Message.ok("success");
        try{
            if (Objects.isNull(jobId)){
                throw new JobErrorException(-1, "Params 'jobId' cannot be empty");
            }
            String userName = SecurityFilter.getLoginUsername(req);
            StreamJob streamJob = this.streamJobService.getJobById(jobId);
            if (!this.streamJobService.hasPermission(streamJob, userName)
                    && !this.privilegeService.hasAccessPrivilege(req, streamJob.getProjectName())){
                throw new JobErrorException(-1, "Have no permission to view the configuration tree of StreamJob [" + jobId + "]");
            }
            result.data("fullTree", this.streamJobConfService.getJobConfValueSet(jobId));
        }catch (Exception e){
            String message = "Fail to view configuration tree(查看任务配置树失败), message: " + e.getMessage();
            LOG.warn(message, e);
            result = Message.error(message);
        }
        return result;
    }

    @RequestMapping(path = {"/add", "/update"}, method = RequestMethod.POST)
    public Message saveConfigTree(@RequestBody JsonNode json, HttpServletRequest req){
        Message result = Message.ok("success");
        try{
            String userName = SecurityFilter.getLoginUsername(req);
            JobConfValueSet fullTrees = DWSHttpClient.jacksonJson().readValue(json.get("fullTree").traverse(), JobConfValueSet.class);
            // Accept the developer to modify
            if (!streamJobService.isCreator(fullTrees.getJobId(), userName) &&
                    !JobConf.STREAMIS_DEVELOPER().getValue().contains(userName)) {
                return Message.error("you con not modify the config ,the job is not belong to you");
            }
            streamJobConfService.saveJobConfValueSet(fullTrees);
        }catch (Exception e){
            String message = "Fail to insert/update configuration tree(保存/更新任务配置树失败), message: " + e.getMessage();
            LOG.warn(message, e);
            result = Message.error(message);
        }
        return result;
    }
}
