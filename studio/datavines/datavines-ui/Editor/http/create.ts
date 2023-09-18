import axios from 'axios';
import { ICreate, IRequestConfig } from './type';
import { requestInterceptor } from './request.interceptor';
import { responseOnError, responseOnSuccess } from './response.interceptor';

const defaultHeaders = {
    'X-Requested-With': 'XMLHttpRequest',
    'Cache-Control': 'no-cache',
    'Content-Type': 'application/json;charset=utf-8',
    Accept: 'application/json;version=3.0;compress=false;',
};

const defaultConfig = {
    method: 'get',
    baseURL: '/',
    withCredentials: true,
    timeout: 30 * 1000,
    responseType: 'json',
    maxContentLength: 1024 * 1024 * 1024,
    filter: false,
    cache: false,
    overrideDefaultRequestInterceptor: false,
    overrideDefaultResponseInterceptor: false,
};

const create: ICreate = (config) => {
    const { headers, ...rest } = config;
    const mergeConfig = {
        ...defaultConfig,
        headers: {
            ...defaultHeaders,
            ...(headers || {}),
        },
        ...rest,
    } as IRequestConfig;
    const http = axios.create(mergeConfig);
    if (typeof config.requestInterceptor === 'function') {
        http.interceptors.request.use(config.requestInterceptor);
    }
    if (typeof config.responseInterceptor === 'function') {
        http.interceptors.response.use(config.responseInterceptor);
    }
    if (!config.overrideDefaultRequestInterceptor) {
        http.interceptors.request.use(requestInterceptor);
    }
    if (!config.overrideDefaultResponseInterceptor) {
        http.interceptors.response.use(responseOnSuccess, responseOnError);
    }
    http.get = (url = '', data = {}, getConfig = {}) => http.request({ url, params: data, ...(getConfig || {}) });
    http.delete = (url = '', data = {}, deleteConfig = {}) => http.request({
        url, method: 'delete', data, ...(deleteConfig || {}),
    });
    return http;
};

export { create };
