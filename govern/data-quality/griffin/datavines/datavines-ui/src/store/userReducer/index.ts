import { Dispatch, Action } from 'redux';
import { useActions } from '../useActions';

export interface ILoginInfo {
    email?: string,
    admin?: boolean,
    id?: string,
    username?: string,
    token?: string,
    phone?: string,
}

export interface UserReducer {
    loginInfo: ILoginInfo
}

type TAction = {
    type?: 'save_login',
    payload?: Partial<UserReducer>
}
type TDispatch = Dispatch<Action<TAction['type']>>;

const initialState: UserReducer = {
    loginInfo: {},
};

export const userActionsMap = {
    setLoginInfo: (loginInfo: ILoginInfo) => (dispatch: TDispatch) => {
        dispatch({
            type: 'save_login',
            payload: loginInfo,
        });
    },
};

export const useUserActions = () => useActions(userActionsMap);

const userReducer = (state: UserReducer = initialState, action: TAction = {}) => {
    switch (action.type) {
        case 'save_login':
            return { ...state, loginInfo: action.payload };
        default:
            return state;
    }
};

export default userReducer;
