/**
 * Created by caoshuaibiao on 2020/11/4.
 */

import  httpClient from '../../utils/httpClient';
import * as util from '../../utils/utils'
import {getBuiltInWidgetCatgory} from '../framework/components/WidgetRepository';
import properties from 'appRoot/properties';
import cacheRepository from '../../utils/cacheRepository'

const newPrefix="gateway/v2/foundation/frontend-service/frontend";
const opsPrefix="gateway/v2/common/productops/frontend";

const productopsPrefix = (properties.envFlag===properties.ENV.Standalone?newPrefix:opsPrefix)




function getHeader(app) {
    return {
        headers:{
             'X-Biz-App': util.getNewBizApp()
        }
    }
}
function getProductName(){
    //从路径获取应用名,如果没有则默认为sreworks主站
    let productName=window.location.hash.split("/")[1];
    if(!productName) {
        if(properties.defaultProduct&&!properties.defaultProduct.includes("$")){
            productName=properties.defaultProduct;
        }else{
            productName='desktop';
        }
    }
    if(productName.includes("?")){
        productName=productName.split("?")[0];
    }
    return productName;
}

class Service {
    /**
     * 获取应用的菜单树
     * @param app
     */
    getMenuTree(app) {
        let params = {
            includeExtra: true,
            treeType: "app",
            appId: app,
        }
        let productName = getProductName()
        return httpClient.get(`${productopsPrefix}/appTrees/structures?stageId=dev`, {
            ...getHeader(app),
            params
        });
    }

    updateNodeRoles(params){
        if(properties.devFlag==="dev"){
            httpClient.put(`${newPrefix}/nodeTypePaths/roles`, params);
        }
        return httpClient.put(`${productopsPrefix}/nodeTypePaths/roles?stageId=dev`, params);
    }


    deleteNode(data) {
        if(properties.devFlag==="dev"){
            httpClient.delete(`${newPrefix}/appTrees/structure`, {data: data});
        }
        return httpClient.delete(`${productopsPrefix}/appTrees/structure?stageId=dev`, {data: data});
    }

    updateNode(params) {
        if(properties.devFlag==="dev"){
            httpClient.put(`${newPrefix}/appTrees/structure`, params,{...getHeader()});
        }
        return httpClient.put(`${productopsPrefix}/appTrees/structure?stageId=dev`, params,{...getHeader()});
    }

    insertNode(params) {
        if(properties.devFlag==="dev"){
            httpClient.post(`${newPrefix}/appTrees/structure`,params,{...getHeader()});
        }
        return httpClient.post(`${productopsPrefix}/appTrees/structure?stageId=dev`, params,{...getHeader()});
    }


    getAllServiceTypes(params){
        return httpClient.get(`${productopsPrefix}/serviceTypes`,{params:params,...getHeader()})
    }

    getNodeRoles(params) {
        if(properties.envFlag===properties.ENV.Standalone){
            return Promise.resolve([]);
        }
        return httpClient.get(`${productopsPrefix}/nodeTypePaths/roles?stageId=dev`, {params: params});
    }
    /**
     * 获取应用的所有角色
     * @param params
     */
    getRoles(app){
        return httpClient.get(`gateway/v2/common/authProxy/roles?appId=${app}`,{...getHeader()});
    }

    getAppTables(params){
        return httpClient.get(`gateway/v2/foundation/teslacmdb/entity/product/meta/getTables`, {params: params});
    }

    getColumnList(params){
        return httpClient.get(`gateway/v2/foundation/teslacmdb/entity/product/meta/getColumnList`, {params: params});
    }

    getMainPage(nodeTypePath,stageId){
        let params = {};
        let productId = getProductName()
         if(stageId) {
            params = {
                nodeTypePath,
                stageId
            }
         } else {
            params = {
                nodeTypePath,
                stageId: cacheRepository.getBizEnv(productId)==='dev'? 'dev' : 'prod'
            }
         }
        return httpClient.get(`${productopsPrefix}/nodeTypePaths/tabs`, {
            ...getHeader(),
            params
        }).then(result=>{
            return result.tabs[0]
        })
    }


    saveElement(itemData) {
        if(properties.devFlag==="dev"){
            httpClient.put(`${newPrefix}/element`,itemData,{...getHeader()});
        }
        //flayAdmin内部前端设计器全部为dev环境
        return httpClient.put(`${productopsPrefix}/element?stageId=dev`,itemData,{...getHeader()});
    }

    getElements(nodeTypePath,stageId) {
        let params = {};
        let productId = getProductName()
        if(stageId) {
           params = {
               nodeTypePath,
               stageId
           }
        } else {
           params = {
               nodeTypePath,
               stageId: cacheRepository.getBizEnv(productId)==='dev'? 'dev' : 'prod'
           }
        }
        return httpClient.get(`${productopsPrefix}/nodeTypePaths/elements`, {
            ...getHeader(),
            params
        })
    }



    deleteElement(itemData) {
        if(properties.devFlag==="dev"){
            httpClient.delete(
                `${newPrefix}/nodeTypePaths/element`,
                {data:itemData},
                {...getHeader()}
            )
        }
        return httpClient.delete(
            `${productopsPrefix}/nodeTypePaths/element?stageId=dev`,
            {data:itemData},
            {...getHeader()}
        )
    }

    saveMainPage(tabData={}) {
        if(properties.devFlag==="dev"){
            httpClient.put(
                `${newPrefix}/nodeTypePaths/tab?stageId=dev`,
                tabData,
                {...getHeader()}
            )
        }
        return httpClient.put(
            `${productopsPrefix}/nodeTypePaths/tab?stageId=dev`,
            tabData,
            {...getHeader()}
        )
    }

    attachNode(nodeTypePath,elementId){
        if(properties.devFlag==="dev"){
            httpClient.put(
                `${newPrefix}/nodeTypePaths/element`,
                {
                    "nodeTypePath": nodeTypePath,
                    "elementId": elementId,
                    "order": 999,
                    version:0
                },
                {...getHeader()}
            ).then(res => {
                return { success: true, data: res };
            });
        }
        return httpClient.put(
            `${productopsPrefix}/nodeTypePaths/element?stageId=dev`,
            {
                "nodeTypePath": nodeTypePath,
                "elementId": elementId,
                "order": 999,
                version:0,
            },
            {...getHeader()}
        ).then(res => {
            return { success: true, data: res };
        });
    }

    getWidgetRepository(){
        return Promise.resolve(getBuiltInWidgetCatgory());
    }

    getNodeParams (nodeId,parameters={}) {
        return Promise.resolve({});
        //return httpClient.post(productopsPrefix+"/nodes/params",{nodeId:nodeId,parameters:parameters})
    }
    getCustomList () {
        return httpClient.get('/gateway/v2/foundation/frontend-service/frontend/component/list?stageId=prod')
    }
    createFromTemplateByClone(params) {
        return httpClient.post('/gateway/v2/foundation/frontend-service/frontend/appTrees/clone?stageId=dev',params)
    }

}

const service = new Service();

export default service;