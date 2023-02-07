const path = require('path');

module.exports = {
  entry: './src/index.ts',
  module: {
    rules: [
      {
        test: /\.ts?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
    ],
  },
  resolve: {
    extensions: ['.ts', '.js'],
    alias: {
      '^@grpc/grpc-js': '@grpc/grpc-js/build/src',
    }
  },
  output: {
    filename: 'crawlab-sdk.js',
    path: path.resolve(__dirname, 'dist'),
  },
};
