import { StoreEnhancer } from '@reduxjs/toolkit';

const injectReducerEnhancer = (createReducer): StoreEnhancer => {
  return createStore =>
    (...args) => {
      const store = createStore(...args);

      return {
        ...store,
        createReducer,
        injectedReducers: {},
      };
    };
};

export default injectReducerEnhancer;
