import { useReducer } from 'react';
import { getHttp, THttp } from '../../http';
import { useImmutable, usePersistFn } from '../../common';
import { useEditorContextState } from '../../store/editor';

export type TOption = {
    url: string,
    method: 'post' | 'get' | 'put' | 'delete',
    params?: Record<string, any>
}

type TConfig = {
    hideError?: boolean;
    showWholeData?: boolean;
    [key: string]: any,
}

export type TUseRequestResult<T> = {
    loading: boolean,
    result: T,
    error: Error | undefined,
    run: (option: TOption, config: TConfig) => Promise<T | undefined>
    $http: THttp,
}

const useRequest = <T = any>(): TUseRequestResult<T> => {
    const [{ headers, baseURL }] = useEditorContextState();
    const initValue = { loading: false, result: undefined, error: undefined };

    const [value, dispatch] = useReducer((state: any, action: any) => ({ ...state, ...action }), initValue);
    const $http = useImmutable(getHttp({ baseURL, headers: headers || {} }));

    const run = usePersistFn(async (option: TOption, config: TConfig) => {
        const { params, url } = option;
        const method = option.method || 'get';
        if (value.loading) {
            return;
        }
        dispatch({ loading: true });
        try {
            const res = await $http[method](url, params || {}, config);
            dispatch({ loading: false, result: res, error: undefined });
            return res;
        } catch (error) {
            dispatch({ loading: false, result: undefined, error });
            return undefined;
        }
    });

    return {
        $http,
        run,
        ...value,
    };
};

export default useRequest;
