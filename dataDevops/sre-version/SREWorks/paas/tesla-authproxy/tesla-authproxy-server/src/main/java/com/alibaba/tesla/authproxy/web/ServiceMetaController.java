package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.model.ServiceMetaDO;
import com.alibaba.tesla.authproxy.service.ServiceMetaService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import com.github.pagehelper.PageInfo;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tesla服务元数据请求处理
 *
 * @author tandong.td@alibaba-inc.com
 */
@Controller
@RequestMapping("service/meta")
public class ServiceMetaController extends BaseController {

    @Autowired
    ServiceMetaService serviceMetaService;

    /**
     * 查询某个应用下所有已经开启的服务元数据（含部分开启）
     *
     * @param appId
     * @return
     */
    @GetMapping("grant/list")
    @ResponseBody
    public TeslaBaseResult listGrant(@RequestParam @NotBlank String appId) {
        List<ServiceMetaDO> serviceMetaDOList = serviceMetaService.selectWithGrant(appId);
        return buildSucceedResult(serviceMetaDOList);
    }

    /**
     * 分页查询所有的TESLA服务元数据
     *
     * @param page
     * @param size
     * @return
     */
    @GetMapping("list")
    @ResponseBody
    public TeslaBaseResult list(@RequestParam @NotBlank String appId, @RequestParam int page, @RequestParam int size) {
        PageInfo<ServiceMetaDO> permissionMeta = serviceMetaService.select(appId, page, size);
        Map<String, Object> ret = new HashMap<>();
        ret.put("list", permissionMeta.getList());
        ret.put("total", permissionMeta.getTotal());
        return buildSucceedResult(ret);
    }

    @PostMapping("create")
    @ResponseBody
    public TeslaBaseResult create(@Valid @RequestBody ServiceMetaDO serviceMetaDO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        ServiceMetaDO old = serviceMetaService.selectOne(serviceMetaDO.getServiceCode());
        if (null != old) {
            return buildExceptionResult(
                new Exception("The same serviceMetaDO already exists, Please change service code."));
        }
        int ret = serviceMetaService.insert(serviceMetaDO);
        return buildSucceedResult(ret);
    }

    /**
     * @param serviceMetaDO
     * @param bindingResult
     * @return
     */
    @PostMapping("update")
    @ResponseBody
    public TeslaBaseResult update(@Valid @RequestBody ServiceMetaDO serviceMetaDO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        ServiceMetaDO old = serviceMetaService.selectOne(serviceMetaDO.getServiceCode());
        if (null == old) {
            return buildExceptionResult(
                new Exception("Cannot find serviceMetaDO, Please change service code."));
        }
        int ret = serviceMetaService.update(serviceMetaDO);
        return buildSucceedResult(ret);
    }

    /**
     * @param id
     * @return
     */
    @DeleteMapping("delete")
    @ResponseBody
    public TeslaBaseResult delete(@RequestParam long id) {
        int ret = serviceMetaService.delete(id);
        return buildSucceedResult(ret);
    }
}
