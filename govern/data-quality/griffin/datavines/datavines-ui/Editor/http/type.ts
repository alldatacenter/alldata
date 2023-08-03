import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';

export interface IRequestConfig extends AxiosRequestConfig {
    filter?: boolean;
    cache?: boolean;
    message?: any;
    responseInterceptor?: (response: AxiosResponse) => any;
    requestInterceptor?: (config: IRequestConfig) => IRequestConfig;
    overrideDefaultRequestInterceptor?: boolean;
    overrideDefaultResponseInterceptor?: boolean;
}

export interface ICreate {
    (config: IRequestConfig): IAxiosInterface;
}

export interface IAxiosInterface extends AxiosInstance {
    get: <T = any, R = AxiosResponse<T>>(url: string, params?: any, config?: AxiosRequestConfig) => Promise<R>;
    delete: <T = any, R = AxiosResponse<T>>(url: string, config?: AxiosRequestConfig) => Promise<R>;
}

export interface IDefaultAxiosInterface extends IAxiosInterface {
    create?: ICreate
}

export interface IResponseInterface extends AxiosResponse {
    config: IRequestConfig;
}
