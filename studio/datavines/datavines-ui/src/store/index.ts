import {
    applyMiddleware, combineReducers, legacy_createStore as createStore,
} from 'redux';
import { useSelector as nativeSelector, useStore as naviveStore } from 'react-redux';
import thunk from 'redux-thunk';
import { composeWithDevTools } from 'redux-devtools-extension';
import logger from 'redux-logger';
import rootReducer, { RootReducer } from './rootReducer';

export * from './rootReducer';

const store = createStore(
    combineReducers(rootReducer),
    process.env.NODE_ENV === 'development' ? composeWithDevTools(applyMiddleware(thunk, logger)) : applyMiddleware(thunk),
);
export const useStore = () => naviveStore<RootReducer>().getState();

export const useSelector = <T>(fn: (state: RootReducer) => T) => fn(nativeSelector((selector: RootReducer) => selector));

export const useLoginInfo = () => {
    const { loginInfo } = useSelector((r) => r.userReducer);
    return loginInfo;
};

export default store;
