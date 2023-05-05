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
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const outputPath = path.resolve(__dirname, '../../public/static/uc');

const isProd = process.env.NODE_ENV === 'production';

module.exports = {
  webpack: {
    alias: {
      src: path.join(__dirname, 'src'),
    },
    configure: !isProd
      ? undefined
      : (webpackConfig, { paths }) => {
          paths.appBuild = outputPath;
          webpackConfig.output = {
            ...webpackConfig.output,
            path: outputPath,
            publicPath: '/static/uc/',
          };
          webpackConfig.plugins = [...webpackConfig.plugins, new CleanWebpackPlugin()];
          return webpackConfig;
        },
  },
  style: {
    postcss: {
      plugins: [require('tailwindcss'), require('autoprefixer')],
    },
  },
  devServer: {
    port: 3031,
    proxy: {
      '/api/uc': {
        target: 'http://30.43.48.143:4433',
        source: false,
        changeOrigin: true,
        pathRewrite: {
          '/api/uc': '',
        },
      },
    },
  },
};
