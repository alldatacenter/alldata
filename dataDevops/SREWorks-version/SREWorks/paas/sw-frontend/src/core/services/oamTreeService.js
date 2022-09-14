/**
 * Created by caoshuaibiao on 2019/1/3.
 * 运维树相关服务
 */
import  httpClient from '../../utils/httpClient';
import properties from "../../properties";
import cacheRepository from '../../utils/cacheRepository'
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
//const apiPrefix="gateway/v2/common/productops/frontend/";
const apiPrefix = (properties.envFlag===properties.ENV.Standalone?
    "gateway/v2/foundation/frontend-service/frontend"
    :"gateway/v2/common/productops/frontend");

class OamTreeService {


    /**
     * 获取运维树元数据
     * @param appTreeId
     * @returns {*|V}
     */
    getTreeMeta (appTreeId) {
        let params = {nodeId:appTreeId}
        params['stageId'] = cacheRepository.getBizEnv(getProductName())==='dev'? 'dev' : 'prod';
        return httpClient.get(apiPrefix+"/appTrees/meta",{params})
    }

    /**
     * 全量加载指定node的树结构,数据量大时慎用
     * @param nodeId
     * @returns {*|V}
     */
    getTreeArchives (nodeId) {
        let params ={nodeId:nodeId,level:-1}
        params['stageId'] = cacheRepository.getBizEnv(getProductName())==='dev'? 'dev' : 'prod';
        return httpClient.get(apiPrefix+"/nodes/archives",{params})
    }


    /**
     * 获取节点上配置的功能页签
     * @param nodeId
     * @param parameters
     * @returns {*|V}
     */
    getNodeTabs (nodeId,parameters) {
        if(properties.envFlag===properties.ENV.Standalone){
            return httpClient.get(`${apiPrefix}/nodeTypePaths/tabs`, {
                params:{
                    nodeTypePath: nodeId
                }
            })
        }
        let params ={nodeId:nodeId,parameters:parameters}
        let stageId = cacheRepository.getBizEnv(getProductName())==='dev'? 'dev' : 'prod';
        return httpClient.post(apiPrefix+"/nodes/tabs?stageId="+stageId,params)
    }

    /**
     * 获取节点上的数据过滤查询参数
     * @param nodeId
     * @returns {*|V}
     */
    getNodeFilters (nodeId) {
        let params = {nodeId:nodeId}
        params['stageId'] = cacheRepository.getBizEnv(getProductName())==='dev'? 'dev' : 'prod';
        return httpClient.get(apiPrefix+"/nodes/filters",{params})
    }


    /**
     * 获取内置组件的元数据信息
     * @param elementId
     * @param nodeId
     * @param parameters
     */
    getWidgetMeta (nodeId,elementId,parameters) {
        let params = {nodeId:nodeId,elementId:elementId,parameters:parameters}
        let stageId = cacheRepository.getBizEnv(getProductName())==='dev'? 'dev' : 'prod';
        return httpClient.post(apiPrefix+"/nodes/elements/config?stageId="+stageId,params);
    }


    /**
     * 获取指定节点下的所有action定义
     * @param nodeId
     * @param parameters
     */
    getNodeActions(nodeId,parameters){
        let params = {nodeId:nodeId,filters:'ACTION'}
        params['stageId'] = cacheRepository.getBizEnv(getProductName())==='dev'? 'dev' : 'prod';
        return httpClient.get(apiPrefix+"/nodes/elements",{params})
    }

}


export default new OamTreeService();
