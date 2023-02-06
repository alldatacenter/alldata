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
const WebpackBuildNotifierPlugin = require('webpack-build-notifier');

module.exports = {
  mode: 'development',
  devtool: 'eval-cheap-module-source-map',
  output: {
    publicPath: '/static/shell/',
    path: path.resolve(__dirname, '../public/static/shell'),
    filename: 'scripts/[name].js',
    chunkFilename: 'scripts/[id].chunk.js',
  },
  watchOptions: {
    aggregateTimeout: 500,
    ignored: ['node_modules', 'public', 'test', 'docs', 'tmp'],
    poll: 5000,
  },
  stats: { children: false },
  plugins: [
    new WebpackBuildNotifierPlugin({
      title: 'Erda UI Development',
      // suppressSuccess: true,
    }),
  ],
  optimization: {
    minimize: false,
  },
};
