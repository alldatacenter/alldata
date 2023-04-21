import { request } from 'umi';
export async function loginAPI(options?: { [key: string]: any }) {
    return request<API.stringResult>('/apiPre/acc/doLogin', {
        method: 'GET',
        params: {
            ...options,
        },
        headers: {
        'Content-Type': 'application/json',
        }
    });
}

export async function logoutAPI(options?: { [key: string]: any }) {
    return request<API.stringResult>('/apiPre/acc/logout', {
        method: 'GET',
        params: {
            ...options,
        },
        headers: {
        'Content-Type': 'application/json',
        }
    });
}