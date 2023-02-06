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
const CracoAntDesignPlugin = require('craco-antd');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

const resolve = (pathname) => path.resolve(__dirname, pathname);
const themeColor = '#6A549E';
const outputPath = path.resolve(__dirname, '../../public/static/market');

module.exports = {
  webpack: {
    configure: (webpackConfig, { paths }) => {
      paths.appBuild = outputPath;
      webpackConfig.output = {
        ...webpackConfig.output,
        path: outputPath,
        publicPath: '/static/market/',
      };
      webpackConfig.plugins = [...webpackConfig.plugins, new CleanWebpackPlugin()];
      return webpackConfig;
    },
    module: {
      rules: [
        {
          test: /\.(tsx?|jsx?)$/,
          include: [resolve('./src')],
          use: ['thread-loader', 'babel-loader'],
          resolve: {
            fullySpecified: false,
          },
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
  },
  output: {},
  style: {
    postcss: {
      plugins: [require('autoprefixer')],
    },
    sass: {
      loaderOptions: {
        additionalData: `@import "./src/styles/_resources.scss";`,
      },
    },
  },
  plugins: [
    {
      plugin: CracoAntDesignPlugin,
      options: {
        customizeTheme: {
          '@primary-color': themeColor,
          '@link-color': themeColor,
        },
      },
    },
  ],
};
