package com.platform.devops.controller;

import com.platform.devops.entity.Machine;
import com.platform.devops.entity.Project;
import com.platform.devops.service.MachineService;
import com.platform.devops.service.ProjectService;
import com.platform.mall.entity.Result;
import com.platform.mall.utils.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author wlhbdp
 * @desc Devops--机器管理Controller
 */
@RestController
@Api(description = "机器管理")
public class MachineController {

    @Autowired
    private MachineService machineService;

    @RequestMapping(value = "/getAllMachine", method = RequestMethod.GET)
    @ApiOperation(value = "获取项目列表")
    public Result<Object> getAllMachine() {

        List<Machine> machines = machineService.getAllMachine();
        return new ResultUtil<Object>().setData(machines);
    }
}
