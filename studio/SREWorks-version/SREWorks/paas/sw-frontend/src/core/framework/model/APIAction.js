/**
 * Created by caoshuaibiao on 2019/3/13.
 * 对接API接口的Action模型
 */
import Action from './Action';
import httpClient from '../../../utils/httpClient';
import actionService from '../../services/actionService';
export default class APIAction extends Action {

    constructor(actionData, userInfo, product, runtimeContext) {
        super(actionData, userInfo, product, runtimeContext);
        this.bodyType = actionData.bodyType;
        this.api = actionData.api;
        this.method = actionData.method;
    }

    /**
     * 执行动作,自定义api支持根据传入的操作button定义执行
     */
    execute(params, formValues = {}, actionButton) {
        if (params) {
            this.parameterDefiner.updateParametersValue(params);
        }
        let data = { ...formValues }, parameters = this.parameterDefiner.getParameters(), uploadParams = [];
        parameters.forEach(param => {
            //上传类型
            if (param.type === 81 && param.defModel.type !== "async") {
                uploadParams.push(param.name);
            }
            data[param.name] = param.getValue();
        });
        let url = this.api, method = this.method;
        if (actionButton) {
            url = actionButton.url;
            method = actionButton.method;
        }
        //存在上传类型需要直接请求至文件接口,并进行上传操作的的记录保存
        if (uploadParams.length > 0) {
            let formData = new FormData(), filesName = [];
            Object.keys(data).forEach(key => {
                if (uploadParams.includes(key)) {
                    //上传项支持多个文件，因此值是数组
                    data[key].forEach(file => {
                        filesName.push(file.name);
                        formData.append(key, file);
                    });
                    delete data[key];
                } else {
                    formData.append(key, data[key]);
                }
            });
            data.files = filesName;
            return httpClient.post(url, formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            }).then(result => {
                let startTime = (new Date()).getTime();
                actionService.recordAction(Object.assign({}, this, { api: url, method: method }), data, result, startTime);
                return result;
            });
        }
        /*if(properties.deployEnv==='prepub'){
            let apiRequest=null;

            //delete同get 和put同post
            if(method==='PUT'){
                apiRequest= httpClient.put(url,data);
            }else if(method==='DELETE'){
                apiRequest= httpClient.delete(url);
            }else{
                if(this.bodyType===2){
                    apiRequest= httpClient.postFormData(url,data);
                }else{
                    apiRequest= httpClient.post(url,data);
                }
            }
            let startTime=(new Date()).getTime();

            return apiRequest.then(result=>{
                actionService.recordAction(Object.assign({},this,{api:url,method:method}),data,result,startTime);
                return result;
            })
        }*/
        //切换到后端调用后
        let relativeUrl = url;
        if (relativeUrl.includes("http")) {
            let spUrl = relativeUrl.split("/gateway/")[1];
            if (spUrl) {
                relativeUrl = "gateway/" + spUrl;
            } else {
                console.error("----非约定的url地址----->", relativeUrl);
            }
        }
        let { __approveComment, __approveCountersign, __actionPlanExecutionTime, ...submitData } = data;
        return actionService.submitAction(this, {
            method: method || 'POST',
            url: relativeUrl,
            params: submitData
        });
    }
    canExecute() {
        return true;
    }

}