const { merge } = require("webpack-merge");
const common = require("./webpack.common.js");
const CopyPlugin = require('copy-webpack-plugin');

module.exports = merge(common("development"), {
  mode: "development",
  devtool: "eval-source-map",
  plugins: [
    new CopyPlugin({
      patterns: [
        { from: "../../config/config.js", to: "config.js" },
      ],
    }),
  ],
  devServer: {
    static: {
      directory: './dist',
    },
    client: {
      overlay: true,
    },
    hot: "only",
    historyApiFallback: true,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, PATCH, OPTIONS',
      'Access-Control-Allow-Headers':
        'X-Requested-With, content-type, Authorization',
    },
    allowedHosts: 'all',
  },
});