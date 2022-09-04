/**
 * Created by caoshuaibiao on 2018-02-07 23:10
 * 公共实体基类 继承此类实体将会有基础的保存、修改、删除、加载等操作
 * 规约：继承的实体必须实现getUrl(),返回当前定义的实体对应后台的url地址,及后台必须实现对此实体restful接口
 **/

import httpClient from './httpClient';
import localeHelper from '../utils/localeHelper';

class BaseEntity {

    constructor(modeData) {
        if (!this.getUrl()) throw new Error(localeHelper.get('common.realizeUrlMsg', "请实现实体对应的服务url方法"));
        Object.assign(this, modeData)
    }

    /**
     * 模拟抽象方法,要求子类必须实现
     */
    getUrl() {
        return false;
    }

    save() {
        return httpClient.post(this.getUrl(), this);
    }

    load() {
        return httpClient.get(this.getUrl(), {
            params: {
                id: this.id
            }
        }).then(modeData => {
            return Object.assign(this, modeData);
        });
    }

    update() {
        return httpClient.put(this.getUrl(), this);
    }

    delete() {
        return httpClient.delete(this.getUrl(), {
            params: {
                id: this.id
            }
        });
    }

}


export default BaseEntity;
