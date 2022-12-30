/**
 * Created by caoshuaibiao on 2018/8/22.
 * 用户服务接口
 */

import {httpClient,util} from '../index';
import properties from 'appRoot/properties';
import cacheRepository from '../../utils/cacheRepository';

const  baseUrl=properties.baseUrl;

const apiEndpoint=properties.apiEndpoint;

const applicationApiPerfix="gateway/v2/foundation/application/";
const authPrefix="gateway/v2/common/authProxy/";
const opsAppPerfix="gateway/v2/common/productops/apps";

let loadTime=0,loginStartPoint=0;
function getProductName(){
    //从路径获取应用名,如果没有则默认为tesla主站
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

class AppService {

    constructor(){
    }

    checkLogin () {
        let app=getProductName();
        let urlParams=util.getUrlParams();
        let {namespaceId,stageId}=urlParams;
        if(!namespaceId){
            let cacheBizId=cacheRepository.getAppBizId(app);
            if(cacheBizId){
                cacheBizId=cacheBizId.split(",");
                namespaceId=cacheBizId[1];
                stageId=cacheBizId[2];
            }else{
                namespaceId=properties.defaultNamespace;
                stageId=properties.defaultStageId;
            }
        }
        cacheRepository.setAppBizId(app,namespaceId,stageId);
        httpClient.addRequestInterceptor(function (req) {
            req.headers.common['X-Biz-App'] = util.getNewBizApp(app);
            return req;
        });
        return httpClient.get(authPrefix+'auth/user/info').then(resp=>{
            if(resp.code===401){
                return resp;
            }
            return httpClient.get(authPrefix+'profile').then(result=>{
                return Object.assign({},resp,result,result&&result.ext,{
                    originalUser:{empId:result.empId,account:result.account},
                    userName:result.userName||result.nickNameCn||result.name,
                    canSwitchUser:false,
                    roles:result.roles
                })
            });
        })

    }

    logout () {
        if(properties.envFlag===properties.ENV.Internal){
            return httpClient.get(baseUrl+"/logout?appId=tesla-authproxy")
        }else{
            let callback=window.location.href;
            window.location.href = apiEndpoint + authPrefix+'auth/logout?appId=bcc&callback='+callback;
        }
    }
    getAppInitData(){
        let productName=getProductName(),settingReq=[];
        let params = {nodeId:`${productName}|app|I`,level:-1,cache:false};
        params['stageId'] = cacheRepository.getBizEnv(productName)==='dev'? 'dev' : 'prod';
        settingReq.push(httpClient.get("gateway/v2/foundation/appmanager/apps/"+productName));
        settingReq.push(Promise.resolve([]));
        settingReq.push(httpClient.get(
            properties.envFlag===properties.ENV.Standalone?
            "gateway/v2/foundation/frontend-service/frontend/nodes/archives"
            :`gateway/v2/common/productops/frontend/nodes/archives`,
            {params:params}).
        then(menuData=>{
            //接口切换后进行的路由数据适配
            const genPath=function (children,path) {
                children.forEach(c=>{
                    c.path=path+`/${c.name}`;
                    c.config=c.config||c.extensionConfig;
                    Object.assign(c,c.config);
                    if(c.children&&c.children.length){
                        genPath(c.children,c.path);
                    }
                });

            };
            menuData.config=menuData.config||menuData.extensionConfig;
            menuData.path=`/${menuData.name}`;
            Object.assign(menuData,menuData.config);
            genPath(menuData.children,menuData.name);
            return menuData;
        }));
        return httpClient.all(settingReq);
    }

    /**
     * 获取应用设置,内外的后端暂时无法统一,因此在此做了路由数据的控制生成
     */
    getApplicationSettings=(routes,userInfo)=>{
        //增加是否为主页判断，如果是主页直接返回
        let path=window.location.hash;
        if(properties.envFlag===properties.ENV.PaaS
            &&(path.endsWith("#/")||path.endsWith("#"))
            &&!properties.defaultProduct
        ){
            return Promise.resolve(new ApplicationSetting({
                products:{},
                currentProduct:{},
                accessRoutes:[],
                auths:[]
            }))
        }

        let {roles=[]}=userInfo,app=getProductName();
        //对角色进行的过滤验证
        userInfo.roles=roles.filter(role=>{
            return role.roleId.startsWith(app+":");
        });
        window.__ABM_CURRENT_USER_ID=userInfo.empId;
        //获取上次本地缓存的角色,如果不存在就以所有角色中的第一个访问,如果不存在角色定义以内置的guest访客访问
        let guestRole=(app+":guest");
        let asRole=cacheRepository.getRole(app)||(userInfo.roles[0]&&userInfo.roles[0].roleId)||guestRole;
        //若角色id非访客且不在拥有角色内,则直接赋值为访客角色
        if(asRole!==guestRole&&userInfo.roles.length&&userInfo.roles.filter(role=>role.roleId===asRole).length===0){
            asRole=guestRole;
        }
        cacheRepository.setRole(app,asRole);
        //localStorage.setItem(roleCacheKey, asRole);
        let needRole=util.getUrlParams()["__needRole"];
        if(needRole==='false'||needRole===false||properties.envFlag!==properties.ENV.Internal){

        }else{
            httpClient.addRequestInterceptor(function (req) {
                req.headers.common['X-Run-As-Role'] = asRole;
                req.headers.common['X-Biz-Tenant'] = "alibaba";
                return req;
            });
        }

          return this.getAppInitData().then(respDatas=>{
                 if(!respDatas||!respDatas[0]||!respDatas[0].options){
                     console.error("无法获取产品数据------->",respDatas);
                     return {
                         currentProduct:false,
                     }
                 }
                //生成兼容原有代码的当前产品
                let currentProduct=respDatas[0].options;
                window.__TESLA_COMPONENT_LIB_CURRENT_APP=currentProduct;
                currentProduct.productName=currentProduct.nameCn||currentProduct.appName||currentProduct.name;
                currentProduct.productId=respDatas[0].appId||currentProduct.appId||currentProduct.name;
                currentProduct.groupKey=currentProduct.organization;
                currentProduct.admins=respDatas[0].admins||[];
                currentProduct.approvalUsers=respDatas[0].approvalUsers||[];
                currentProduct.environments = respDatas[0].environments;
                //生成产品导航定义
                let products=respDatas[1].map(group=>{
                    let productCategory={title:group.label,children:[]};
                    //group.title=group.label;
                    group.children.forEach(c=>{
                        let ops=c.options;
                        productCategory.children.push(
                            {
                                productName:ops.nameCn,
                                productId:ops.name,
                                groupKey:ops.organization,
                                ...ops
                            }
                        );
                    });
                    //return group;
                    return productCategory;
                });
                //生成当前产品的路由,并把路由组件进行拍平处理,以方便合并至后台资源数据生成路由数据
                let componentMapping={};
                const genPath=function(routes){
                    routes.filter(r=>r.name!=='home').map((route, key) => {
                        if(route.path){
                            route.path="/"+currentProduct.productId+route.path;
                            componentMapping[route.path]=route;
                        }
                        if(route.children){
                            genPath(route.children)
                        }
                    })
                };
                genPath(routes);
                if(!currentProduct){
                    return {};
                }
                //生成路由路径
                let resourceTree=respDatas[2];
                if(!resourceTree) return {};
                //以后端返回资源数据为准生成路由数据
                let remoteRouteData=resourceTree.children||[];
                //把框架层次提供的组件填入后端资源数据中生成可控的路由数据
                const genAccessRoutes=function(remoteRoutes){
                    remoteRoutes.sort(function(a,b){return a.seq-b.seq});
                    remoteRoutes.map((route) => {
                        if(!route.path.startsWith("/")) route.path="/"+route.path;
                        let path=route.path;
                        if(path&&componentMapping[path]){
                            if(route.serviceType==="mon"&&route.type==="buildIn"&&route.config.defaultMonId){
                                currentProduct.defaultMonId=route.config.defaultMonId;
                            }
                            //后端配置以后端为准,没有取前端配置值
                            Object.assign(route,componentMapping[path],{children:route.children},
                                {layout:route.layout||componentMapping[path].layout},
                                {label:route.label||componentMapping[path].label},
                                {icon:route.icon||componentMapping[path].icon,seq:route.seq||componentMapping[path].seq}
                                );
                        }
                        if(route.children){
                            genAccessRoutes(route.children)
                        }
                    })
                };
                genAccessRoutes(remoteRouteData);
                let setting=new ApplicationSetting({
                    products:products,
                    currentProduct:currentProduct,
                    accessRoutes:remoteRouteData,
                    auths:[]
                });

                return setting
            })
    }

    switchLanguage(lng){
        return httpClient.post(authPrefix+'profile/lang',{"lang": lng}).then(result=>{
            window.location.reload();
        });
    }

    getAllProfile(params) {
        // console.log(props.home);
        //http://frontend.ca221ae8860d9421688e59c8ab45c8b21.cn-hangzhou.alicontainer.com/gateway/v2/foundation/appmanager/instances?namespaceId=default&stageId=dev&visit=true&pagination=false
       const {stageId,visit} = params;
       return httpClient.get("gateway/v2/foundation/appmanager/realtime/app-instances", {params: {stageId,visit,page:1,pageSize:1000,optionKey:'source',optionValue:'swadmin'}})
          .then(res => {
            //   服务端数据结构改变
              let transItems = []
              transItems = res.items && res.items.length && res.items.map(item=>{
                  return Object.assign(item,{})
              })
               transItems && transItems.forEach(item=> {
                  if(item.options) {
                    item.appName= item.options.name || '';
                    item.logoUrl= item.options.logoImg || '';
                    item.introduction = item.options.description || '';
                    item.category = item.options.category
                  }
                  item.id = item.appInstanceId || '';
              })
              return httpClient.get("gateway/v2/foundation/appmanager/profile", {params: params})
                .then(profile => {
                    let copyProFile = _.cloneDeep(profile);
                    let includesIndex = [];
                    let collectList = profile.collectList || [];
                    let workspaces = profile.workspaces || [{
                        name: "新建桌面",
                        type: "custom",
                        hasSearch: true,
                        items: [],
                        background: "../../assets/deskstop/deskstop_four.jpg",
                    }];
                    let customQuickList = profile.customQuickList || [];
                    JSON.parse(JSON.stringify([...transItems || [], ...customQuickList])).map(item => {
                        includesIndex.push(item.id);
                    });
                    workspaces.map(workspace => {
                        workspace.items = workspace.items.filter(item => {
                            return includesIndex.includes(item.id);
                        });
                    });
                    // if (workspaces[0].items.length === 0) {
                    //     alert(JSON.stringify(workspaces[0].items));
                    // }
                    //过滤掉没有权限的应用
                    profile.collectList = collectList.filter(item => {
                        return includesIndex.includes(item.id);
                    });
                    profile.workspaces = workspaces;
                    profile.customQuickList = customQuickList;
                    return {appStore: transItems, profile, isEqual: _.isEqual(copyProFile,profile)};
                });
          });
    }

    postWorkspaces(params,namespaceId,stageId) {
        return httpClient.put(`gateway/v2/foundation/appmanager/profile?namespaceId=${namespaceId}&stageId=${stageId}`, params);
    }
    isLogined() {
        return httpClient.get('gateway/v2/common/authProxy/auth/user/info')
    }
    // // 更新桌面背景图片列表
    updateDesktopBackgroundList(params) {
        return httpClient.post(`gateway/sreworks/config/setObject?name=imgList`,params)
    }
    // 获取桌面背景列表
    getDesktopBackgroundList() {
        return httpClient.get(`gateway/sreworks/config/getObject?name=imgList`)
    }
    desktopKeySearch(userEmpId,category,query,type,currentPage=1,size=10) {
        return httpClient.get(`gateway/v2/foundation/kg/sreworkssearch/query/query_simple_nodes_from_size?__userEmpId=${userEmpId}&category=${category}&query=${query}&type=${type}&page=${currentPage}&size=${size}`)
    }
    testSearchService() {
        return httpClient.get('/gateway/v2/foundation/appmanager/apps/search/version')
    }

}

// 实例化后再导出
export default new AppService();


/**
 *定义些应用级别的设置对象
 */

export class ApplicationSetting{

    constructor(settingsData){
        this.products=settingsData.products;
        Object.assign(this,settingsData);
    }

}
