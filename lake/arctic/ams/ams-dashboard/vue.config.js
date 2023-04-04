/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

// const webpack = require('webpack')
// eslint-disable-next-line @typescript-eslint/no-var-requires
const path = require('path')
// eslint-disable-next-line @typescript-eslint/no-var-requires
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')
const isProduction = ['production'].includes(process.env.NODE_ENV)
const ENV = 'DEV'
const ENV_HOST = {
  DEV: 'http://sloth-commerce-test1.jd.163.org:29499/', // http://10.196.98.23:29099/
  TEST: '',
  ONLINE: ''
}

const devServer = {
  port: '8080',
  headers: {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Credentials': 'true',
    'Access-Control-Allow-Headers':
      'Content-Type, Content-Length, Authorization, Accept, X-Requested-With , yourHeaderFeild',
    'Access-Control-Allow-Methods': 'PUT,POST,GET,DELETE,OPTIONS'
  },
  proxy: {
    '^/ams': {
      target: ENV_HOST[ENV],
      changeOrigin: true,
      onProxyReq (proxyReq) {
        proxyReq.setHeader('cookie', process.env.VUE_APP_COOKIE)
      }
    }
  }
}

const css = {
  loaderOptions: {
    less: {
      modifyVars: {
        'primary-color': '#1890ff',
        'link-color': '#1890ff',
        'border-color-base': '#e8e8f0',
        'border-radius-base': '2px',
        'border-color-split': '#e8e8f0',
        'header-color': 'rgba(0, 0, 0, 0.85)',
        'text-color': '#79809a',
        'text-color-secondary': '#c0c0ca',
        'font-size-base': '14px',
        'dark-gray-color': '#2b354a',
        'dark-bg-color': '#202a40',
        'dark-bg-primary-color': '#1a2232'
      },
      javascriptEnabled: true
    }
  }
}
const plugins = [
  new MonacoWebpackPlugin({
    output: './js',
    languages: ['sql', 'shell', 'json']
  })
]

const pluginOptions = {
  mock: {
    entry: './mock/index.js',
    debug: true,
    disable: isProduction
  }
}

function getPublicPath () {
  // if (isProduction) {
  //   return '/public/'
  // }
  return '/'
}

module.exports = {
  css,
  devServer,
  publicPath: getPublicPath(),
  productionSourceMap: !isProduction,
  chainWebpack: (config) => {
    // vue-svg-loader
    const svgRule = config.module.rule('svg') // find svg-loader
    svgRule.uses.clear() // clear loader
    svgRule.exclude.add([/node_modules/, path.resolve(__dirname, 'src/assets/images')])
    svgRule // Add new loader handling for svg
      .test(/\.svg$/)
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]'
      })

    // Modify images loader to add svg processing
    const imagesRule = config.module.rule('images')
    imagesRule.exclude.add(path.resolve(__dirname, 'src/assets/icons'))
    config.module
      .rule('images')
      .test(/\.(png|jpe?g|gif|svg)(\?.*)?$/)

    // @vueuse/core
    config.module
      .rule('mjs')
      .test(/\.mjs$/)
      .include.add(/node_modules/)
      .end()
      .type('javascript/auto')
  },
  configureWebpack: (config) => {
    if (isProduction) {
      config.optimization.minimizer[0].options.terserOptions.compress.drop_console = true
    }
    return { plugins }
  },
  pluginOptions
}
