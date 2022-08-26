const path = require("path");
const configs = require('./src/config')
const CompressionPlugin = require("compression-webpack-plugin");
const UglifyJsPlugin = require("uglifyjs-webpack-plugin");
const resolve = dir => {
  return path.join(__dirname, dir);
};
/**
 * 在项目开发的时候将生产环境以及开发环境进行判断
 * 将生产环境中的路径用cdn来进行优化处理
 * 将开发环境中替换为本地的内容，方便处理bug以及开启vueDev
 * 我们可以根据环境变量进行相应的处理，只有在产品的时候，才让插件去自动注入相应的资源文件到html页面
 */
const enableProduction = process.env.NODE_ENV === "production"; // 是否生产环境
// const enableProduction = false // 统一取消cdn加载 目前cdn链接的https请求挂了

let externals = {
  vue: "Vue",
  axios: "axios",
  "vue-router": "VueRouter",
  vuex: "Vuex",
  "view-design": "iview",
  "vue-lazyload": "VueLazyload",
  "js-cookie": "Cookies",
  "sockjs-client": "SockJS",
  "@antv/g2": "G2"
};

// 使用CDN的内容
let cdn = {
  css: ["https://cdn.pickmall.cn/cdn/iview.css"],
  js: [
    // vue must at first!
    "https://cdn.pickmall.cn/cdn/vue.min.js",
    "https://cdn.pickmall.cn/cdn/vuex.min.js",
    "https://cdn.pickmall.cn/cdn/vue-router.min.js",
    "https://cdn.pickmall.cn/cdn/axios.min.js",
    "https://cdn.pickmall.cn/cdn/iview.min.js",
    "https://cdn.pickmall.cn/cdn/vue-lazyload.min.js",
    "https://cdn.pickmall.cn/cdn/js.cookie.min.js",
    "https://cdn.pickmall.cn/cdn/sockjs.min.js",
    "https://gw.alipayobjects.com/os/lib/antv/g2/4.1.24/dist/g2.min.js"
  ]
};

// 删除注释
let jsPlugin = [
  new UglifyJsPlugin({
    uglifyOptions: {
      // 删除注释
      output: {
        comments: false
      },
      compress: {
        drop_console: true, // 删除所有调式带有console的
        drop_debugger: true,
        pure_funcs: ["console.log"] // 删除console.log
      }
    }
  })
];
// 判断是否需要加载CDN
cdn = enableProduction && configs.enableCDN ? cdn : { css: [], js: [] };
externals = enableProduction && configs.enableCDN  ? externals : {};
jsPlugin = enableProduction ? jsPlugin : [];

module.exports = {
  // 输出文件目录，当运行 vue-cli-service build 时生成的生产环境构建文件的目录。注意目标目录在构建之前会被清除
  outputDir:'dist',
  // 放置生成的静态资源 (js、css、img、fonts) 的目录。
  assetsDir:'static',

  css: {
    // 是否为 CSS 开启 source map。设置为 true 之后可能会影响构建的性能。
    sourceMap:false,
    loaderOptions: {
      // 向 CSS 相关的 loader 传递选项
      less: {
        lessOptions: {
          javascriptEnabled: true
        }
      }
    }
  },
  devServer: {
    port: configs.port
  },

  // 打包时不生成.map文件 避免看到源码
  productionSourceMap: false,

  // 部署优化
  configureWebpack: {
    externals,

    // GZIP压缩
    plugins: [
      new CompressionPlugin({
        test: /\.js$|\.html$|\.css/, // 匹配文件
        threshold: 10240 // 对超过10k文件压缩
      })
    ],
    optimization: {
      minimizer: jsPlugin,
      runtimeChunk: "single",
      splitChunks: {
        chunks: "all",
        maxInitialRequests: Infinity,
        minSize: 20000,
        cacheGroups: {
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name(module) {
              const packageName = module.context.match(
                /[\\/]node_modules[\\/](.*?)([\\/]|$)/
              )[1];
              return `npm.${packageName.replace("@", "")}`;
            }
          }
        }
      }
    }
  },

  // 将cdn的资源挂载到插件上
  chainWebpack(config) {
    //  @ 对应 src目录
    config.resolve.alias.set("@", resolve("src"));
    config.plugin("html").tap(args => {
      args[0].cdn = cdn;
      return args;
    });
  },
  pluginOptions: {
    "style-resources-loader": {
      preProcessor: "scss",
      patterns: [path.resolve(__dirname, "./src/styles/common.scss")]
    }
  }
};
