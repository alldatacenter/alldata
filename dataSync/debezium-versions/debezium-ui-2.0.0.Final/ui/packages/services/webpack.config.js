const path = require('path');

module.exports = {
  mode: "production",
  entry: {
    'index': './src/index.ts'
  },
  devtool: 'source-map',
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
