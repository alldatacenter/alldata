// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

const os = require('os');
const path = require('path');
const webpack = require('webpack');
const TerserPlugin = require('terser-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const GitRevisionPlugin = require('git-revision-webpack-plugin');
const FileManagerPlugin = require('filemanager-webpack-plugin');
// const SWPrecacheWebpackPlugin = require('sw-precache-webpack-plugin');

const gitRevisionPlugin = new GitRevisionPlugin();
const banner = `module: shell
commit: ${gitRevisionPlugin.commithash().slice(0, 6)}
branch: ${gitRevisionPlugin.branch()}
buildTime: ${new Date().toLocaleString('zh-CH', { timeZone: 'Asia/Shanghai' })}
buildBy: ${os.userInfo().username}`;

module.exports = {
  mode: 'production',
  devtool: process.env.enableSourceMap === 'true' ? 'hidden-source-map' : false,
  output: {
    publicPath: '/static/shell/',
    path: path.resolve(__dirname, '../public/static/shell'),
    filename: 'scripts/[name].[chunkhash].js',
    chunkFilename: 'scripts/[chunkhash].chunk.js',
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: 'style/[name].[contenthash].css',
      ignoreOrder: true,
    }),
    ...(process.env.enableSourceMap === 'true'
      ? [
          new FileManagerPlugin({
            events: {
              onEnd: {
                mkdir: ['sourcemap'],
                copy: [
                  {
                    source: '../public/static/shell/scripts/*.map',
                    destination: 'sourcemap/',
                  },
                ],
                delete: [
                  {
                    source: '../public/static/shell/scripts/*.map',
                    options: {
                      force: true,
                    },
                  },
                ],
              },
            },
            runTasksInSeries: true,
          }),
        ]
      : []),

    // 需要https
    // new SWPrecacheWebpackPlugin({
    //   cacheId: 'dice-sw',
    //   filename: 'sw.js',
    //   minify: true,
    //   staticFileGlobs: ['public/**/*.js', 'public/images/**/*.{png,ico,jpg}', 'public/**/*.css'],
    //   stripPrefix: 'public/',
    //   navigateFallback: '/',
    //   staticFileGlobsIgnorePatterns: [/sw\.js$/i],
    //   runtimeCaching: [{
    //     urlPattern: '/vendors~app*', // 修正vendors~app未被cache的问题,因vendors~app.chunk.js在sw.js之后生成
    //     handler: 'cacheFirst',
    //     options: {
    //       successResponses: /^200$/,
    //     },
    //   }, {
    //     urlPattern: /\/api\/(?!ws\/).*/, // 拦截api请求，但过滤/api/ws,这样可以防止ws断开后可以继续重连
    //     handler: 'networkFirst',
    //     options: {
    //       successResponses: /^200$/,
    //     },
    //   }, {
    //     urlPattern: /\/.*/, // cache页面，这样当没有联网时页面可以正常加载
    //     handler: 'networkFirst',
    //     options: {
    //       successResponses: /^200$/,
    //     },
    //   }, {
    //     urlPattern: '/*', // cache第三方的一些资源
    //     handler: 'cacheFirst',
    //     options: {
    //       origin: /https?:\/\/(at.alicdn)\.com/,
    //     },
    //   }],
    // }),
  ],
  optimization: {
    minimize: true,
    minimizer: [
      new webpack.BannerPlugin(banner),
      new TerserPlugin({
        parallel: os.cpus().length,
        extractComments: false,
      }),
      new CssMinimizerPlugin({
        minimizerOptions: {
          preset: [
            'default',
            {
              discardComments: { removeAll: true },
            },
          ],
        },
      }),
    ],
  },
};
