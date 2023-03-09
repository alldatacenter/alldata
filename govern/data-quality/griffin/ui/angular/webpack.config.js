/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
module.exports = {

  entry: {
    'app': './src/main.ts',
    'vendor': './src/vendor.ts',
    'polyfills': './src/polyfills.ts'
  },
  output: {
    // path: '/dist',
    filename: '[name].js'
  },
  module: {
    rules: [
      {
        test: /\.ts$/,
        loader: 'ts-loader'
      },
      {
        test: /\.html$/,
        loader: 'html-loader'
      },
      {
        test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)$/,
        loader: 'file-loader?name=assets/[name].[hash].[ext]'
      },
      {
        test: /\.css$/,
        loaders: 'style-loader!css-loader'
      },
      {
        test: /\.css$/,
        loader: 'raw-loader'
      }
    ]
  },

  resolve: {
    extensions: ['.js', '.ts']
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin({
      name: ['app', 'vendor', 'polyfills', 'vendor.js']
    }),

    new HtmlWebpackPlugin({
      template: 'src/index.html'
    }),

    new webpack.ProvidePlugin({
      $: 'jquery',
      jQuery: 'jquery',
      'window.jQuery': 'jquery',
      jquery: 'jquery',
      Popper: ['popper.js', 'default'],
      // In case you imported plugins individually, you must also require them here:
      Util: "exports-loader?Util!bootstrap/js/dist/util",
      Dropdown: "exports-loader?Dropdown!bootstrap/js/dist/dropdown",
      echarts: "echarts"
    })
  ],

};
