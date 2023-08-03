const path = require('path')

const { loaderByName } = require('@craco/craco')
const CracoLessPlugin = require('craco-less')
const webpack = require('webpack')

const packageJson = require('./package.json')

const resolve = (dir) => path.resolve(__dirname, dir)

const currentTime = new Date()

module.exports = {
  babel: {
    plugins: [
      [
        'import',
        {
          libraryName: 'antd',
          libraryDirectory: 'es',
          style: true
        }
      ]
    ]
  },
  webpack: {
    alias: {
      '@': resolve('src')
    },
    configure: (webpackConfig, { env, paths }) => {
      const index = webpackConfig.plugins.findIndex((itme) => itme instanceof webpack.DefinePlugin)

      if (index > -1) {
        const definePlugin = webpackConfig.plugins[index]
        webpackConfig.plugins.splice(
          index,
          1,
          new webpack.DefinePlugin({
            'process.env': {
              ...definePlugin.definitions['process.env'],
              FEATHR_VERSION: JSON.stringify(packageJson.version),
              FEATHR_GENERATED_TIME: JSON.stringify(currentTime.toISOString())
            }
          })
        )
      }

      return webpackConfig
    }
  },
  plugins: [
    {
      plugin: CracoLessPlugin,
      options: {
        lessLoaderOptions: {
          lessOptions: {
            modifyVars: {},
            javascriptEnabled: true
          }
        },
        modifyLessModuleRule(lessModuleRule, context) {
          // Configure the file suffix
          lessModuleRule.test = /\.module\.less$/

          // Configure the generated local ident name.
          const cssLoader = lessModuleRule.use.find(loaderByName('css-loader'))
          cssLoader.options.modules = {
            localIdentName: '[local]_[hash:base64:5]'
          }

          return lessModuleRule
        }
      }
    }
  ]
}
