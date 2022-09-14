/**
 * Created by caoshuaibiao on 2020/12/4.
 * 数据源定义,http的ds需要实现http工具库实现定义，用框架的还是用原生的
 */
import * as util from '../../../utils/utils';
import httpClient from '../../../utils/httpClient';
import safeEval from '../../../utils/SafeEval';
import Constants from './Constants';
import { message } from 'antd';
/**
 * 数据源父类,定义数据源的共性行为无实现
 */
export class DataSource {
    /**
     * 唯一标识
     */
    uniqueKey;
    /**
     * 数据源类型,API、CMDB、Mysql等等
     */
    type;
    /**
     * 自定义返回结果处理
     */
    customResultHandler = [];
    /**
     * 自定义请求前处理
     */
    customRequestHandler = [];

    constructor(sourceMeta) {
        //Object.assign(this,sourceMeta);
        this.sourceMeta = sourceMeta;
    }

    /**
     * 获取数据
     * @param runtimeParams 运行时需要动态追加的参数
     * @returns {Promise}
     */
    query(nodeParams) {
        return new Promise(
            function (resolve, reject) {
                return reject({ success: false, message: "无DataSource实现" });
            }
        );
    }

    /**
     * 获取数据源所需的参数
     * @param runtimeParams  运行时动态参数,一般来源与用户的交互操作,如table的分页点击这些
     */
    getParams(runtimeParams) {

    }

    /**
     * 序列化为JSON
     */
    toJSON() {

    }

}
/**
 * api类型的数据源
 */
export class ApiDataSource extends DataSource {

    constructor(sourceMeta) {
        super(sourceMeta);
    }
    query(nodeParams) {
        let renderSource = util.renderTemplateJsonObject(this.sourceMeta, nodeParams);
        let { beforeRequestHandler, afterResponseHandler, api, method, params } = renderSource;
        //长度进行了判断是因为存在模板函数
        let reqParams = {};
        if (beforeRequestHandler && beforeRequestHandler.length > 50) {
            reqParams = safeEval("(" + beforeRequestHandler + ")(nodeParams)", { nodeParams: nodeParams });
            params = { ...params, ...reqParams }
        }
        let request = null;
        if (method === 'GET' && api) {
            request = httpClient.get(api, { params });
        } else if (method === 'POST' && api) {
            request = httpClient.post(api, params);
        }
        if (request) {
            return request.then(respData => {
                if (afterResponseHandler && afterResponseHandler.length > 50) {
                    return safeEval("(" + afterResponseHandler + ")(respData,nodeParams,httpClient)", { respData: respData, nodeParams: nodeParams, httpClient: httpClient });
                }
                return respData;
            })
        } else {
            return null;
        }
    }

}


/**
 * 源于系统cmdb的数据源
 */
export class CmdbDataSource extends ApiDataSource {

}

/**
 * 自定义function数据源
 */
export class FunctionDataSource extends DataSource {

    constructor(sourceMeta) {
        super(sourceMeta);
    }
    query(nodeParams) {
        let { } = this.sourceMeta
        try{
            if (this.sourceMeta.function) {
                return safeEval("(" + this.sourceMeta.function + ")(nodeParams)", { nodeParams: nodeParams });
            }
        } catch(err) {
            message.warning('格式有误!')
        }
        return null;
    }
}

/**
 * 静态json数据源
 */
export class JSONDataSource extends DataSource {
    constructor(sourceMeta) {
        super(sourceMeta);
    }
    query(nodeParams) {
        let json = {}
        try {
            let renderSource = util.renderTemplateJsonObject(this.sourceMeta, nodeParams);
            let json = renderSource.JSON || null;
            if (typeof json === 'string') {
                json = JSON.parse(json);
            }
            return json;
        } catch(err) {
            json = {}
            message.warning("数据格式有误!")
        }
    }
}

export default class DataSourceAdaptor {

    dataSource;

    constructor(sourceMeta) {
        let { type } = sourceMeta;
        if (type === Constants.DATASOURCE_API) {
            this.dataSource = new ApiDataSource(sourceMeta)
        } else if (type === Constants.DATASOURCE_JSONDATA) {
            this.dataSource = new JSONDataSource(sourceMeta);
        }
        else if (type === Constants.DATASOURCE_FUNCTION) {
            try {
                this.dataSource = new FunctionDataSource(sourceMeta);
            } catch (error) {
                this.dataSource = [];
            }
        }
    }

    query(nodeParams) {
        return Promise.resolve(this.dataSource && this.dataSource.query(nodeParams));
    }

}
