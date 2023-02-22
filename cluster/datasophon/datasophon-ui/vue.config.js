let path = require('path')
const webpack = require('webpack')
const ThemeColorReplacer = require('webpack-theme-color-replacer')
const {getThemeColors, modifyVars} = require('./src/utils/themeUtil')
const {resolveCss} = require('./src/utils/theme-color-replacer-extend')
const CompressionWebpackPlugin = require('compression-webpack-plugin')
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')
// const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')
const UglifyJsPlugin = require('uglifyjs-webpack-plugin')

const productionGzipExtensions = ['js', 'css']
const isProd = process.env.NODE_ENV === 'production'
console.log(process.env.NODE_ENV, 'process.env.NODE_ENV')
function resolve(dir) {
  return path.join(__dirname, dir)
}
const assetsCDN = {
  // webpack build externals
  externals: {
    vue: 'Vue',
    'vue-router': 'VueRouter',
    vuex: 'Vuex',
    axios: 'axios',
    nprogress: 'NProgress',
    clipboard: 'ClipboardJS',
    // '@antv/data-set': 'DataSet',
    'js-cookie': 'Cookies'
  },
  css: [
  ],
  js: [
    // '//cdn.jsdelivr.net/npm/vue@2.6.11/dist/vue.min.js',
    // '//cdn.jsdelivr.net/npm/vue-router@3.3.4/dist/vue-router.min.js',
    // '//cdn.jsdelivr.net/npm/vuex@3.4.0/dist/vuex.min.js',
    // '//cdn.jsdelivr.net/npm/axios@0.19.2/dist/axios.min.js',
    // '//cdn.jsdelivr.net/npm/nprogress@0.2.0/nprogress.min.js',
    // '//cdn.jsdelivr.net/npm/clipboard@2.0.6/dist/clipboard.min.js',
    // '//cdn.jsdelivr.net/npm/@antv/data-set@0.11.4/build/data-set.min.js',
    // '//cdn.jsdelivr.net/npm/js-cookie@2.2.1/src/js.cookie.min.js'
    '//cdn.bootcdn.net/ajax/libs/vue/2.6.10/vue.min.js',
    '//cdn.bootcdn.net/ajax/libs/vue-router/3.1.3/vue-router.min.js',
    '//cdn.bootcdn.net/ajax/libs/vuex/3.1.1/vuex.min.js',
    '//cdn.bootcdn.net/ajax/libs/axios/0.19.0/axios.min.js',
    '//cdn.bootcdn.net/ajax/libs/nprogress/0.2.0/nprogress.js',
    '//cdn.bootcdn.net/ajax/libs/clipboard.js/2.0.11/clipboard.min.js',
    '//cdn.bootcdn.net/ajax/libs/js-cookie/3.0.1/js.cookie.min.js'
  ]
}

module.exports = {
  // 172.31.96.16:5001 
  devServer: {
    port: 8080,
    proxy: {
      '/ddh': { //此处要与 /services/api.js 中的 API_PROXY_PREFIX 值保持一致
        target: process.env.VUE_APP_API_BASE_URL,
        changeOrigin: true,
        pathRewrite: {
          '^/api': '/ddh'
        }
      }
    }
  },
  pluginOptions: {
    'style-resources-loader': {
      preProcessor: 'less',
      patterns: [path.resolve(__dirname, "./src/assets/less/theme.less")],
    }
  },
  configureWebpack: config => {
    config.entry.app = ["babel-polyfill", "whatwg-fetch", "./src/main.js"];
    config.performance = {
      hints: false
    }
    config.plugins.push(
      new ThemeColorReplacer({
        fileName: 'css/theme-colors-[contenthash:8].css',
        matchColors: getThemeColors(),
        injectCss: true,
        resolveCss
      })
    )
    // Ignore all locale files of moment.js
    config.plugins.push(new webpack.IgnorePlugin(/^\.\/locale$/, /moment$/))
    // 生产环境下将资源压缩成gzip格式
    if (isProd) {
      // add `CompressionWebpack` plugin to webpack plugins
      config.plugins.push(new CompressionWebpackPlugin({
        algorithm: 'gzip',
        test: new RegExp('\\.(' + productionGzipExtensions.join('|') + ')$'),
        threshold: 10240,
        minRatio: 0.8
      }))
      config.plugins.push(
        //打包环境去掉console.log
        new UglifyJsPlugin({
          uglifyOptions: {
            compress: {
              // warnings: false,
              drop_console: true, //注释console
              drop_debugger: true, //注释debugger
              pure_funcs: ['console.log'], //移除console.log
            },
          },
        }),
      )
    }
    // if prod, add externals
    if (isProd) {
      // config.externals = assetsCDN.externals
    }
    config.plugins.push(new MonacoWebpackPlugin())
    // config.module = {
		// 	rules: [{
		// 		test: /\.js/,
		// 		enforce: 'pre',
		// 		include: /node_modules[\\\/]monaco-editor[\\\/]esm/,
		// 		use: MonacoWebpackPlugin.loader
		// 	}]
    // }
  },
  chainWebpack(config) {
     // 第一步：让其他svg loader不要对路径下进行操作
    config.module
      .rule('svg')
      .exclude.add(resolve('src/icons'))
      .end()
    config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(resolve('src/icons'))
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]',
      })
     .end()
    // 生产环境下关闭css压缩的 colormin 项，因为此项优化与主题色替换功能冲突
    if (isProd) {
      config.plugin('optimize-css')
        .tap(args => {
          args[0].cssnanoOptions.preset[1].colormin = false
          return args
        })
    }
    // 生产环境下使用CDN
    if (isProd) {
      // config.plugin('html')
      //   .tap(args => {
      //     args[0].cdn = assetsCDN
      //     return args
      //   })
    }
  },
  css: {
    loaderOptions: {
      less: {
        lessOptions: {
          modifyVars: modifyVars(),
          javascriptEnabled: true
        }
      }
    }
  },
  publicPath: process.env.VUE_APP_PUBLIC_PATH,
  outputDir: 'dist',
  assetsDir: 'static',
  productionSourceMap: false
}
