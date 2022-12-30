/**
 * Created by caoshuaibiao on 2018-08-12 23:44
 * http请求封装,其中post默认以json方式提交
 **/
import React from 'react';
import axios from 'axios';
import debounce from 'lodash.debounce';
import Qs from 'qs';
import { message, notification } from 'antd';
import localeHelper from './localeHelper';
import properties from 'appRoot/properties';
import _ from 'lodash';

let request = axios.create({
    //timeout:15000,
    withCredentials: true
});

let interceptor = [], ignoreExceptionUrls = {};

function getTargetUrl(config) {
    let targetUrl = "";
    let apiEndpoint = config.apiEndpoint || properties.apiEndpoint;
    config.url = config.url || '';
    if (config.url.startsWith("http:") || config.url.startsWith("https:")) {     // 如果配置的url以http或https开头，则不在前面加apiEndpoint
        targetUrl = config.url;
    } else if (apiEndpoint.endsWith("/") && config.url.startsWith("/")) {
        targetUrl = apiEndpoint + config.url.substring(1);
    } else {
        targetUrl = apiEndpoint + config.url;
    }
    return targetUrl;
}
if (process.env.NODE_ENV !== 'development') {
    //增加请求api统一地址拦截
    request.interceptors.request.use(function (config) {
        config.url = config.url || '';
        let targetUrl = getTargetUrl(config);
        return {
            ...config,
            url: targetUrl
        };
    }, function (error) {
        return Promise.reject(error);
    });
}
function showErrorDetails(res) {
    function syntaxHighlight(json) {
        if (typeof json !== 'string') {
            json = JSON.stringify(json, undefined, 2);
        }
        json = json.replace(/&/g, '&').replace(/</g, '<').replace(/>/g, '>');
        return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
            function (match) {
                let cls = 'number';
                if (/^"/.test(match)) {
                    if (/:$/.test(match)) {
                        cls = 'key';
                    } else {
                        cls = 'string';
                    }
                } else if (/true|false/.test(match)) {
                    cls = 'boolean';
                } else if (/null/.test(match)) {
                    cls = 'null';
                }
                return '<span class="' + cls + '">' + match + '</span>';
            }
        );
    }
    let requestDetails = syntaxHighlight(res.config), respDetails = syntaxHighlight(res.data);
    let errorWin = window.open("", "_blank");
    let doc = errorWin.document;
    doc.open();
    doc.write(`<html lang="en"><head><meta charset="UTF-8"><title>Error Console</title></head>
<style>
.content {width: 100%;height: auto;margin: 0 auto;background-color: black;color: white;}
pre {padding: 5px; margin: 5px; }.string { color: green;white-space: pre-line } .number { color: darkorange; }.boolean { color: blue; }.null { color: magenta; }.key { color: red; }            
</style>
<body style="background-color: black"><div class="content">
<label>Request Details:</label>
<pre style="font-size: 12px">
${requestDetails}
</pre>
<label>Error Details:</label>
<pre style="font-size: 12px">
${respDetails}
</pre>
</div></body></html>`);
    doc.close();
}

const ExceptionMessage = ({ res }) => {
    if (res.data && res.data.message_type === 'success') {
        return (
            <div style={{ fontSize: 12, marginLeft: -38 }}>
                <span style={{ wordWrap: 'break-word', wordBreak: 'break-all' }}>{res.data.message}</span><br />
            </div>
        )
    }
    let { message = "" } = res.data || {};
    if (message && message.length > 120) {
        message = message.substring(0, 120) + "...";
    }
    return (
        <div style={{ fontSize: 12, marginLeft: -38, marginTop: 5 }}>
            <span style={{ wordWrap: 'break-word', wordBreak: 'break-all' }}>{message}</span>
            <br />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 5 }}>
                {
                    properties.envFlag === properties.ENV.Internal ?
                        <span>请求ID:{res.data && res.data.requestId}</span>
                        : null
                }
                {
                    properties.hiddenErrorDetails ? null :
                        <a onClick={() => showErrorDetails(res)}>{">>>Details<<<"}</a>
                }
            </div>
        </div>
    )
};

//对发生多次错误提示的进行节流提示
const showExceptionNotification = debounce(function (res) {
    let { status } = res, nType = 'error';
    let nCode = parseInt(status);
    if (nCode === 200) {
        nCode = parseInt(res.data.code);
    }
    if ((nCode >= 400 && nCode <= 499) || (nCode >= 4000 && nCode <= 4999)) {
        nType = "warning";
    }
    if (res.data && res.data.message_type) {
        nType = res.data.message_type;
    }
    notification[nType]({
        message: nType.toUpperCase(),
        description: <ExceptionMessage res={res} />
    });
}, 1000);

const showConstExceptionNotification = debounce(function (title, message) {
    notification.error({
        message: title.toUpperCase(),
        description: <div style={{ fontSize: 12, marginLeft: -38, marginTop: 5 }}>{message}</div>
    });
}, 500);

function isIgnoreException(reqConfig) {
    let { url='' } = reqConfig;
    //存在带参数的url需要去掉参数
    let urlFix = url.indexOf("?");
    if (urlFix > 0) {
        url = url.substring(0, urlFix);
    }
    return ignoreExceptionUrls[url];
}
//定义全局默认的数据返回处理
request.interceptors.response.use(
    res => {//请求成功后,等于200默认业务成功直接返回数据,其他的code分发到模块自己处理
        if (res.status === 200) {
            let resData = res.data;
            if (resData.code) {//有data code的场景启动公共拦截及提示
                resData.code = parseInt(resData.code);
                if (resData.code === 200) {
                    return resData.data;
                } else if (resData.code === 401) {
                    if (properties.envFlag === properties.ENV.Internal) {
                        window.location.href = properties.baseUrl + '/api-proxy/auth/login?backurl=' + encodeURIComponent(window.location.href);
                        return Promise.reject(resData);
                    } else {
                        if (resData.data && resData.data.loginUrl) {
                            window.location.href = resData.data.loginUrl;
                            return Promise.reject(resData);
                        } else {
                            return Promise.resolve(resData);
                        }
                    }
                } else if (resData.code === 403) {
                    notification.warning({
                        message: 'WARNING',
                        description: <div style={{ fontSize: 12, marginLeft: -42 }}>{"No Permission,Please contact administrator,message:" + resData.message}</div>
                    });
                    return Promise.resolve({ code: 403 });
                } else {
                    let handleResults = [];
                    interceptor.forEach(inter => {
                        handleResults.push(inter.handleException(resData));
                    });
                    handleResults = handleResults.filter(exception => exception);
                    //TODO 临时展示返回错误信息,后续需要根据返回的异常业务处理结果来展示
                    if (handleResults && handleResults.length > 0) {
                        let result = handleResults[0];
                        if (result.notification) {
                            notification.warning({
                                message: 'WARNING',
                                description: result.message
                            });
                            return Promise.reject(resData);
                        } else {
                            return Promise.resolve(result);
                        }
                    } else {
                        if (isIgnoreException(res.config || {})) {
                            return Promise.reject(resData);
                        }
                        showExceptionNotification(res);
                        return Promise.reject(resData);
                    }
                }
            }
            //需要兼容老的接口,没有data.code的场景,不拦截直接返回
            return res;
        }
    },
    err => {//请求发生异常的统一处理
        if (isIgnoreException(err.config || {})) {
            return Promise.reject(err);
        }
        if (err && err.response) {
            let errorStatus = err.response.status;
            if (errorStatus === 401) {
                if (properties.envFlag === properties.ENV.Internal) {
                    window.location.href = properties.baseUrl + '/api-proxy/auth/login?backurl=' + encodeURIComponent(window.location.href);
                    return Promise.reject({ code: 401 });
                } else {
                    //return Promise.resolve({code:401});
                    //登录环境发生异常,造成的401问题,进行提示
                    showConstExceptionNotification('Error', "Not logged in or no permission,Please contact administrator");
                    return Promise.reject(err);
                }
            } else if (errorStatus === 404) {
                showConstExceptionNotification('Error', "The Service is not found,Please contact administrator");
                return Promise.reject(err);
            } else if (errorStatus === 502) {
                showConstExceptionNotification('Error', "The Gateway is error,Please contact administrator");
                return Promise.reject(err);
            } else if (errorStatus === 405) {
                showConstExceptionNotification('Error', "Method Not Allowed,Please contact administrator");
                return Promise.reject(err);
            } else if (errorStatus === 504) {
                showConstExceptionNotification('Error', "Gateway Timeout,Please contact administrator");
                return Promise.reject(err);
            } else {
                showExceptionNotification(err.response);
                return Promise.reject(err);
            }
        } else {
            showConstExceptionNotification('Error', (err.message ? err.message : "Unknown error") + ",Please contact administrator");
            return Promise.reject(err);
        }
    }
);

let reqInterceptors = [];

class HttpClient {


    /**
     * 添加请求拦截器
     * @param handler
     * @param errorHandler
     */
    addRequestInterceptor(handler, errorHandler) {
        let interceptorId = request.interceptors.request.use(handler, errorHandler);
        reqInterceptors.push(interceptorId);
        return interceptorId;
    }
    /**
     * 添加响应拦截器
     * @param handler
     * @param errorHandler
     */
    addResponseInterceptor(handler, errorHandler) {
        return request.interceptors.response.use(handler, errorHandler);
    }
    /**
     * 移除请求拦截器
     * @param handler
     * @param errorHandler
     */
    removeRequestInterceptor(handler, errorHandler) {
        request.interceptors.request.eject(handler, errorHandler);
    }
    /**
     * 移除响应拦截器
     * @param handler
     * @param errorHandler
     */
    removeResponseInterceptor(handler, errorHandler) {
        request.interceptors.response.eject(handler, errorHandler);
    }

    /**
     * 添加自定义的业务拦截器,httpCode为200,但里面返回的业务code为自定义业务code,非401,403,500,200.
     */
    addCustomBusinessInterceptor(handler) {
        if (!interceptor.includes(handler)) {
            interceptor.push(handler);
        }
    }

    /**
     * 清空请求拦截器
     */
    clearRequestInterceptors() {
        reqInterceptors.forEach(inter => {
            request.interceptors.request.eject(inter)
        })
    }

    /**
     * 以标准form的数据格式提交
     * @param url
     * @param data
     * @param config
     */
    postFormData(url, data, config) {
        return new Promise((resolve, reject) => {
            request(
                {
                    method: 'POST',
                    headers: { 'content-type': 'application/x-www-form-urlencoded' },
                    data: Qs.stringify(data),
                    url,
                    ...config
                }
            ).then(data => {
                resolve(data);
            });
        })

    }

    /**
     * 兼容迁移已有功能的按照标准form提交数据
     * @param url
     * @param config
     * @returns {Promise}
     */
    originalPost(url, config) {
        return new Promise((resolve, reject) => {
            request(
                {
                    method: 'post',
                    url: url,
                    ...config,
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    transformRequest: [function (data, headers) {
                        return Qs.stringify(data)
                    }],
                }
            ).then(data => {
                resolve(data);
            });
        })
    }

    /**
     * 注册异常忽略url,当注册过的url发生请求异常不进行拦截提示
     * @param url  string|array
     */
    registerIgnoreExceptionUrl(url) {
        if (!Array.isArray(url)) {
            url = [url];
        }
        url.forEach(l => {
            let targetUrl = getTargetUrl({ url: l });
            ignoreExceptionUrls[targetUrl] = true;
        });
    }
}

const httpClient = new HttpClient();

//初始化一些通用方法

['delete', 'get', 'head', 'options', 'originalGet'].forEach(function (method) {
    httpClient[method] = function (url, config) {
        return request[method === 'originalGet' ? 'get' : method](url, config).then(resp => {
            return resp;
        })
    };
});

['post', 'put', 'patch'].forEach(function (method) {
    httpClient[method] = function (url, data, config) {
        return request[method](url, data, config).then(resp => {
            return resp;
        })
    };
});

/**
 * 并发请求支持,参数为所有请求的数组
 */
httpClient['all'] = ([...fun]) => {
    return axios.all([...fun]).then(axios.spread(function (...res) {
        return res
    }))
};

export default httpClient;



