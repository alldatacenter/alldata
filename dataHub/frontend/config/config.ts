import { defineConfig } from 'umi'
import layout from './layout'
import routes from './routes'

const path = require('path')

export default defineConfig({
  // 路由，参考: https://umijs.org/docs/routing
  routes,

  // 静态文件路径
  publicPath: '/dist/',

  // 本地开发的服务配置
  devServer: {
    host: '0.0.0.0',
    port: 3000,
  },

  define: {
    API_GRAPHQL_URL: 'http://localhost:9800/graphql/',
    MEDIA_URL: 'http://localhost:9800',
  },

  // 打包生成的静态文件加 hash
  hash: true,

  // 参考: https://umijs.org/plugins/plugin-antd
  antd: {
    dark: false,
  },

  // 参考: https://umijs.org/plugins/plugin-dva
  dva: {
    hmr: true,
  },

  // 参考: https://umijs.org/plugins/plugin-layout
  layout: {
    name: '开源大数据平台',
    locale: true,
    siderWidth: 160,
  },

  // 多语言配置
  locale: {
    // default zh-CN
    default: 'zh-CN',
    // default true, when it is true, will use `navigator.language` overwrite default
    antd: true,
    baseNavigator: true,
  },

  // 动态加载
  dynamicImport: {
    loading: '@/components/PageLoading/index',
  },

  // Theme for antd: https://ant.design/docs/react/customize-theme-cn
  theme: {
    'primary-color': layout.primaryColor,
  },

  // 关闭默认标题
  title: false,

  // 不加载 moment 多语言文件
  ignoreMomentLocale: true,

  // 配置 <head> 里的额外脚本，数组项为字符串或对象
  headScripts: ['https://at.alicdn.com/t/font_1992548_o1ynnlje8j.js'],

  // 配置alias
  alias: {
    '@/types': path.resolve(__dirname, '..', 'src/types'),
    // '@ant-design/icons/lib/index.es': path.resolve(__dirname, '..', 'src/utils/match'),
  },

  // 按需引入 antd
  extraBabelPlugins: [['import', { libraryName: 'antd', style: 'css' }]],
})
