package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.model.PermissionMetaDO;
import com.alibaba.tesla.authproxy.service.PermissionMetaService;
import com.alibaba.tesla.authproxy.service.PermissionResService;
import com.alibaba.tesla.authproxy.util.StringUtil;
import com.alibaba.tesla.authproxy.web.input.DeletePermissionMetaParam;
import com.alibaba.tesla.authproxy.web.input.DisablePermissionMetaParam;
import com.alibaba.tesla.authproxy.web.input.EnablePermissionMetaParam;
import com.alibaba.tesla.authproxy.web.input.InitAppPermissionMetaParam;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.*;
import java.util.Set;

/**
 * 权限元数据请求处理
 *
 * @author tandong.td@alibaba-inc.com
 */
@Controller
@RequestMapping("permission/meta")
public class PermissionMetaController extends BaseController {

    @Autowired
    PermissionMetaService permissionMetaService;

    @Autowired
    PermissionResService permissionResService;

    /**
     * 使用场景：应用管理中用户自定义菜单创建后，需要将自定菜单写入权限元数据，同时将权限初始化到acl中 创建并初始化应用的权限元数据
     *
     * @param initAppPermissionMetaParam
     * @param bindingResult
     * @return
     */
    @PostMapping("create-init")
    @ResponseBody
    public TeslaBaseResult createAndInit(@Valid @RequestBody InitAppPermissionMetaParam initAppPermissionMetaParam,
                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }

        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        Validator validator = vf.getValidator();
        Set<ConstraintViolation<PermissionMetaDO>> set = validator.validate(
            initAppPermissionMetaParam.getPermissionMetaDO());
        for (ConstraintViolation<PermissionMetaDO> constraintViolation : set) {
            return buildClientErrorResult("Param permissionMetaDO validate failed:" + constraintViolation.getMessage());
        }
        PermissionMetaDO old = permissionMetaService.selectOne(
            initAppPermissionMetaParam.getPermissionMetaDO().getPermissionCode());
        if (null != old) {
            
            return buildExceptionResult(
                new Exception("The same permission already exists, Please change service code or permission code."));
        }
        permissionMetaService.createAndInit(initAppPermissionMetaParam.getUserId(),
            initAppPermissionMetaParam.getAppId(), initAppPermissionMetaParam.getPermissionMetaDO());
        return buildSucceedResult(null);
    }

    /**
     * 删除权限元数据 使用场景：在应用管理中用户添加了自定义菜单之后进行删除，和create-init对应
     *
     * @param delParam 删除参数
     * @return
     */
    @PostMapping("delete")
    @ResponseBody
    public TeslaBaseResult delete(@Valid @RequestBody DeletePermissionMetaParam delParam, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        permissionMetaService.delete(delParam.getAppId(), delParam.getUserId(), delParam.getPermissionCode(),
            delParam.isDelAclOam());
        return buildSucceedResult(null);
    }

    /**
     * 添加一条权限元数据
     *
     * @return
     */
    @PostMapping("create")
    @ResponseBody
    public TeslaBaseResult create(@Valid @RequestBody PermissionMetaDO permissionMetaDO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        PermissionMetaDO old = permissionMetaService.selectOne(permissionMetaDO.getPermissionCode());
        if (null != old) {
            return buildExceptionResult(
                new Exception("The same permission already exists, Please change service code or permission code."));
        }
        int ret = permissionMetaService.insert(permissionMetaDO);
        return buildSucceedResult(ret);
    }

    /**
     * @param permissionMetaDO
     * @param bindingResult
     * @return
     */
    @PostMapping("update")
    @ResponseBody
    public TeslaBaseResult update(@Valid @RequestBody PermissionMetaDO permissionMetaDO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        PermissionMetaDO old = permissionMetaService.selectOne(permissionMetaDO.getPermissionCode());
        if (null == old) {
            return buildExceptionResult(
                new Exception("The same permission already exists, Please change service code or permission code."));
        }
        int ret = permissionMetaService.update(permissionMetaDO);
        return buildSucceedResult(ret);
    }

    /**
     * 开启某个应用的权限
     *
     * @param param
     * @param bindingResult
     * @return
     */
    @PostMapping("enable")
    @ResponseBody
    public TeslaBaseResult enable(@Valid @RequestBody EnablePermissionMetaParam param, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        String employeeId = getUserEmployeeId();
        if (StringUtil.isEmpty(employeeId)) {
            return buildClientErrorResult("request header params x-empid is empty.");
        }
        permissionResService.enable(employeeId, param.getAppId(), param.getServiceCode(), param.getPermissionMetaIds());
        return buildSucceedResult(null);
    }

    /**
     * 关闭某个应用的权限
     *
     * @param param
     * @param bindingResult
     * @return
     */
    @PostMapping("disable")
    @ResponseBody
    public TeslaBaseResult disable(@Valid @RequestBody DisablePermissionMetaParam param, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        String employeeId = getUserEmployeeId();
        if (StringUtil.isEmpty(employeeId)) {
            return buildClientErrorResult("request header params x-empid is empty.");
        }
        permissionResService.disable(employeeId, param.getAppId(), param.getServiceCode(),
            param.getPermissionMetaIds());
        return buildSucceedResult(null);
    }
}
