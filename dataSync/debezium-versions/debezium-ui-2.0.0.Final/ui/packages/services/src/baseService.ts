/**
 * @license
 * Copyright 2020 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import {LoggerService} from "./logger";
import {ConfigService} from "./config";
import axios, { AxiosRequestConfig } from 'axios';
import {ContentTypes} from "@debezium/ui-models";

/**
 * Interface implemented by all services.
 */
export interface Service {
    init(): void;
}


/**
 * Base class for all services.
 */
export abstract class BaseService implements Service {

    protected logger: LoggerService | null = null;
    protected config: ConfigService | null = null;

    private apiBaseHref: string;
    private newApiBaseHref: string;


    public init(): void {
        this.apiBaseHref = this.config?.artifactsUrl() || '';
        this.newApiBaseHref = this.config?.newArtifactsUrl() || '';
        if (this.apiBaseHref.endsWith("/")) {
            this.apiBaseHref = this.apiBaseHref.substring(0, this.apiBaseHref.length - 1);
        }
        if (this.newApiBaseHref.endsWith("/")) {
            this.newApiBaseHref = this.newApiBaseHref.substring(0, this.newApiBaseHref.length - 1);
        }
        this.logger?.debug("[BaseService] Base HREF of REST API: ", this.apiBaseHref);
        this.logger?.debug("[BaseService] Base HREF of Connectors REST API: ", this.newApiBaseHref);
    }

    /**
     * Creates an endpoint to use when making a REST call.  Supports path params and query params.
     * @param path
     * @param params
     * @param queryParams
     */
    protected endpoint(path: string, newEndpoint = false, params?: any, queryParams?: any): string {
        if (params) {
            Object.keys(params).forEach(key => {
                const value: string = encodeURIComponent(params[key]);
                path = path.replace(":" + key, value);
            });
        }
        let rval: string = (newEndpoint ? this.newApiBaseHref : this.apiBaseHref) + path;
        if (queryParams) {
            let first: boolean = true;
            for (const key in queryParams) {
                if (queryParams[key]) {
                    const value: string = encodeURIComponent(queryParams[key]);
                    if (first) {
                        rval = rval + "?" + key;
                    } else {
                        rval = rval + "&" + key;
                    }
                    if (value !== null && value !== undefined) {
                        rval = rval + "=" + value;
                    }
                    first = false;
                }
            }
        }
        this.logger?.info("[BaseService] Using REST endpoint: ", rval);
        return rval;
    }

    /**
     * Creates the request options used by the HTTP service when making API calls.
     * @param headers
     */
    protected options(headers: {[header: string]: string}): AxiosRequestConfig {
        const options: AxiosRequestConfig = {headers};
        return options;
    }

    /**
     * Performs an HTTP GET operation to the given URL with the given options.  Returns
     * a Promise to the HTTP response data.
     */
    protected httpGet<T>(url: string, options?: AxiosRequestConfig, successCallback?: (value: any) => T): Promise<T> {
        this.logger?.info("[BaseService] Making a GET request to: ", url);

        if (!options) {
            options = this.options({ "Accept": ContentTypes.APPLICATION_JSON });
        }

        const config: AxiosRequestConfig = this.axiosConfig("get", url, options);
        return axios.request(config)
            .then(response => {
                const data: T = response.data;
                if (successCallback) {
                    return successCallback(data);
                } else {
                    return data;
                }
            }).catch(error => {
                return Promise.reject(this.unwrapErrorData(error));
            });
    }

    /**
     * Performs an HTTP POST operation to the given URL with the given body and options.  Returns
     * a Promise to null (no response data expected).
     * @param url
     * @param body
     * @param options
     */
    protected httpPost<I>(url: string, body: I, options?: AxiosRequestConfig, successCallback?: () => void): Promise<void> {
        this.logger?.info("[BaseService] Making a POST request to: ", url);

        if (!options) {
            options = this.options({ "Content-Type": ContentTypes.APPLICATION_JSON });
        }

        const config: AxiosRequestConfig = this.axiosConfig("post", url, options, body);
        return axios.request(config)
            .then(() => {
                if (successCallback) {
                    return successCallback();
                } else {
                    return;
                }
            }).catch(error => {
                return Promise.reject(this.unwrapErrorData(error));
            });
    }

    /**
     * Performs an HTTP POST operation to the given URL with the given body and options.  Returns
     * a Promise to the HTTP response data.
     * @param url
     * @param body
     * @param options
     */
    protected httpPostWithReturn<I, O>(url: string, body: I, options?: AxiosRequestConfig, successCallback?: (data: any) => O): Promise<O> {
        this.logger?.info("[BaseService] Making a POST request to: ", url);

        if (!options) {
            options = this.options({ "Accept": ContentTypes.APPLICATION_JSON, "Content-Type": ContentTypes.APPLICATION_JSON });
        }

        const config: AxiosRequestConfig = this.axiosConfig("post", url, options, body);
        return axios.request(config)
            .then(response => {
                const data: O = response.data;
                if (successCallback) {
                    return successCallback(data);
                } else {
                    return data;
                }
            }).catch(error => {
                return Promise.reject(this.unwrapErrorData(error));
            });
    }

    /**
     * Performs an HTTP PUT operation to the given URL with the given body and options.  Returns
     * a Promise to null (no response data expected).
     * @param url
     * @param body
     * @param options
     */
    protected httpPut<I>(url: string, body: I, options?: AxiosRequestConfig, successCallback?: () => void): Promise<void> {
        this.logger?.info("[BaseService] Making a PUT request to: ", url);

        if (!options) {
            options = this.options({ "Content-Type": ContentTypes.APPLICATION_JSON });
        }

        const config: AxiosRequestConfig = this.axiosConfig("put", url, options, body);
        return axios.request(config)
            .then(() => {
                if (successCallback) {
                    return successCallback();
                } else {
                    return;
                }
            }).catch(error => {
                return Promise.reject(this.unwrapErrorData(error));
            });
    }

    /**
     * Performs an HTTP PUT operation to the given URL with the given body and options.  Returns
     * a Promise to the HTTP response data.
     * @param url
     * @param body
     * @param options
     */
    protected httpPutWithReturn<I, O>(url: string, body: I, options?: AxiosRequestConfig, successCallback?: (data: O) => O): Promise<O> {
        this.logger?.info("[BaseService] Making a PUT request to: ", url);

        if (!options) {
            options = this.options({ "Accept": ContentTypes.APPLICATION_JSON, "Content-Type": ContentTypes.APPLICATION_JSON });
        }

        const config: AxiosRequestConfig = this.axiosConfig("put", url, options, body);
        return axios.request(config)
            .then(response => {
                const data: O = response.data;
                if (successCallback) {
                    return successCallback(data);
                } else {
                    return data;
                }
            }).catch(error => {
                return Promise.reject(this.unwrapErrorData(error));
            });
    }

    /**
     * Performs an HTTP DELETE operation to the given URL with the given body and options.
     * @param url
     * @param options
     */
    protected httpDelete<T>(url: string, options?: AxiosRequestConfig, successCallback?: () => T): Promise<T | null> {
        this.logger?.info("[BaseService] Making a DELETE request to: ", url);

        if (!options) {
            options = {};
        }

        const config: AxiosRequestConfig = this.axiosConfig("delete", url, options);
        return axios.request(config)
            .then(() => {
                return successCallback ? successCallback() : null;
            }).catch(error => {
                return Promise.reject(this.unwrapErrorData(error));
            });
    }

    private axiosConfig(method: string, url: string, options: any, data?: any): AxiosRequestConfig {
        return {...{
                data,
                method,
                url,
                validateStatus: (status) => {
                    return status >= 200 && status < 300;
                }
            }, ...options};
    }

    private unwrapErrorData(error: any): any {
        if (error.response && error.response.data) {
            return error.response.data;
        }
        return error;
    }

}
