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
var ParallelUglifyPlugin =require('webpack-parallel-uglify-plugin');
const CompressionWebpackPlugin = require('compression-webpack-plugin');
const productionGzipExtensions = ['js', 'css', 'html'];
const UglifyJsPlugin = require('uglifyjs-webpack-plugin')

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
  build: {
    productionSourceMap: false,
    productionGzip: true,
  }
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
    new UglifyJSPlugin({
      parallel: 4,
      uglifyOptions: {
          output: {
              comments: false,
              beautify: false,
          },
          compress: {
              warnings: false
          },
      },
      cache: true,
    }),
    new CompressionWebpackPlugin({
      filename: '[path].gz[query]',
      algorithm: 'gzip',
      test: new RegExp(
        '\\.(' + productionGzipExtensions.join('|') + ')$'
      ),
      threshold: 512, // 只有大小大于该值才会被处理
      minRatio: 0.99, // 压缩率小于这个值的资源才会被处理
      deleteOriginalAssets: false, // 删除原文件
    }),

    new webpack.optimize.CommonsChunkPlugin({
      name: ['app', 'vendor', 'polyfills', 'vendor.js']
    }),

    new HtmlWebpackPlugin({
      title: '',
      template: 'src/index.html',
      minify: {
          removeComments: true,
          collapseWhitespace: true,
          removeRedundantAttributes: true,
          useShortDoctype: true,
          removeEmptyAttributes: true,
          removeStyleLinkTypeAttributes: true,
          keepClosingSlash: true,
          minifyJS: true,
          minifyCSS: true,
          minifyURLs: true,
      },
      chunksSortMode:'dependency'
   }),
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
    }),

    new ParallelUglifyPlugin({
      cacheDir: '.cache/',
      uglifyJs:{
        compress: {
          warnings: false,
          drop_debugger: true,
          drop_console:true,       // 打包移除console
          pure_funcs: ['console.log']
        },
        sourceMap: config.build.productionSourceMap,
      }
     })

  ],

};
