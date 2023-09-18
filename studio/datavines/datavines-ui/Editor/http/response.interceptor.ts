/* eslint-disable prefer-promise-reject-errors */
import { IResponseInterface } from './type';

export const responseOnSuccess = (response: IResponseInterface) => {
    const { data } = response;
    if (data) {
        if (data instanceof Blob) {
            return {
                success: true,
                data,
            };
        }
        if (data.code === 200) {
            return data;
        }
    }
    return Promise.reject({
        msg: data?.msg,
        code: data?.code,
    });
};
export const responseOnError = (error: any):any => {
    console.log(error);
    // eslint-disable-next-line no-underscore-dangle
    if (error?.__CANCEL__) {
        return;
    }
    return Promise.reject({
        msg: error?.msg || 'interface error',
        code: null,
    });
};
