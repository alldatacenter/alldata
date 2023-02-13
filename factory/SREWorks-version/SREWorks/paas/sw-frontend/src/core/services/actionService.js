/**
 * Created by caoshuaibiao on 2019/1/24.
 * action服务接口
 */

import httpClient  from '../../utils/httpClient';
const jobApiPrefix="gateway/v2/platform/taskPlatform";
const flowApiPrefix="flow";
const actionPrefix="gateway/v2/foundation";

class ActionService {

    loadJobTemplateDetail(templateId){
        return httpClient.get(`${jobApiPrefix}/job-center/templates/${templateId}`);
    }

    executeJobAction(templateContent){
        /*return httpClient.post(`${jobApiPrefix}/job-center/templates/${templateId}/exec-once`,templateContent).then(instanceId=>{
           return httpClient.post(`${jobApiPrefix}/job-center/instances/${instanceId}/control`,{action: 'START'});
        });*/
        return httpClient.post(`${jobApiPrefix}/job-center/instances/template/exec`,templateContent);
    }

    actionPreview(api,params,method='POST'){
        if(method==='POST'){
            return httpClient.post(api,params);
        }else{
            return httpClient.get(api);
        }
    }

    executeFlowAction(productId,defKey,bizKey,name,userId,payload,defId,bizId,event){
        return httpClient.post(`${flowApiPrefix}/process/start`,{
            defKey:defKey,
            bizKey:bizKey,
            name:name,
            userId:userId,
            payload:payload,
            defId:defId,
            bizId:bizId,
            tenantId:productId,
            event:event
        });
    }

    submitAction(action,params){
        let {__approveComment,__approveCountersign,__actionPlanExecutionTime,...submitParams}=params,approveList=false,actionPlanExecutionTime;
        if(action.enableApproval){
            approveList=[...action.approveList];
            if(action.__approveCountersign&&action.__approveCountersign.length){
                action.__approveCountersign.forEach(approve=>{
                    let approveInfo={
                        approveTitle:'加签人审批',
                        approvers:[{
                            emplId:approve.emplId,
                            name:approve.name,
                            nickNameCn:approve.nickNameCn,
                            showName:approve.nickNameCn||approve.name
                        }]
                    };
                    approveList.push(approveInfo);
                });
            }
        }
        if(action.__actionPlanExecutionTime){
            actionPlanExecutionTime=action.__actionPlanExecutionTime.valueOf();
        }
        //添加自定义明细页面配置信息
        if(action.detailComponents&&action.detailComponents.length){
            submitParams.detailComponents=action.detailComponents;
        }
        submitParams.__detailFormKv = action.__detailFormKv;
        let notifyDataJson = null;
        if (action.noticeEnable) {
            notifyDataJson = {
                "noticeGroupKey": action.noticeGroupKey,
                "noticeTypes": action.noticeTypes,
                "templateKey": action.templateKey,
                "notifyTime": action.notifyTime,
            };
        }
        let submitActionData={
            "actionType": action.actionType,
            "elementId": action.elementId,
            "entityType":action.entityType||'unknown',
            "entityValue":action.entityValue||'unknown',
            "actionId":action.id,
            "actionMeta": submitParams,
            "actionName": action.name,
            "actionLabel": action.orderTitle||action.label,
            "node":action.nodeId,
            "empId": action.userInfo.empId,
            "orderType":action.orderType||"changefree",
            "bpmsMetaData":(approveList||action.approvalKey)?{
                approverList:approveList||[],
                remark:action.__detailFormKv
            }:undefined,
            "processorInfo": {
                "name": action.userInfo.nickname || action.userInfo.nickNameCn||action.userInfo.nickName||action.userInfo.lastName||action.userInfo.name||action.userInfo.empId,
                "empId": action.userInfo.empId,
                "account":action.userInfo.emailPrefix
            },
            notifyDataJson,
            approvalKey:action.approvalKey,
            actionPlanExecutionTime:actionPlanExecutionTime
        };
        //console.log("submitActionData---->",submitActionData);
        //return Promise.resolve();
        return httpClient.post(`${actionPrefix}/action/run`,submitActionData);
        //return httpClient.post(`http://30.5.16.57:7001/action/run`,submitActionData);
    }


    //只记录操作数据无需后端去调用
    recordAction(action,params,result,startTime){
        let submitActionData={
            "appCode":action.product.productId,
            "status" : 'SUCCESS',
            "actionType": action.actionType,
            "elementId": action.elementId,
            "actionId":action.id,
            "entityType":action.entityType||'unknown',
            "entityValue":action.entityValue||'unknown',
            "processorInfo": {
                "name": action.userInfo.nickNameCn||action.userInfo.nickName||action.userInfo.lastName||action.userInfo.name||action.userInfo.empId,
                "empId": action.userInfo.empId
            },
            "actionMeta" :params,
            "actionName": action.name,
            "actionLabel": action.label,
            "node":action.nodeId,
            "empId" : action.userInfo.empId,
            "createTime":startTime,
            "execData" :{
                "status": "SUCCESS",
                "createTime":startTime,
                "startTime": startTime,
                "endTime": (new Date()).getTime(),
                "result" : result
            }
        };

        return httpClient.post(`${actionPrefix}/action/save`,submitActionData);
    }



}

export default new ActionService();