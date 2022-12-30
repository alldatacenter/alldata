package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.model.PermissionMetaDO;
import com.alibaba.tesla.authproxy.model.PermissionResDO;
import com.alibaba.tesla.authproxy.service.PermissionMetaService;
import com.alibaba.tesla.authproxy.service.PermissionResService;
import com.alibaba.tesla.authproxy.web.input.InitAppPermissionParam;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import com.github.pagehelper.PageInfo;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Title: AuthProxyPermissionController.java<／p>
 * <p>Description: 权限认证请求处理Controller，主要提供了验权、查询权限数据等接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Controller
@RequestMapping("permissionres")
public class PermissionResController extends BaseController {

    /**
     * 权限资源服务接口
     */
    @Autowired
    PermissionResService permissionResService;

    @Autowired
    PermissionMetaService permissionMetaService;

    @Autowired
    UserMapper userMapper;

    /**
     * 分页查询应用已经开通的服务下的权限元数据
     * @param appId 应用标识
     * @param serviceCode 服务标识
     * @param page
     * @param size
     * @return
     */
    @GetMapping("list")
    @ResponseBody
    public TeslaBaseResult list(@RequestParam @NotBlank String appId, @RequestParam @NotBlank String serviceCode, @RequestParam int page, @RequestParam int size) {
        PageInfo<PermissionMetaDO> permissionMeta = permissionMetaService.select(appId, serviceCode, page, size);
        Map<String, Object> ret = new HashMap<>();
        ret.put("list", permissionMeta.getList());
        ret.put("total", permissionMeta.getTotal());
        return buildSucceedResult(ret);
    }

    /**
     * 添加权限资源关系
     * @param permissionResDo
     * @return
     */
    @RequestMapping(value = "/insert", method = RequestMethod.POST)
    @ResponseBody
    public TeslaBaseResult insert(@RequestBody PermissionResDO permissionResDo) {
        int ret = permissionResService.insert(permissionResDo);
        return buildSucceedResult(ret);
    }

    /**
     * 更新
     *
     * @param request
     * @param permissionResDo
     * @return
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public TeslaBaseResult update(HttpServletRequest request, @RequestBody PermissionResDO permissionResDo) {
        int ret = permissionResService.update(permissionResDo);
        return buildSucceedResult(ret);
    }

    /**
     * 删除指定ID的资源权限
     * @param id
     * @return
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public TeslaBaseResult delete(@RequestParam long id) {
        int ret = permissionResService.delete(id);
        return buildSucceedResult(ret);
    }

    /**
     * 批量添加权限资源关系
     * @param permissionResDOS
     * @return
     */
    @RequestMapping(value = "/batchInsert", method = RequestMethod.GET)
    @ResponseBody
    public TeslaBaseResult batchInsert(@RequestBody List<PermissionResDO> permissionResDOS) {
        int ret = permissionResService.batchInsert(permissionResDOS);
        return buildSucceedResult(ret);
    }

    /**
     * 初始化应用的权限资源
     * @param initAppPermissionParam
     * @param bindingResult
     * @return
     */
    @PostMapping(value = "/init")
    @ResponseBody
    public TeslaBaseResult initPermission(@Valid @RequestBody InitAppPermissionParam initAppPermissionParam, BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        permissionResService.init(initAppPermissionParam.getUserId(), initAppPermissionParam.getAppId(), initAppPermissionParam.getAccessKey());
        return buildSucceedResult(null);
    }
}
