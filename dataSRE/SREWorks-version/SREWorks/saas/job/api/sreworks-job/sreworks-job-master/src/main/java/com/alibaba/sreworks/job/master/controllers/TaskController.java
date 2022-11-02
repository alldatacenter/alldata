package com.alibaba.sreworks.job.master.controllers;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJobTask;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobTaskDTO;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobTaskRepository;
import com.alibaba.sreworks.job.master.params.TaskCreateParam;
import com.alibaba.sreworks.job.master.params.TaskModifyParam;
import com.alibaba.sreworks.job.master.params.TaskStartParam;
import com.alibaba.sreworks.job.master.services.TaskService;
import com.alibaba.sreworks.job.taskinstance.TaskInstanceStatus;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.sreworks.job.utils.StringUtil;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/task")
public class TaskController extends BaseController {

    @Autowired
    TaskService taskService;

    @Autowired
    SreworksJobTaskRepository taskRepository;

    @RequestMapping(value = "listStatus", method = RequestMethod.GET)
    public TeslaBaseResult listStatus() {
        return buildSucceedResult(TaskInstanceStatus.values());
    }

    @RequestMapping(value = "listMeta", method = RequestMethod.GET)
    public TeslaBaseResult list(String sceneType) {
        sceneType = StringUtil.isEmpty(sceneType) ? "normal" : sceneType;
        List<SreworksJobTask> tasks = taskRepository.findAllBySceneType(sceneType);
        return buildSucceedResult(tasks.stream().map(task -> JsonUtil.map(
            "id", task.getId(),
            "name", task.getName(),
            "alias", task.getAlias(),
            "varConf", JSONObject.parse(task.getVarConf())
        )).collect(Collectors.toList()));

    }

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(Integer page, Integer pageSize, String name, String execType, String sceneType) {
        if (page == null) {
            page = 1;
        }
        if (pageSize == null) {
            pageSize = 100;
        }
        List<SreworksJobTask> tasks = taskService.list(name, execType, sceneType);
        return buildSucceedResult(JsonUtil.map(
            "total", tasks.size(),
            "items", tasks.subList((page - 1) * pageSize, Math.min(page * pageSize, tasks.size()))
        ));

    }

    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) {

        return buildSucceedResult(new SreworksJobTaskDTO(taskRepository.findFirstById(id)));

    }

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public TeslaBaseResult create(@RequestBody TaskCreateParam param) {

        param.setCreator(getUserEmployeeId());
        param.setOperator(getUserEmployeeId());
        param.setAppId(getAppId());
        SreworksJobTask task = taskRepository.saveAndFlush(param.task());
        return buildSucceedResult(task.getId());

    }

    @RequestMapping(value = "modify", method = RequestMethod.POST)
    public TeslaBaseResult modify(Long id, @RequestBody TaskModifyParam param) {

        param.setOperator(getUserEmployeeId());
        SreworksJobTask task = taskRepository.findFirstById(id);
        param.patchTask(task);
        taskRepository.saveAndFlush(task);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(Long id) {

        taskRepository.deleteById(id);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "start", method = RequestMethod.POST)
    public TeslaBaseResult start(Long id, @RequestBody TaskStartParam param) throws Exception {

        return buildSucceedResult(taskService.start(id, param.varConf(), getUserEmployeeIdRequired()));

    }

}
