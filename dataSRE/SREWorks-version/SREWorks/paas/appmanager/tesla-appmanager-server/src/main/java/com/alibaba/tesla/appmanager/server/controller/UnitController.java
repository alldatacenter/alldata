package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.UnitProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.UnitDTO;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitCreateReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitDeleteReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitQueryReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitUpdateReq;
import com.alibaba.tesla.appmanager.server.service.unit.UnitService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

/**
 * Unit 管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/units")
@RestController
public class UnitController extends AppManagerBaseController {

    @Autowired
    private UnitService unitService;

    @Autowired
    private UnitProvider unitProvider;

    @RequestMapping("{unitId}/proxy/**")
    public void proxy(
            @PathVariable String unitId, HttpServletRequest request, HttpServletResponse response,
            OAuth2Authentication auth) throws Exception {
        String method = request.getMethod();
        String requestUri = request.getRequestURI().substring(String.format("/units/%s/proxy/", unitId).length());
        unitService.proxy(unitId, method, requestUri, request, response);
    }

    @PostMapping("{unitId}/apps/{appId}/app-packages/{appPackageId}/sync")
    public TeslaBaseResult sync(
            @PathVariable String unitId,
            @PathVariable String appId,
            @PathVariable Long appPackageId,
            OAuth2Authentication auth) throws Exception {
        return buildSucceedResult(unitProvider.syncRemote(unitId, appPackageId));
    }

    @Deprecated
    @GetMapping("/{unitId}/deployments")
    public void listDeployment(
            @PathVariable String unitId, HttpServletRequest request, HttpServletResponse response,
            OAuth2Authentication auth) throws Exception {
        String requestUri = request.getRequestURI().substring(String.format("/units/%s/", unitId).length());
        unitService.proxy(unitId, "GET", requestUri, request, response);
    }

    @Deprecated
    @PostMapping("/{unitId}/deployments/launch")
    public void launchDeployment(
            @PathVariable String unitId, HttpServletRequest request, HttpServletResponse response,
            OAuth2Authentication auth) throws Exception {
        String requestUri = request.getRequestURI().substring(String.format("/units/%s/", unitId).length());
        unitService.proxy(unitId, "POST", requestUri, request, response);
    }

    @Deprecated
    @GetMapping("/{unitId}/deployments/{deployAppId}")
    public void getDeployment(
            @PathVariable String unitId,
            @PathVariable Long deployAppId,
            HttpServletRequest request, HttpServletResponse response,
            OAuth2Authentication auth) throws Exception {
        String requestUri = request.getRequestURI().substring(String.format("/units/%s/", unitId).length());
        unitService.proxy(unitId, "GET", requestUri, request, response);
    }

    @Deprecated
    @GetMapping("/{unitId}/stages")
    public void listStage(
            @PathVariable String unitId,
            HttpServletRequest request, HttpServletResponse response,
            OAuth2Authentication auth) throws Exception {
        String requestUri = request.getRequestURI().substring(String.format("/units/%s/", unitId).length());
        unitService.proxy(unitId, "GET", requestUri, request, response);
    }

    @Deprecated
    @GetMapping("/{unitId}/apps/{appId}/app-packages")
    public void listAppPackage(
            @PathVariable String unitId,
            @PathVariable String appId,
            HttpServletRequest request, HttpServletResponse response,
            OAuth2Authentication auth) throws Exception {
        String requestUri = request.getRequestURI().substring(String.format("/units/%s/", unitId).length());
        unitService.proxy(unitId, "GET", requestUri, request, response);
    }

    @GetMapping
    public TeslaBaseResult queryByCondition(@Valid @ModelAttribute UnitQueryReq request, OAuth2Authentication auth) {
        Pagination<UnitDTO> units = unitProvider.queryByCondition(request);
        String initValue = "";
        if (units.getItems().size() > 0) {
            initValue = units.getItems().get(0).getUnitId();
        }
        JSONObject response = JSONObject.parseObject(JSONObject.toJSONString(units));
        response.put("initValue", initValue);
        return buildSucceedResult(response);
    }

    @PostMapping
    public TeslaBaseResult create(@Valid @RequestBody UnitCreateReq request, OAuth2Authentication auth) {
        return buildSucceedResult(unitProvider.create(request, getOperator(auth)));
    }

    @GetMapping("{unitId}")
    public TeslaBaseResult get(@PathVariable String unitId, OAuth2Authentication auth) {
        UnitDTO result = unitProvider.get(UnitQueryReq.builder().unitId(unitId).build());
        if (result == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot find specified unit");
        }
        return buildSucceedResult(result);
    }

    @PutMapping("{unitId}")
    public TeslaBaseResult update(
            @PathVariable("unitId") @NotEmpty String unitId,
            @Valid @RequestBody UnitUpdateReq request, OAuth2Authentication auth) {
        request.setUnitId(unitId);
        return buildSucceedResult(unitProvider.update(request, getOperator(auth)));
    }

    @DeleteMapping("{unitId}")
    public TeslaBaseResult delete(
            @PathVariable("unitId") @NotEmpty String unitId, OAuth2Authentication auth) {
        UnitDeleteReq request = UnitDeleteReq.builder()
                .unitId(unitId)
                .build();
        unitProvider.delete(request, getOperator(auth));
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }
}
