import { configureStore } from '@reduxjs/toolkit';
import injectReducerEnhancer from 'utils/@reduxjs/injectReducer/enhancer';
import rejectedErrorHandlerMiddleware from '../utils/@reduxjs/rejectedErrorHandlerMiddleware';
import { createReducer } from './reducers';

export function configureAppStore() {
  const enhancers = [injectReducerEnhancer(createReducer)];

  const store = configureStore({
    reducer: createReducer(),
    middleware: getDefaultMiddleware =>
      getDefaultMiddleware({
        serializableCheck: false,
        // immutableCheck: false,
      }).prepend(rejectedErrorHandlerMiddleware.middleware),
    devTools:
      /* istanbul ignore next line */
      process.env.NODE_ENV !== 'production' ||
      process.env.PUBLIC_URL.length > 0,
    enhancers,
  });

  return store;
}
