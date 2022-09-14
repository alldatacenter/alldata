package com.alibaba.tesla.action.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.dao.ActionDAO;
import com.alibaba.tesla.action.dao.ActionDO;
import com.alibaba.tesla.action.dao.ActionExample;
import com.alibaba.tesla.action.dao.ActionExample.OrderCondition;
import com.alibaba.tesla.action.dao.ActionExample.SortType;
import com.alibaba.tesla.action.params.ActionParam;
import com.alibaba.tesla.action.params.QueryParam;
import com.alibaba.tesla.action.params.SaveActionParam;
import com.alibaba.tesla.action.service.ActionService;
import com.alibaba.tesla.action.service.impl.ActionStatusService.Status;
import com.alibaba.tesla.action.utils.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import java.util.*;

@Slf4j
@Service
public class ActionServiceImp implements ActionService {

    @Autowired
    ActionStatusService actionStatusService;

    @Autowired
    ActionDAO actionDAO;

    @Value("${tesla.abm.url}")
    private String AbmUrl;

    @Value("${env.new.api.prefix}")
    private String apiUrl;


    private ActionParam convertData(ActionDO actionDo, String type) {
        try {
            ActionParam actionParam = new ActionParam();
            if (actionDo == null) {
                return null;
            }
            ClassUtil.copy(actionParam, actionDo);
            if (actionDo.getExecData() != null) {
                actionParam.setInstanceId(JSONObject.parseObject(actionDo.getExecData()).getString("instanceId"));
                actionParam.setInstanceUrl(JSONObject.parseObject(actionDo.getExecData()).getString("instanceUrl"));
            }
            if (type.equals("all")) {
                actionParam.setActionMeta(JSONObject.parseObject(actionDo.getActionMetaData()));
            }
            actionParam.setProcessorInfo(JSONObject.parseObject(actionDo.getProcessor()));
            if (actionDo.getExecData() !=null) {
                actionParam.setExecResult(JSONObject.parseObject(actionDo.getExecData()));
            }
            else {
                actionParam.setExecResult(new JSONObject());
            }
            return actionParam;
        }
        catch (Exception e) {
            log.error("convert err msg: {}", e.getMessage());
            throw new RuntimeException("convert action value error");
        }
    }

    private List<ActionParam> convertListData(List<ActionDO> data){
        List<ActionParam> actionParams = new ArrayList<>();
        for (ActionDO actionDo : data) {
            ActionParam actionParam = convertData(actionDo, "simple");
            if (actionParam != null) {
                actionParams.add(convertData(actionDo, "simple"));
            }

        }
        return actionParams;
    }

    @Override
    public List<ActionParam> getActionSimpleList(QueryParam queryParam){
        ActionExample actionExample = new ActionExample();
        ActionExample.Criteria criteria = actionExample.createCriteria();
        if (queryParam.getActionId() != null) {
            criteria.andActionIdEqualTo(queryParam.getActionId());
        }
        if (queryParam.getActionType() != null) {
            criteria.andActionTypeEqualTo(queryParam.getActionType());
        }
        if (queryParam.getAppCode() != null) {
            criteria.andAppCodeEqualTo(queryParam.getAppCode());
        }
        if (queryParam.getElementId() != null) {
            criteria.andElementIdEqualTo(queryParam.getElementId());
        }
        if (queryParam.getNode() != null) {
            criteria.andNodeEqualTo(queryParam.getNode());
        }
        if (queryParam.getStatus() != null) {
            List<String> statuses= Arrays.asList(queryParam.getStatus().split(","));
            criteria.andStatusIn(statuses);
        }
        if (queryParam.getEntityType() != null) {
            criteria.andEntityTypeEqualTo(queryParam.getEntityType());
        }
        if (queryParam.getEntityValue() != null) {
            criteria.andEntityValueEqualTo(queryParam.getEntityValue());
        }
        if (queryParam.getEmpld() != null) {
            criteria.andEmpIdEqualTo(queryParam.getEmpld());
        }
        actionExample.appendOrderByClause(OrderCondition.CREATETIME, SortType.DESC);
        List<ActionDO> actionDOS = actionDAO.selectByParamWithBLOBs(actionExample);
        return convertListData(actionDOS);
    }

    @Override
    public Integer getCount(QueryParam queryParam){
        ActionExample actionExample = new ActionExample();
        ActionExample.Criteria criteria = actionExample.createCriteria();
        if (queryParam.getActionId() != null) {
            criteria.andActionIdEqualTo(queryParam.getActionId());
        }
        if (queryParam.getActionType() != null) {
            criteria.andActionTypeEqualTo(queryParam.getActionType());
        }
        if (queryParam.getAppCode() != null) {
            criteria.andAppCodeEqualTo(queryParam.getAppCode());
        }
        if (queryParam.getElementId() != null) {
            criteria.andElementIdEqualTo(queryParam.getElementId());
        }
        if (queryParam.getNode() != null) {
            criteria.andNodeEqualTo(queryParam.getNode());
        }
        if (queryParam.getStatus() != null) {
            List<String> statuses= Arrays.asList(queryParam.getStatus().split(","));
            criteria.andStatusIn(statuses);
        }
        if (queryParam.getEntityType() != null) {
            criteria.andEntityTypeEqualTo(queryParam.getEntityType());
        }
        if (queryParam.getEntityValue() != null) {
            criteria.andEntityValueEqualTo(queryParam.getEntityValue());
        }
        if (queryParam.getEmpld() != null) {
            criteria.andEmpIdEqualTo(queryParam.getEmpld());
        }

        List<ActionDO> actionDOS = actionDAO.selectByParamWithBLOBs(actionExample);
        return actionDOS.size();
    }


    @Override
    public ActionParam getAction(Long id){
        ActionDO actionDO = actionDAO.selectByPrimaryKey(id);
        return convertData(actionDO, "all");
    }

    @Override
    public int updateStatus(Long id, String status) {
        ActionDO actionDO = new ActionDO();
        actionDO.setStatus(status);
        actionDO.setId(id);
        return actionDAO.updateByPrimaryKeySelective(actionDO);
    }

    private Object callValidation(ActionParam actionParam) {
        String entityType = actionParam.getEntityType();
        String entityValue = actionParam.getEntityValue();
        if (entityType.equals("unknown") || entityValue.equals("unknown")) {
            return null;
        }
        Long actionId = actionParam.getActionId();
        ActionExample actionExample = new ActionExample();
        List<String> successStatuses = Arrays.asList("SUCCESS", "CANCELED", "REJECTED", Status.EXCEPTION.toString());
        ActionExample.Criteria criteria = actionExample.createCriteria();
        criteria.andActionIdEqualTo(actionId);
        criteria.andEntityTypeEqualTo(entityType);
        criteria.andEntityValueEqualTo(entityValue);
        criteria.andStatusNotIn(successStatuses);
        List<ActionDO> actionDOS = actionDAO.selectByParam(actionExample);
        if (actionDOS.size() > 0) {
            JSONArray array = new JSONArray();
            JSONObject data = new JSONObject();
            JSONObject message = new JSONObject();
            message.put("Error", String.format("Current entity %s has unfinished action", entityValue));
            data.put("data", message);
            data.put("type", "kvlist");
            array.add(data);
            JSONObject result = new JSONObject();
            result.put("feedbacks", array);
            return result;
        }
        return null;
    }

    @Override
    public Object callAction(ActionParam actionParam, String env, Cookie[] cookies, String tenant) {

        log.info("[Action][Call] action label: {}", actionParam.getActionLabel());
        String actionType = actionParam.getActionType();
        ActionDO actionDO = new ActionDO();
        if (! paramsValidation(actionType, actionParam.getActionMeta())) {
            throw new RuntimeException(String.format("params is not valid: %s", actionParam));
        }
        try {
            ClassUtil.copy(actionParam, actionDO);
        } catch (Exception e) {
            log.error("convert actionparams to actionDo failed, msg: {}, traceback: {}", e.getMessage(), e.getStackTrace());
            throw new RuntimeException("Can't convert actionparams to actionDo");
        }
        Object validationResult = callValidation(actionParam);
        if (validationResult != null) {
            return validationResult;
        }
        actionDO.setProcessor(actionParam.getProcessorInfo().toJSONString());
        actionDO.setActionMetaData(actionParam.getActionMeta().toJSONString());
        Long startTime = System.currentTimeMillis();
        actionDO.setUuid(UUID.randomUUID().toString());
        actionParam.setUuid(actionDO.getUuid());
        if (actionParam.getOrderType() != null) {
            actionDO.setOrderType(actionParam.getOrderType());
        }

            JSONObject result = getData(actionParam, env, cookies, tenant);
            log.info("----[Action Get Data] {}", result);

            actionParam.setStartTime(System.currentTimeMillis());

            JSONObject execData = getExecData(actionParam.getActionType(), actionParam.getAppCode(), result);
            actionDO.setExecData(execData.toJSONString());
            actionDO.setStartTime(startTime);
            actionDO.setEndTime(execData.getLong("endTime"));
            actionDO.setStatus(execData.getString("status"));
            actionDO.setActionType(execData.getString("_tesla_action_type"));
            log.info("[Action][Insert] action label: {}", actionDO.getActionLabel());
            actionDAO.insert(actionDO);
            if (actionType.equals("API")) {
                JSONObject urlMeta = actionParam.getActionMeta();
                return result;
            }
            log.info("---begin to create order");
            return result;
    }


    @Override
    public void saveAction(SaveActionParam data) throws  RuntimeException {
        log.info("----get save action params data: {}", data);
        log.info("----get empid: {}", data.getEmpId());
        ActionDO actionDO = new ActionDO();
        actionDO.setUuid(UUID.randomUUID().toString());
        actionDO.setStatus(data.getStatus());
        actionDO.setCreateTime(data.getCreateTime());
        actionDO.setStartTime(data.getCreateTime());
        actionDO.setEndTime(data.getCreateTime());
        actionDO.setExecData(data.getExecData().toJSONString());
        actionDO.setActionMetaData(data.getActionMeta().toJSONString());
        actionDO.setActionLabel(data.getActionLabel());
        actionDO.setActionId(data.getActionId());
        actionDO.setActionName(data.getActionName());
        actionDO.setEmpId(data.getEmpId());
        actionDO.setAppCode(data.getAppCode());
        actionDO.setElementId(data.getElementId());
        actionDO.setActionType(data.getActionType());
        actionDO.setEntityType(data.getEntityType());
        actionDO.setEntityValue(data.getEntityValue());
        actionDO.setProcessor(data.getProcessorInfo().toJSONString());
        actionDO.setNode(data.getNode());
        //if (data.getRole() == null || data.getRole().isEmpty()) {
        //    orderService.createOrder(actionDO);
        //}
        //else {
        //    orderService.createOrder(actionDO, data.getRole());
        //}
        actionDAO.insert(actionDO);
    }


    @Override
    public JSONObject getData(ActionParam actionParam, String env, Cookie[] cookies, String tenant){
        JSONObject result = new JSONObject();
        try {
            Map<String, Object> params = actionParam.getActionMeta();
            String appCode = actionParam.getAppCode();
            switch (actionParam.getActionType()) {
                case "API":
                    String url;
                    if (params.get("url").toString().contains("http")) {
                        url = params.get("url").toString();
                    }
                    else {
                        url = apiUrl + params.get("url").toString();
                    }
                    String method = ((JSONObject)params).getString("method");
                    log.info("begin to call api {}, method {}, params {}", url, method, params.get("params"));
                    Map<String, String> headers = new HashMap<>();
                    headers.put("x-biz-tenant", tenant);
                    headers.put("x-biz-app", actionParam.getAppCode());
                    headers.put("x-run-as-role", actionParam.getRole());
                    result = actionStatusService.call(url,
                        HttpMethod.valueOf(params.get("method").toString()),
                        params.get("params"), env, cookies, headers);
                    break;
                case "API_ASYNC":
                    result = actionStatusService.call(params.get("url").toString(),
                        HttpMethod.valueOf(params.get("method").toString()),
                        params.get("params"), env);
                    //result.put("startTime", System.currentTimeMillis());
//                case "JOB":
//                    log.info("[Action][Job] begin to create job instance, params: {}", params);
//                    result = actionStatusService.call(taskEndpoint + "/job-center/instances/template/exec",  HttpMethod.POST, params, env);
//                    log.info("create job instance success, return data is {}", result);
//                    break;
//                case "FLOW":
//                    params.put("app", appCode);
//                    result = actionStatusService.call(processEndpoint + "/process/start", HttpMethod.POST, params, env);
//                    break;

            }
        }
        catch (RuntimeException e) {
            log.error("call action failed, result is {}", result);
            throw new RuntimeException(String.format("call action failed, msg: %s", e.getMessage()));
        }
        return result;
    }


    @Override
    public JSONObject getData(ActionDO actionParam){
        JSONObject result = new JSONObject();
        try {
            Map<String, Object> params = JSONObject.parseObject(actionParam.getActionMetaData());
            String appCode = actionParam.getAppCode();
            JSONObject bpmsData = JSONObject.parseObject(actionParam.getBpmsData());
            String env = bpmsData.getString("env");
            switch (actionParam.getActionType()) {
                case "API":
                    String url;
                    Map<String, String> headers = new HashMap<>();
                    headers.put("x-empid", actionParam.getEmpId());
                    if (actionParam.getProcessor() != null) {
                        JSONObject processorInfo = JSONObject.parseObject(actionParam.getProcessor());
                        if (processorInfo.getString("account") != null) {
                            headers.put("x-auth-user", processorInfo.getString("account"));
                        }
                    }
                    if (params.get("url").toString().contains("http")) {
                        url = params.get("url").toString();
                    }
                    else {
                        url = apiUrl + params.get("url").toString();
                    }
                    String method = ((JSONObject)params).getString("method");
                    log.info("begin to call api {}, method {}, params {}", url, method, params.get("params"));

                    result = actionStatusService.call(url,
                        HttpMethod.valueOf(params.get("method").toString()),
                        params.get("params"), env, headers);
                    //log.info("[API RESULT] result is {}", result);
                    break;
                case "API_ASYNC":
                    result = actionStatusService.call(params.get("url").toString(),
                        HttpMethod.valueOf(params.get("method").toString()),
                        params.get("params"), env);
                    //result.put("startTime", System.currentTimeMillis());
//                case "JOB":
//                    result = actionStatusService.call(taskEndpoint + "/job-center/instances/template/exec",  HttpMethod.POST, params, env);
//                    log.info("create job instance success, return data is {}", result);
//                    break;
//                case "FLOW":
//                    params.put("app", appCode);
//                    result = actionStatusService.call(processEndpoint + "/process/start", HttpMethod.POST, params, env);
//                    break;

            }
        }
        catch (RuntimeException e) {
            log.error("call action failed, result is {}", result);
            throw new RuntimeException(String.format("call action failed, msg: %s", e.getMessage()));
        }
        return result;
    }

    @Override
    public JSONObject getExecData(String actionType, String appCode, JSONObject result) {
        JSONObject execData  = new JSONObject();
        execData.put("actionType", actionType);
        JSONObject data;
        switch (actionType) {
            case "FLOW":
                data = result.getJSONObject("data");
                execData.put("status", Status.RUNNING);
                execData.put("instanceId", data.get("id").toString());
                execData.put("createTime", data.get("startTime"));
                execData.put("startTime", data.get("startTime"));
                execData.put("instanceUrl", AbmUrl + String.format("/%s/manage/flow/instance/detail/%s",
                    appCode, data.get("id").toString()));
                execData.put("_tesla_action_type", "FLOW");
                break;
            case "JOB":
                execData.put("status", actionStatusService.getTaskStatus("RUNNING"));
                execData.put("createTime", System.currentTimeMillis());
                execData.put("instanceId", result.get("data"));
                execData.put("instanceUrl", AbmUrl + String.format("/%s/manage/taskplatform/instance/%s",
                    appCode, result.get("data")));
                execData.put("_tesla_action_type", "JOB");
                break;
            case "API":
                execData = result;
                try {
                    data = result.getJSONObject("data");
                    //log.info("---get api result: {}", data);
                    if (data != null && data.containsKey("_tesla_action_type") && data.getString("_tesla_action_type").equals("flow") && data.containsKey("instanceId")) {
                        execData.put("status", Status.RUNNING);
                        execData.put("instanceId", data.getString("instanceId"));
                        execData.put("createTime", System.currentTimeMillis());
                        execData.put("startTime", System.currentTimeMillis());
                        execData.put("instanceUrl", AbmUrl + String.format("/%s/manage/flow/instance/detail/%s",
                            appCode, data.getString("instanceId")));
                        execData.put("_tesla_action_type", "FLOW");
                        return execData;
                    }
                }
                catch (Exception e) {
                    log.error("[Action][API] can not get jsonobject of api result: {}, err: {}", result, e.getMessage());
                }
                execData.put("endTime", System.currentTimeMillis());
                execData.put("status", Status.SUCCESS);
                execData.put("_tesla_action_type", "API");
                break;
            case "API_ASYNC":
                execData = result;
                execData.put("status", Status.SUCCESS);
                execData.put("_tesla_action_type", "API_ASYNC");
        }
        return execData;
    }

    private Boolean paramsValidation(String type, Map<String, Object> params) {
        switch (type) {
            case "API":
                if (! params.containsKey("url") || (! params.containsKey("method"))) {
                    return false;
                }
                break;
            case "JOB":
                if (! params.containsKey("templateId")) {
                    return false;
                }
                break;
            case "FLOW":
                if (! params.containsKey("name") || (! params.containsKey("defKey"))) {
                    return false;
                }
                break;
        }
        return true;
    }

    @Override
    public void checkBpmsStatus(){
        return;
    //    ActionExample actionExample = new ActionExample();
    //    ActionExample.Criteria criteria = actionExample.createCriteria();
    //    criteria.andStatusEqualTo(Status.APPROVING.toString());
    //    List<ActionDO> actionDOS = actionDAO.selectByParamWithBLOBs(actionExample);
    //    for (ActionDO actionDO : actionDOS) {
    //        String bpmsString = actionDO.getBpmsResult();
    //        String bpmsId = JSONObject.parseObject(bpmsString).getString("instanceId");
    //        String status = bpmsService.checkStatus(bpmsId);
    //        Long curTime = System.currentTimeMillis();
    //        if (Status.APPROVED.toString().equals(status)) {
    //            try {
    //                log.info("[Bpms][Exec] bpms approved, beigin to exec, action id: {}", actionDO.getUuid());
    //                JSONObject execResult = getData(actionDO);
    //                JSONObject execData = getExecData(actionDO.getActionType(), actionDO.getAppCode(), execResult);
    //                actionDO.setExecData(execData.toJSONString());
    //                actionDO.setStartTime(curTime);
    //                actionDO.setEndTime(execData.getLong("endTime"));
    //                actionDO.setStatus(execData.getString("status"));
    //                actionDO.setActionType(execData.getString("_tesla_action_type"));
    //                actionDO.setExecData(execData.toJSONString());
    //            }
    //            catch (RuntimeException e) {
    //                log.info("[Bpms] [Exec] call exec failed, begin to set action to exception, action id: {}, err: {}", actionDO.getUuid(), e.getMessage());
    //                Long endTime = System.currentTimeMillis();
    //                actionDO.setEndTime(endTime);
    //                actionDO.setStatus(Status.EXCEPTION.toString());
    //                actionDO.setExecData(e.getMessage());
    //            }
    //            finally {
    //                actionDAO.updateByPrimaryKeyWithBLOBs(actionDO);
    //                orderService.updateOrder(actionDO);
    //            }
    //
    //        }
    //        else if (Status.REJECTED.toString().equals(status)) {
    //            log.info("[Bpms][Exec] set action to rejected, action id: {}", actionDO.getUuid());
    //            actionDO.setStatus(Status.REJECTED.toString());
    //            actionDO.setEndTime(curTime);
    //            actionDAO.updateByPrimaryKeyWithBLOBs(actionDO);
    //            orderService.updateOrder(actionDO);
    //        }
    //        else if (Status.CANCELED.toString().equals(status)) {
    //            log.info("[Bpms][Exec] set action to canceled, action id: {}", actionDO.getUuid());
    //            actionDO.setStatus(Status.CANCELED.toString());
    //            actionDO.setEndTime(curTime);
    //            actionDAO.updateByPrimaryKeyWithBLOBs(actionDO);
    //            orderService.updateOrder(actionDO);
    //        }
    //        else {
    //            continue;
    //        }
    //
    //    }
    }
}
