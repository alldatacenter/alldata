const { merge } = require("webpack-merge");
const common = require("./webpack.common.js");
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const TerserJSPlugin = require('terser-webpack-plugin');

module.exports = merge(common('production', { mode: "production" }), {
  mode: "production",
  devtool: "source-map",
  optimization: {
    minimize: true,
    minimizer: [
      new TerserJSPlugin({}),
      new CssMinimizerPlugin()
    ]
  },
  output: {
    filename: '[name].[contenthash:8].js'
  }
});