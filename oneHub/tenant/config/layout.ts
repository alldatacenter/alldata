import { Settings } from '@ant-design/pro-layout'

export default {
  navTheme: 'light',
  primaryColor: '#1890ff',
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  colorWeak: false,
  menu: {
    locale: true,
  },
  pwa: false,
  title: '开源大数据平台',
  logo: false,
  iconfontUrl: '',
  showBreadcrumb: false,
} as Settings & {
  pwa: boolean
}
