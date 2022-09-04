/**
 * Created by caoshuaibiao on 2018-02-07 23:43
 * 缓存资源库可支持 本地持久缓存、页面缓存、服务器缓存等方式屏蔽浏览器底层的实现
 **/

import localforage from 'localforage';
//环境
const ENV_CACHE_KEY__SUFFIX = "_ABM_CURRENT_ENV";
//角色
const ROLE_CACHE_KEY__SUFFIX = "_ABM_CURRENT_ROLE";
//样式
const THEME_CACHE_KEY__SUFFIX = "";
//SREWorks
const SRE_BIZ_ID = "SRE_BIZ_ID_";

class CacheRepository {

    getRole(product) {
        return localStorage.getItem(product + ROLE_CACHE_KEY__SUFFIX);
    }

    setRole(product, role) {
        localStorage.setItem(product + ROLE_CACHE_KEY__SUFFIX, role);
    }

    getProduct(product) {
        const cacheKey = product + "_" + this.getRole(product) + "_" + this.getEnv(product);
        return localforage.getItem(cacheKey);
    }

    setProduct(product, data) {
        const cacheKey = product + "_" + this.getRole(product) + "_" + this.getEnv(product);
        return localforage.setItem(cacheKey, data);
    }

    getEnv(product) {
        return localStorage.getItem(product + ENV_CACHE_KEY__SUFFIX)
    }

    setEnv(product, env) {
        localStorage.setItem(product + ENV_CACHE_KEY__SUFFIX, env);
    }
    getBizEnv(app) {
        let bizArr = localStorage.getItem(SRE_BIZ_ID + app).split(',') || [];
        return bizArr[2] || 'dev';
    }
    getAppBizId(app) {
        return localStorage.getItem(SRE_BIZ_ID + app);
    }

    setAppBizId(app, namespaceId, stageId) {
        let bizId = `${app},${namespaceId},${stageId}`
        localStorage.setItem(SRE_BIZ_ID + app, bizId);
    }

}

const cacheRepository = new CacheRepository();
export default cacheRepository;