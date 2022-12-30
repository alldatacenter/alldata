const path = require('path');

module.exports = {
  mode: "production",
  devtool: 'source-map',
  entry: {
    'index': './src/index.ts'
  },
  module: {
    rules: [
      {
        test: /\.ts?$/,
        use: 'ts-loader',
        exclude: /node_modules/
      }
    ]
  },
  resolve: {
    extensions: [ '.ts', '.js' ]
  },
  output: {
    filename: '[name].js',
    path: path.resolve(__dirname, "dist"),
    publicPath: "auto",
  }
};
