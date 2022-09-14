/*
 * Copyright 2019 WeBank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

// vue.config.js
const path = require('path')
const fs = require('fs')
const FileManagerPlugin = require('filemanager-webpack-plugin');
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');
const CspHtmlWebpackPlugin = require('csp-html-webpack-plugin');

const getVersion = () => {
  const pkgPath = path.join(__dirname, './package.json')
  let pkg = fs.readFileSync(pkgPath);
  pkg = JSON.parse(pkg);
  return pkg.version;
}

const plugins = [
  new MonacoWebpackPlugin()
]

/**
 * resolve
 * @param {*} dir
 */
function resolve(dir) {
  return path.join(__dirname, dir)
}

if (process.env.NODE_ENV !== 'dev') {
  plugins.push(new CspHtmlWebpackPlugin(
    {
      "base-uri": "'self'",
      "object-src": "'none'",
      "child-src": "'none'",
      "script-src": ["'self'", "'unsafe-eval'"],
      "style-src": ["'self'", "'unsafe-inline'"],
      "frame-src": "*",
      "worker-src": "'self'",
      "connect-src": [
        "'self'",
        "ws:"
      ],
      "img-src": [
        "data:",
        "'self'"
      ]
    },
    {
      enabled: true,
      nonceEnabled: {
        'style-src': false
      }
    }
  ))
}
console.log(process.env.NODE_ENV);

module.exports = {
  publicPath: './',
  outputDir: 'dist/dist',
  chainWebpack: (config) => {
    // set svg-sprite-loader
    config.module
      .rule('svg')
      .exclude.add(resolve('src/components/svgIcon'))
      .end()
    config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(resolve('src/components/svgIcon'))
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]'
      })
      .end()
    if (process.env.NODE_ENV === 'production') {
      config.plugin('compress').use(FileManagerPlugin, [{
        onEnd: {
          // 先删除根目录下的zip包
          delete: [`./streamis-${getVersion()}-dist.zip`],
          // 将dist文件夹下的文件进行打包
          archive: [
            { source: './dist', destination: `./streamis-${getVersion()}-dist.zip` },
          ]
        },
      }])
    }
  },
  configureWebpack: {
    devtool: process.env.NODE_ENV === 'development' ? 'source-map' : false,
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
        '@component': path.resolve(__dirname, './src/components')
      }
    },
    plugins
  },
  // 选项...
  pluginOptions: {
    mock: {
      entry: 'mock.js',
      power: false
    }
  },
  devServer: {
    host: 'localhost',
    port: 8080,
    proxy: {
      '/api': {
        //target: 'http://10.107.97.166:9188',
        target: 'http://172.24.2.230:9400',
        changeOrigin: true,
        pathRewrite: {
          //'^/api': '/mock/15/api'
          '^/api': '/api',
        }
      }
    }
  }
}
