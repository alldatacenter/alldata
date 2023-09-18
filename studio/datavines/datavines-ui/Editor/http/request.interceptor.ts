import axios from 'axios';
import { IRequestConfig } from './type';

export const source = axios.CancelToken.source();

export const requestInterceptor = (_config: IRequestConfig): IRequestConfig => {
    const config = _config;
    const method = (config.method || '').toLocaleLowerCase();
    config.cancelToken = source.token;
    if (method === 'get') {
        config.params.st = new (Date as any)() * 1;
    }
    if (config.filter) {
        const data = config.data || {};
        // eslint-disable-next-line no-restricted-syntax
        for (const key in data) {
            // eslint-disable-next-line no-prototype-builtins
            if (data.hasOwnProperty(key)) {
                const item = data[key];
                if (item === null || item === undefined) {
                    delete data[key];
                }
            }
        }
    }
    if (method === 'get' && config.data) {
        config.params = config.data;
        delete config.data;
    }
    return config;
};
