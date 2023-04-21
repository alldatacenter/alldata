import { Settings as LayoutSettings } from '@ant-design/pro-components';
import { Button, Image, Space } from 'antd';
import { divide } from 'lodash';
import React, { ReactNode } from 'react';
import logoImg from '../src/assets/images/ic_launcher.png';

// const logoImgDom:React.FC = () => { 
//   return (<Image />)
// }

const Settings: LayoutSettings & {
  pwa?: boolean;
  logo?: ReactNode;
} = {
  navTheme: 'light',
  // 拂晓蓝
  primaryColor: '#1890ff',
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  colorWeak: false,
  title: 'CloudEon',
  pwa: false,
  logo: '../src/assets/images/logo3.png',//'https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg',
  iconfontUrl: '',
};

export default Settings;
