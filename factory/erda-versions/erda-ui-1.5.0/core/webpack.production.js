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
const TerserPlugin = require('terser-webpack-plugin');
const os = require('os');
const webpack = require('webpack');
const GitRevisionPlugin = require('git-revision-webpack-plugin');

const gitRevisionPlugin = new GitRevisionPlugin();
const banner = `module: core
commit: ${gitRevisionPlugin.commithash().slice(0, 6)}
branch: ${gitRevisionPlugin.branch()}
buildTime: ${new Date().toLocaleString('zh-CH', { timeZone: 'Asia/Shanghai' })}
buildBy: ${os.userInfo().username}`;

module.exports = {
  mode: 'production',
  output: {
    path: path.resolve(__dirname, '../public/static/core'),
    filename: 'scripts/[name].js',
    chunkFilename: 'scripts/[chunkhash].chunk.js',
    publicPath: '/static/core/',
  },
  optimization: {
    minimize: true,
    minimizer: [
      new webpack.BannerPlugin(banner),
      new TerserPlugin({
        parallel: true,
        extractComments: false,
      }),
    ],
  },
};
