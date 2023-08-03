import { notification } from 'antd';
import { create } from './create';
import { IRequestConfig } from './type';

type IHttpFn = {
    <T = any>(
        url: string,
        params?: any,
        options?: {
            hideError?: boolean;
            showWholeData?: boolean;
            [key: string]: any,
        },
    ): Promise<T>;
};

export type THttp = Record<TMethods, IHttpFn>;
type TMethods = 'get' | 'post' | 'delete' | 'put';
const methods = ['get', 'post', 'delete', 'put'];

const defaultOptions = {
    hideError: false,
    showWholeData: false,
};

const getHttp = (_config: IRequestConfig): THttp => {
    const $http = create(_config);
    return methods.reduce((map: any, method: string) => {
        map[method] = (url: string, params?: any, config?: any) => {
            const optionsData = { ...defaultOptions, ...(config || {}) };
            const { hideError, showWholeData, ...rest } = optionsData;
            return new Promise((resolve, reject) => {
                // @ts-ignore
                $http[method](url, params, rest).then(
                    (res: any) => {
                        try {
                            resolve(showWholeData ? res : res?.data);
                        } catch (e) {
                            console.log('e', e);
                        }
                    },
                    (e: { msg: string; code: string | number }) => {
                        if (!hideError && e && e.msg) {
                            notification.error({
                                message: '',
                                description: e.msg,
                            });
                        }
                        reject(e);
                    },
                );
            });
        };
        return map;
    }, {}) as THttp;
};

export {
    create,
    getHttp,
};
