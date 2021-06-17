// 开发环境的store
import { createStore, applyMiddleware, compose } from 'redux'
import reducer from '../reducers/main';

// import thunk from './middleware/thunk'
import DevTools from '../devTools/DevTools'

const enhancer = compose(
  //你要使用的中间件，放在前面
  //   applyMiddleware(thunk),
  //必须的！启用带有monitors（监视显示）的DevTools
  DevTools.instrument()
)

export default function createStoreWithMiddleware(initialState) {
  //注意：仅仅只有redux>=3.1.0支持第三个参数
  const store = createStore(reducer, initialState, enhancer);
  return store;
}
