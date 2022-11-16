const path = require('path');
const { execSync } = require('child_process');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyPlugin = require('copy-webpack-plugin');
const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const webpack = require('webpack');
const ChunkMapper = require('@redhat-cloud-services/frontend-components-config/chunk-mapper');
const { dependencies, federatedModuleName } = require('./package.json');

const getCommitHash = () => {
  try {
    return execSync('git rev-parse --short HEAD').toString();
  } catch (e) {
    console.error(
      '\x1b[31m',
      '🔥🔥 Command failed: git rev-parse --short HEAD. Make sure .git dir available and git installed!'
    );
  }
};
const COMMIT_HASH = process.env.COMMIT_HASH || getCommitHash();
// Try the environment variable, otherwise use root
const ASSET_PATH = process.env.ASSET_PATH || '/';
const reactCSSRegex = /(react-[\w-]+\/dist|react-styles\/css)\/.*\.css$/;

module.exports = (argv) => {
  const isProduction = argv || argv.mode === 'production';
  return {
    entry: {
      app: './src/index.tsx',
    },
    module: {
      rules: [
        {
          test: /\.(tsx|ts|jsx)?$/,
          use: [
            {
              loader: 'ts-loader',
              options: {
                transpileOnly: true,
                experimentalWatchApi: true,
              },
            },
          ],
        },
        {
          test: /\.css$/,
          exclude: reactCSSRegex,
          use: [
            MiniCssExtractPlugin.loader,
            {
              loader: 'css-loader',
              options: { importLoaders: 1 },
            },
            {
              loader: 'postcss-loader',
              options: {
                postcssOptions: {
                  path: __dirname + '/postcss.config.js',
                },
              },
            },
          ],
          sideEffects: true,
        },
        {
          test: reactCSSRegex,
          use: 'null-loader',
        },
        {
          test: /\.(ttf|eot|woff|woff2)$/,
          use: {
            loader: 'file-loader',
            options: {
              limit: 5000,
              name: isProduction ? '[contenthash:8].[ext]' : '[name].[ext]',
            },
          },
        },
        {
          test: /\.(svg|jpg|jpeg|png|gif)$/i,
          use: [
            {
              loader: 'url-loader',
              options: {
                limit: 5000,
                name: isProduction
                  ? '[name].[contenthash:8].[ext]'
                  : '[name].[ext]',
              },
            },
          ],
        },
        {
          test: /\.(json)$/i,
          include: path.resolve(__dirname, 'src/locales'),
          use: [
            {
              loader: 'url-loader',
              options: {
                limit: 5000,
                outputPath: 'locales',
                name: isProduction ? '[contenthash:8].[ext]' : '[name].[ext]',
              },
            },
          ],
        },
      ],
    },
    plugins: [
      new HtmlWebpackPlugin({
        template: './src/index.html',
        favicon: './src/favicon.ico',
      }),
      // This makes it possible for us to safely use env vars on our code
      new webpack.DefinePlugin({
        'process.env.ASSET_PATH': JSON.stringify(ASSET_PATH),
      }),
      new webpack.DefinePlugin({
        'process.env.COMMIT_HASH': JSON.stringify(COMMIT_HASH),
      }),
      new CopyPlugin({
        patterns: [{ from: './src/locales', to: 'locales' }],
      }),
      new MiniCssExtractPlugin({
        filename: '[name].[contenthash:8].css',
        chunkFilename: '[contenthash:8].css',
        ignoreOrder: true
      }),
      new ChunkMapper({
        modules: [federatedModuleName],
      }),
      new webpack.container.ModuleFederationPlugin({
        name: 'debezium_ui',
        filename: 'dbz-connector-configurator.remoteEntry.js',
        exposes: {
          './config': './src/app/pages/createConnector/federatedModule/config',
        },
        shared: {
          ...dependencies,
          react: {
            singleton: true,
            requiredVersion: dependencies['react'],
          },
          'react-dom': {
            singleton: true,
            requiredVersion: dependencies['react-dom'],
          },
          'react-router-dom': {
            singleton: false, // consoledot needs this to be off to be able to upgrade the router to v6. We don't need this to be a singleton, so let's keep this off
            requiredVersion: dependencies['react-router-dom'],
          },
        },
      }),
    ],
    output: {
      filename: '[name].bundle.js',
      path: path.resolve(__dirname, 'dist'),
      publicPath: 'auto',
    },
    resolve: {
      alias: {
        shared: path.resolve(__dirname, 'src/app/shared'),
        components: path.resolve(__dirname, 'src/app/components'),
        assets: path.resolve(__dirname, 'assets'),
        i18n: path.resolve(__dirname, 'src/i18n'),
        layout: path.resolve(__dirname, 'src/app/layout'),
        '@debezium/ui-services': '../services/src',
        '@debezium/ui-models': '../models/src',
      },
      extensions: ['.ts', '.tsx', '.js', '.jsx'],
      plugins: [
        new TsconfigPathsPlugin({
          configFile: path.resolve(__dirname, './tsconfig.json'),
        }),
      ],
      symlinks: false,
      cacheWithContext: false,
    },
  };
};
