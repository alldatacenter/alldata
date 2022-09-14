import httpClient from './httpClient';
import safeEval from './SafeEval';
import * as util from './utils'

const loadChartDataRequest = (props = {}, widgetConfig = {}) => {
    let { dataSourceMeta = {} } = widgetConfig;
    let { nodeParams = {} } = props;
    let reqParams = {};
    let renderSource = util.renderTemplateJsonObject(dataSourceMeta, nodeParams);
    let { beforeRequestHandler, api, method, params = {}, } = renderSource;
    if (beforeRequestHandler && beforeRequestHandler.length > 50) {
        reqParams = safeEval("(" + beforeRequestHandler + ")(nodeParams,reqParams)", { nodeParams: nodeParams, reqParams: params });
    }
    reqParams = {
        ...params,
        ...reqParams
    }
    // reqParams = Object.assign(params,reqParams)
    if (method === 'POST') {
        return httpClient.post(api, reqParams);
    } else {
        return httpClient.get(api, { params: reqParams })
    }
}
export async function loadChartData(props = {}, widgetConfig = {}) {
    let promiseData = undefined;
    let { dataSourceMeta = {} } = widgetConfig;
    let { nodeParams = {} } = props;
    let renderSource = util.renderTemplateJsonObject(dataSourceMeta, nodeParams);
    let { afterResponseHandler } = renderSource;
    let respData = await loadChartDataRequest(props, widgetConfig);
    if (afterResponseHandler && afterResponseHandler.length > 50) {
        respData = safeEval("(" + afterResponseHandler + ")(respData,nodeParams,httpClient)", { respData: respData, nodeParams: nodeParams, httpClient: httpClient });
    }
    promiseData = respData;
    return promiseData;
}