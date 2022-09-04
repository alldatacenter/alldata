package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.service.UserDepartmentService;
import com.alibaba.tesla.authproxy.service.ao.UserDepartmentListResultAO;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 部门信息 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@Api(tags = "用户部门 API", description = "用户部门 API")
@RequestMapping("users/{userId:[a-zA-Z0-9,._:\\-|]+}")
@Validated
@Slf4j
public class UserDepartmentController extends BaseController {

    @Autowired
    private UserDepartmentService userDepartmentService;

    @GetMapping("departments")
    @ApiOperation(value = "获取指定用户对应的所有下属对应的部门 ID 集合")
    @ResponseBody
    public TeslaBaseResult retrieve(
        @ApiParam("当前用户")
        @NotEmpty
        @RequestHeader(value = "X-EmpId")
            String empId,
        @ApiParam("实际查询的用户 ID")
        @PathVariable
            String userId) {
        UserDepartmentListResultAO data = userDepartmentService.getDepIdMapBySupervisorUser(userId);
        return TeslaResultFactory.buildSucceedResult(data);
    }
}
