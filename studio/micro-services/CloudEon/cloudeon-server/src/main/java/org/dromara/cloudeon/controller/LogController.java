package org.dromara.cloudeon.controller;

import cn.hutool.core.io.FileUtil;
import org.dromara.cloudeon.dao.CommandTaskRepository;
import org.dromara.cloudeon.dto.ResultDTO;
import org.dromara.cloudeon.entity.CommandTaskEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;

@RestController
@RequestMapping("/log")
public class LogController {

    @Resource
    private CommandTaskRepository commandTaskRepository;

    @GetMapping("/task")
    public ResultDTO<String> commandTaskLog(Integer commandTaskId) {
        CommandTaskEntity commandTaskEntity = commandTaskRepository.findById(commandTaskId).get();
        String taskLogPath = commandTaskEntity.getTaskLogPath();
        String result = FileUtil.readUtf8String(new File(taskLogPath));
        return ResultDTO.success(result);
    }
}
