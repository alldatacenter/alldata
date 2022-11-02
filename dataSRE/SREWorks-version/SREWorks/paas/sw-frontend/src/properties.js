/**
 * Created by caoshuaibiao on 2019/6/5.
 * 配置应用显示
 */
 const blackLogo = require('appAssets/img/aliyun-logo.svg');
 const whiteLogo = require('appAssets/img/abm_logo.png');
 const textLogo = require('appAssets/img/bigdata.png');
 const whiteLogoLine = require('appAssets/img/logo-gif-line.gif');
 
 // 环境公用的配置项
 let properties = {
     ENV: {//运行环境定义
         ApsaraStack: "ApsaraStack",//标专
         DXZ: "DXZ",//大小专
         OXS: "OXS",//OXS
         Internal: "Internal",//对内()
         Standalone: "Standalone",//软件化输出
         RQY: "RQY",//敏捷版
         PaaS: "PaaS"//paas化版本
     },
     name: 'SREWorks',
     // envFlag: "PaaS",
     envFlag: "Standalone",
     defaultProduct: "desktop",
     defaultNamespace: "default",
     defaultStageId: "dev",
     deployEnv: process.env.DEPLOY_ENV || 'local',
     platformName: 'SREWorks',
     platformLogo: '/static/publicMedia/new_favicon.png',
 };
 
 /**
  *
  * 注意:在此不要配置各模块相关的,只配置公共的,模块相关的请在模块内部进行生成,在此只配置动态项,减少配置项功能维护工作
  * 例如:工单模块服务访问的地址,只用获取基础url,然后在模块内部根据 基础url+服务访问路由 来获取api地址 ,开发期间亦只需
  *      在自己模块内修改模块服务地址路径即可无需修改公共使用的配置文件,减少冲突及提交自己设置配置影响其他功能的可能性
  *      baseUrl:主页/域名地址
  *      apiEndpoint: 统一网管服务地址
  */
 // 本地开发环境配置
 const development = {
     baseUrl: 'http://dev.sreworks.net/',
     apiEndpoint: 'http://dev.sreworks.net/',
     gateway: 'http://dev.sreworks.net/'
 };
 // 日常环境配置项
 const daily = {
     baseUrl: 'http://dev.sreworks.net/',
     apiEndpoint: 'http://dev.sreworks.net/',
     gateway: 'http://dev.sreworks.net/'
 };
 // mocks 环境配置
 const mocks = {
     baseUrl: ''
 };
 // 预发环境配置项
 const prepub = {
     baseUrl: '',
     apiEndpoint: '',
     gateway: ''
 };
 // 生产环境配置项
 const production = {
     baseUrl: '',
     apiEndpoint: '',
     gateway: ''
 };
 
 // 根据不同的环境生成运行时配置项
 let deployEnv = process.env.DEPLOY_ENV || 'local';
 if (deployEnv) {
     if (deployEnv === 'local') {
         Object.assign(properties, development);
     } else if (deployEnv === 'daily') {
         Object.assign(properties, daily);
     } else if (deployEnv === 'mocks') {
         Object.assign(properties, mocks);
     } else if (deployEnv === 'prepub') {
         Object.assign(properties, prepub);
     } else if (deployEnv === 'production') {
         Object.assign(properties, production);
         //对外是根据部署时候动态生成的配置来决定访问路径的
         if (window.GlobalBackendConf) {
             Object.assign(properties, window.GlobalBackendConf['production']);
         }
     }
 } else {
     console.error('未指定环境变量');
     process.exit(0);
 }
 
 //增加本地访问模式的apiEndpoint动态方式
 if (properties.apiType === 'local') {
     let { href, origin, hash } = window.location;
     //分为路径中存在hash和不存在hash两种情况
     if (href.includes("#")) {
         properties.apiEndpoint = href.substring(0, href.indexOf(hash));
     } else {
         if (href.endsWith("/")) {
             properties.apiEndpoint = href;
         } else {
             properties.apiEndpoint = href + "/";
         }
     }
     //去除spm埋点污染
     let apiEndpoint = properties.apiEndpoint;
     let cIndex = apiEndpoint.indexOf("?spm=");
     if (cIndex > 0) {
         properties.apiEndpoint = apiEndpoint.substring(0, cIndex);
     }
 }
 
 //导出给组件库使用
 window.__TESLA_COMPONENT_LIB_CONFIG = properties;
 window.__SREWORKS_COMPONENT_LIB_CONFIG = properties;
 
 export default properties;
 