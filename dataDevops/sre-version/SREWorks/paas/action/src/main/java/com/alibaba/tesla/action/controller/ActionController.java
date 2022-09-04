package com.alibaba.tesla.action.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.common.TeslaBaseResult;
import com.alibaba.tesla.action.params.ActionParam;
import com.alibaba.tesla.action.params.QueryParam;
import com.alibaba.tesla.action.params.SaveActionParam;
import com.alibaba.tesla.action.service.ActionService;
import com.alibaba.tesla.action.service.impl.ActionStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static com.alibaba.tesla.action.common.TeslaResultFactory.buildResult;
import static com.alibaba.tesla.action.common.TeslaResultFactory.buildSucceedResult;


/**
 * @program: tesla-frontend-service
 * @description:
 * @author: huibang.lhb
 * @create: 2019-03-27 11:34
 **/

@RestController
@RequestMapping("action")
@Slf4j
public class ActionController {

    @Autowired
    ActionService actionService;

    @Autowired
    ActionStatusService actionStatusService;

    @Autowired
    SaveActionParam saveActionParam;

    @RequestMapping(value = "/run", method = {RequestMethod.POST})
    public TeslaBaseResult runAction (
        @Validated @RequestBody ActionParam request, @RequestHeader(value = "X-Empid", defaultValue = "169715", required = false) String userEmpid,
        @RequestHeader(value="X-Env", defaultValue = "prod", required = false) String env,
        @RequestHeader(value = "X-Biz-App") String appCode,
        @RequestHeader(value = "X-Run-As-Role", defaultValue = "default", required = false) String role,
        @RequestHeader(value = "X-Biz-Tenant", required = false) String tenant,
        HttpServletRequest httpServletRequest
    ) {

        request.setEmpId(userEmpid);
        request.setAppCode(appCode);
        request.setCreateTime(System.currentTimeMillis());
        request.setRole(role);
        log.info("-------action enter api headers env: {}", httpServletRequest.getHeader("x-env"));
        log.info("-------action enter x-env: {}", env);
        log.info("action run request: {}", JSONObject.toJSONString(request));
        Cookie[] cookies = httpServletRequest.getCookies();
        Object result = actionService.callAction(request, env, cookies, tenant);
        log.info("[Action-call-result]: {}, acitonType: {}", result, request.getActionType());
        if (request.getActionType().equals("API")) {
            try {
                String jsonString = JSONObject.toJSONString(result);
                JSONObject jsonResult = JSONObject.parseObject(jsonString);
                return buildResult(jsonResult.getInteger("code"), jsonResult.getString("message"),
                    jsonResult.get("data"));
            } catch (Exception e) {
                log.error("[API-RESULT] convert error, errmsg: {}, data: {}", e.getMessage(), result);
                return buildSucceedResult(result);
            }
        }

        return buildSucceedResult(result);

    }

    @RequestMapping(value = "/save", method = {RequestMethod.POST})
    public TeslaBaseResult saveData (@Validated @RequestBody SaveActionParam data,
        @RequestHeader(value = "X-Biz-App") String appCode,
        @RequestHeader(value = "X-Run-As-Role", defaultValue = "default", required = false) String role
        ) {
        data.setAppCode(appCode);
        data.setRole(role);
        actionService.saveAction(data);
        return buildSucceedResult("success");
    }


    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    public TeslaBaseResult getList (
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityObject,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String elementId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String node,
            @RequestParam(required = false) String empId,
            @RequestParam(required = false) Long actionId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestHeader(value = "X-Biz-App") String appCode
    ) {
        QueryParam queryParam = new QueryParam();
        if (entityType != null) {
            queryParam.setEntityType(entityType);
        }
        if (entityObject != null) {
            queryParam.setEntityValue(entityObject);
        }
        if (actionType != null) {
            queryParam.setActionType(actionType);
        }
        if (actionId != null) {
            queryParam.setActionId(actionId);
        }
        if (elementId != null) {
            queryParam.setElementId(elementId);
        }
        if (status != null) {
            queryParam.setStatus(status);
        }
        if (node != null) {
            queryParam.setNode(node);
        }
        if (appCode != null) {
            queryParam.setAppCode(appCode);
        }
        if (empId != null) {
            queryParam.setEmpld(empId);
        }
        return buildSucceedResult(actionService.getActionSimpleList(queryParam));
    }

    @RequestMapping(value = "/count", method = {RequestMethod.GET})
    public TeslaBaseResult getCount (
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String entityObject,
        @RequestParam(required = false) String actionType,
        @RequestParam(required = false) String elementId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String node,
        @RequestParam(required = false) String empId,
        @RequestParam(required = false) Long actionId,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "20") Integer pageSize,
        @RequestHeader(value = "X-Biz-App") String appCode
    ) {
        QueryParam queryParam = new QueryParam();
        if (entityType != null) {
            queryParam.setEntityType(entityType);
        }
        if (entityObject != null) {
            queryParam.setEntityValue(entityObject);
        }
        if (actionType != null) {
            queryParam.setActionType(actionType);
        }
        if (actionId != null) {
            queryParam.setActionId(actionId);
        }
        if (elementId != null) {
            queryParam.setElementId(elementId);
        }
        if (status != null) {
            queryParam.setStatus(status);
        }
        if (node != null) {
            queryParam.setNode(node);
        }
        if (appCode != null) {
            queryParam.setAppCode(appCode);
        }
        if (empId != null) {
            queryParam.setEmpld(empId);
        }
        return buildSucceedResult(actionService.getCount(queryParam));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.GET})
    public TeslaBaseResult queryOne (@PathVariable(name = "id") Long id) {
        return buildSucceedResult(actionService.getAction(id));
    }

    @RequestMapping(value = "/status/{id}", method = {RequestMethod.POST})
    public TeslaBaseResult updateStatus (@PathVariable(name = "id") Long id, @Validated @RequestBody JSONObject data) {
        String status = data.getString("status");
        return buildSucceedResult(actionService.updateStatus(id, status));
    }

    @RequestMapping(value = "/status", method = {RequestMethod.GET})
    public TeslaBaseResult updateStatus() {
        actionStatusService.updateStatus();
        return buildSucceedResult("success");
    }

    @RequestMapping(value = "/bpms_update", method = {RequestMethod.GET})
    public TeslaBaseResult updateBpms() {
        actionService.checkBpmsStatus();
        return buildSucceedResult("success");
    }

    @RequestMapping(value = "flowTest", method =  {RequestMethod.GET})
    public TeslaBaseResult test() {
        log.info("1--");
        actionStatusService.flowTest("38c3d512-57d5-11ea-995d-00163e037daf");
        return buildSucceedResult("success");
    }
}
