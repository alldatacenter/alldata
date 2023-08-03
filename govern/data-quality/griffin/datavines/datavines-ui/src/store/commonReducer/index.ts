import { Dispatch, Action } from 'redux';
import Cookies from 'js-cookie';
import { useActions } from '../useActions';

export interface CommonReducer {
    prefixCls: 'dark' | undefined // antd prefixxCls
    locale: 'zh_CN' | 'en_US',
    isDetailPage: boolean,
}
type TActionType =
| 'save_prefixCls'
| 'save_locale'
| 'set_is_detail_page'

type TAction = {
    type?: TActionType,
    payload?: Partial<CommonReducer>
}
type TDispatch = Dispatch<Action<TAction['type']>>;

const initialState: CommonReducer = {
    prefixCls: undefined,
    locale: 'en_US',
    isDetailPage: window.location.href.indexOf('/main/detail') > -1,
};

export const commonActionsMap = {
    setPrefixCls: (prefixCls: CommonReducer['prefixCls']) => (dispatch: TDispatch) => {
        dispatch({
            type: 'save_prefixCls',
            payload: prefixCls,
        });
    },
    togglePrefixCls: (prefixCls: CommonReducer['prefixCls']) => (dispatch: TDispatch) => {
        dispatch({
            type: 'save_prefixCls',
            payload: prefixCls,
        });
    },

    setLocale: (locale: CommonReducer['locale']) => (dispatch: TDispatch) => {
        dispatch({
            type: 'save_locale',
            payload: locale,
        });
    },

    setIsDetailPage: (isDetailPage: boolean) => (dispatch: TDispatch) => {
        dispatch({
            type: 'set_is_detail_page',
            payload: isDetailPage,
        });
    },
};

export const useCommonActions = () => useActions(commonActionsMap);

const commonReducer = (state: CommonReducer = initialState, action: TAction = {}) => {
    switch (action.type) {
        case 'save_prefixCls':
            return { ...state, prefixCls: action.payload };
        case 'save_locale':
            Cookies.set('language', action.payload);
            return { ...state, locale: action.payload };
        case 'set_is_detail_page':
            return { ...state, isDetailPage: action.payload };
        default:
            return state;
    }
};

export default commonReducer;
