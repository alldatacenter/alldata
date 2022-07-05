import React from 'react';
import ReactDOM from 'react-dom';

import { LocaleProvider } from 'antd';
import zhCN from 'antd/lib/locale-provider/zh_CN';

import App from './views/App';


// 生产环境修改接口origin
// import Interface from './js/interface';
// Interface.origin = '';

ReactDOM.render(
    <LocaleProvider locale={zhCN}><App/></LocaleProvider>,
  	document.getElementById('root')
);