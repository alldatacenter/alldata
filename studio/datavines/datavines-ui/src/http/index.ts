import { getHttp } from '@Editor/http';
import { createHashHistory } from 'history';
import shareData from 'src/utils/shareData';
import { DV_STORAGE_LOGIN } from 'src/utils/constants';
import store, { RootReducer } from '@/store';

export const history = createHashHistory();

const refreshToken = (newToken: string) => {
    const { userReducer } = store.getState() as RootReducer;
    const loginInfo = userReducer.loginInfo || {};
    if (loginInfo.token) {
        const newLoginInfo = { ...loginInfo, token: newToken };
        Promise.resolve().then(() => {
            store.dispatch({
                type: 'save_login',
                payload: newLoginInfo,
            });
            shareData.sessionSet(DV_STORAGE_LOGIN, newLoginInfo);
        });
    }
};

export const $http = getHttp({
    baseURL: '/api/v1',
    requestInterceptor(config) {
        const { userReducer, commonReducer } = store.getState() as RootReducer;
        const loginInfo = userReducer.loginInfo || {};
        const { locale } = commonReducer;
        if (loginInfo.token) {
            config.headers.Authorization = `Bearer ${loginInfo.token}`;
        }
        if (locale) {
            config.headers.language = locale;
        }
        return config;
    },
    responseInterceptor(response) {
        if (response.data?.code === 10010002 || response.data?.code === 10010003 || response.data?.code === 10010004) {
            setTimeout(() => {
                window.location.href = '#/login';
            }, 1000);
        }
        const newToken = response?.data?.token;
        if (newToken) {
            refreshToken(newToken);
        }
        return response;
    },
});
