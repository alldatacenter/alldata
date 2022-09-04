package com.alibaba.tesla.tkgone.backend.controller;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.backend.controller.vo.BackendVO;
import com.alibaba.tesla.tkgone.backend.service.BackendService;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 存储后端配置Controller
 * @author xueyong.zxy
 */
@RestController
@RequestMapping("/backend")
public class BackendController extends BaseController {
    @Autowired
    BackendService backendService;

    @RequestMapping(value = "/type", method = RequestMethod.GET, name = "查询支持的后端存储集群类型列表")
    @ApiOperation(value = "/type", notes = "查询支持的后端存储集群类型列表")
    public TeslaBaseResult getBackendType() {
        return new TeslaBaseResult(BackendService.BACKEND_TYPES);
    }

    @RequestMapping(value = "", method = RequestMethod.POST, name = "添加后端存储集群")
    @ApiOperation(value = "", notes = "添加后端存储集群")
    public TeslaBaseResult addBackend(@RequestBody @ApiParam(value = "添加后端存储集群详细参数") BackendVO backendVO) {
//        return new TeslaBaseResult(backendService.addBackend(new BackendDTO(backendVO)));
        return null;
    }

    @RequestMapping(value = "", method = RequestMethod.GET, name = "查询后端存储集群列表")
    @ApiOperation(value = "", notes = "查询后端存储集群列表")
    public TeslaBaseResult listBackend(
            @RequestParam @ApiParam(value = "后端存储集群ID") String id,
            @RequestParam @ApiParam(value = "后端存储集群名称") String name) {
        return null;
    }
}
