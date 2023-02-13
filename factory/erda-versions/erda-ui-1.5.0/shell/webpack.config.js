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

const path = require('path');
const os = require('os');
const fs = require('fs');
const webpack = require('webpack');
const { merge } = require('webpack-merge');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const { getScssTheme, getLessTheme } = require('./config/theme');
const css = require('./app/views/css.js');
const pkg = require('./package.json');
const { ModuleFederationPlugin } = require('webpack').container;
const mfConfigs = require('./mf.config');

// const SpeedMeasurePlugin = require('speed-measure-webpack-plugin');

// const smp = new SpeedMeasurePlugin(); // https://github.com/stephencookdev/speed-measure-webpack-plugin/issues/167 issue caused merge not working as expected

const packageJson = require('./package.json');

const mainVersion = packageJson.version.slice(0, -2);

const resolve = (pathname) => path.resolve(__dirname, pathname);

module.exports = () => {
  const nodeEnv = process.env.NODE_ENV || 'development';
  const isProd = nodeEnv === 'production';
  const cpuNum = os.cpus().length;

  console.log('isProd:', isProd, process.version);

  const targetConfig = require(`./webpack.${nodeEnv}.js`);

  const commonConfig = {
    parallelism: cpuNum,
    entry: {
      app: ['./app'],
    },
    target: isProd ? 'browserslist' : 'web',
    resolve: {
      alias: {
        app: resolve('./app'),
        common: resolve('./app/common'),
        configForm: resolve('./app/configForm'),
        'yml-chart': resolve('./app/yml-chart'),
        'config-page': resolve('./app/config-page'),
        layout: resolve('./app/layout'),
        user: resolve('./app/user'),
        charts: resolve('./app/charts'),
        dcos: resolve('./app/modules/dcos'),
        project: resolve('./app/modules/project'),
        publisher: resolve('./app/modules/publisher'),
        cmp: resolve('./app/modules/cmp'),
        org: resolve('./app/modules/org'),
        application: resolve('./app/modules/application'),
        runtime: resolve('./app/modules/runtime'),
        dop: resolve('./app/modules/dop'),
        addonPlatform: resolve('./app/modules/addonPlatform'),
        msp: resolve('./app/modules/msp'),
        apiManagePlatform: resolve('./app/modules/apiManagePlatform'),
        agent: resolve('./app/agent.js'),
        i18n: resolve('./app/i18n.ts'),

        'monitor-overview': resolve('./app/modules/msp/monitor/monitor-overview'),
        'application-insight': resolve('./app/modules/msp/monitor/application-insight'),
        'external-insight': resolve('./app/modules/msp/monitor/external-insight'),
        'browser-insight': resolve('./app/modules/msp/monitor/browser-insight'),
        'gateway-ingress': resolve('./app/modules/msp/monitor/gateway-ingress'),
        'docker-container': resolve('./app/modules/msp/monitor/docker-container'),
        'mobile-insight': resolve('./app/modules/msp/monitor/mobile-insight'),
        'api-insight': resolve('./app/modules/msp/monitor/api-insight'),
        'trace-insight': resolve('./app/modules/msp/monitor/trace-insight'),
        'monitor-common': resolve('./app/modules/msp/monitor/monitor-common'),
        topology: resolve('./app/modules/msp/monitor/topology'),
        'status-insight': resolve('./app/modules/msp/monitor/status-insight'),
        'error-insight': resolve('./app/modules/msp/monitor/error-insight'),
        'monitor-alarm': resolve('./app/modules/msp/monitor/monitor-alarm'),
      },
      modules: ['node_modules', resolve('../../erda-ui/shell/node_modules')],
      extensions: ['.js', '.jsx', '.tsx', '.ts', '.d.ts'],
      fallback: {
        path: require.resolve('path-browserify'),
        https: require.resolve('https-browserify'),
        http: require.resolve('stream-http'),
        events: require.resolve('events'),
      },
    },
    cache: {
      type: 'filesystem',
    },
    module: {
      rules: [
        {
          test: /\.(scss)$/,
          use: [
            ...(isProd ? [MiniCssExtractPlugin.loader] : []), // extract not support hmr, https://github.com/webpack-contrib/extract-text-webpack-plugin/issues/222
            ...(isProd ? [] : ['style-loader']),
            {
              loader: 'css-loader',
              options: {
                url: false,
                sourceMap: false,
              },
            },
            {
              loader: 'sass-loader',
              options: {
                sourceMap: false,
                webpackImporter: false,
                additionalData: getScssTheme(),
              },
            },
            {
              loader: 'sass-resources-loader',
              options: {
                sourceMap: false,
                resources: [
                  resolve('./app/styles/_variable.scss'),
                  resolve('./app/styles/_color.scss'),
                  resolve('./app/styles/_mixin.scss'),
                ],
              },
            },
          ],
        },
        {
          test: /\.(less)$/,
          use: [
            ...(isProd ? [MiniCssExtractPlugin.loader] : ['style-loader']),
            'css-loader',
            {
              loader: 'less-loader',
              options: {
                sourceMap: true,
                lessOptions: {
                  modifyVars: getLessTheme(),
                  javascriptEnabled: true,
                },
              },
            },
          ],
        },
        {
          test: /\.(css)$/,
          use: [
            ...(isProd ? [MiniCssExtractPlugin.loader] : ['style-loader']),
            'css-loader',
            {
              loader: 'postcss-loader',
              options: {
                postcssOptions: {
                  plugins: [require.resolve('tailwindcss'), require.resolve('autoprefixer')],
                },
              },
            },
          ],
        },
        {
          test: /\.(tsx?|jsx?)$/,
          use: [
            {
              loader: 'babel-loader',
            },
          ],
          resolve: {
            fullySpecified: false,
          },
        },
        {
          test: /\.map$/,
          loader: 'ignore-loader',
        },
        {
          test: /\.(png|jpe?g|gif|svg|ico)$/i,
          use: [
            {
              loader: 'file-loader',
              options: {
                name: '[name].[hash].[ext]',
                outputPath: 'images',
              },
            },
          ],
        },
      ],
    },
    plugins: [
      new webpack.ProvidePlugin({
        process: 'process/browser.js',
        Buffer: ['buffer', 'Buffer'],
      }),
      new CopyWebpackPlugin({
        patterns: [
          // { from: './app/images', to: 'shell/images' },
          { from: './app/static', to: resolve('../public/static') },
        ],
      }),
      new CleanWebpackPlugin(),
      new HtmlWebpackPlugin({
        filename: 'index.html',
        template: './app/views/index.ejs',
        excludeChunks: ['modules'],
        css,
        skeleton: {
          html: fs.readFileSync(resolve('./app/views/skeleton.html')),
        },
        minify: isProd
          ? {
              collapseWhitespace: true,
              minifyJS: true,
              minifyCSS: true,
              removeEmptyAttributes: true,
            }
          : false,
        diceVersion: JSON.stringify(pkg.version),
      }),
      new webpack.ContextReplacementPlugin(
        // eslint-disable-next-line
        /moment[\\\/]locale$/,
        /(zh-cn)\.js/,
      ),
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': JSON.stringify(nodeEnv),
        'process.env.UI_ENV': JSON.stringify(process.env.ERDA_UI_ENV),
        'process.env.FOR_COMMUNITY': JSON.stringify(process.env.FOR_COMMUNITY),
        'process.env.mainVersion': JSON.stringify(mainVersion),
      }),
      ...mfConfigs.map((mfConfig) => new ModuleFederationPlugin(mfConfig)),
    ],
    optimization: {
      splitChunks: {
        chunks: 'all', // 默认作用于异步chunk，值为all/initial/async， all=initial+async
        minSize: 30000, // 默认值是30kb,代码块的最小尺寸
        // maxSize: 500000, // 500kb
        minChunks: 1, // 被多少模块共享,在分割之前模块的被引用次数
        maxAsyncRequests: 5, // 限制异步模块内部的并行最大请求数的，说白了你可以理解为是每个import()它里面的最大并行请求数量
        maxInitialRequests: 5, // 限制入口的拆分数量
        name: false,
        cacheGroups: {
          // 设置缓存组用来抽取满足不同规则的chunk
          vendors: {
            test: /[\\/]node_modules[\\/]/,
            reuseExistingChunk: true,
            priority: -10,
          },
          eCharts: {
            test: /[\\/]node_modules[\\/]echarts\/lib/,
            reuseExistingChunk: true,
            name: 'eCharts',
            priority: -5,
          },
          styles: {
            type: 'css/mini-extract',
            enforce: true, // force css in new chunks (ignores all other options)
          },
        },
      },
    },
  };

  return merge(commonConfig, targetConfig);
};
