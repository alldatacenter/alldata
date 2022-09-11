/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/* eslint-disable @typescript-eslint/no-var-requires */
const path = require('path');
const webpack = require('webpack');
const AntdDayjsWebpackPlugin = require('antd-dayjs-webpack-plugin');
const { addReactRefresh } = require('customize-cra-react-refresh');
const {
  override,
  addWebpackAlias,
  addLessLoader,
  addWebpackPlugin,
  fixBabelImports,
} = require('customize-cra');

module.exports = {
  webpack: override(
    addReactRefresh(),
    addLessLoader({
      lessOptions: {
        javascriptEnabled: true,
        modifyVars: {
          hack: `true; @import "src/themes/antd.var.less";`, // Override antd
        },
      },
    }),
    addWebpackAlias({
      '@': path.resolve(__dirname, 'src'),
    }),
    fixBabelImports('antd', {
      libraryDirectory: 'lib',
      style: true,
    }),
    addWebpackPlugin(new AntdDayjsWebpackPlugin()),
  ),
};
