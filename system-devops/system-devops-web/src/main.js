import React from 'react';
import ReactDOM from 'react-dom';

import { LocaleProvider } from 'antd';
import zhCN from 'antd/lib/locale-provider/zh_CN';

import { AppContainer } from 'react-hot-loader';
// AppContainer 是一个 HMR 必须的包裹(wrapper)组件

import App from './views/App';

let counter = 1;

const render = () => {
	ReactDOM.render(
	    <AppContainer>
        	<LocaleProvider locale={zhCN}><App counter={counter}/></LocaleProvider>
	    </AppContainer>,
	    document.getElementById('root')
	);
};

render();

// 模块热替换的 API
if (module.hot) {
  	module.hot.accept('./views/App', () => {
		counter++;
    	render();
  	});
}