const webpack = require('webpack');
const path = require('path');
const config = require('./config.js');

module.exports = {
    externals: {
        'react': 'React',
        'react-dom': 'ReactDOM',
        'antd': 'antd',
        'moment': 'moment'
    },
    resolve: {                                    // 指定可以被 import 的文件后缀
	    extensions: ['.js', '.jsx'],
	    alias: {                                    // 配置常用路径
	      	src: path.resolve(__dirname, '../src'),
	      	views: 'src/views',
	      	js: 'src/js',
	    }
  	},
	module: {
		rules: [{
			test: /\.(js|jsx)$/,
			loader: 'babel-loader',
			include: path.resolve(__dirname, '../src'),
		}, {
			test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
	        loader: 'url-loader',
	        options: {
	          	limit: 10000,
	          	name: path.posix.join(config.prod.subDirectory, 'img/[name].[hash:7].[ext]'),
	        }
		}, {
	        test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
	        loader: 'url-loader',
	        options: {
	          	limit: 10000,
	          	name: path.posix.join(config.prod.subDirectory, 'fonts/[name].[hash:7].[ext]')
	        }
      	}]
	},
	optimization: {
	   	// splitChunks: {
	    //  	chunks: 'all',
	    //  	name: 'common',
	   	// },
	   	// runtimeChunk: {
	    //  	name: 'runtime',
           // }
        splitChunks: { 
            chunks: "all",         // 代码块类型 必须三选一： "initial"（初始化） | "all"(默认就是all) | "async"（动态加载） 
            minSize: 0,                // 最小尺寸，默认0
            minChunks: 1,              // 最小 chunk ，默认1
            maxAsyncRequests: 1,       // 最大异步请求数， 默认1
            maxInitialRequests: 1,     // 最大初始化请求书，默认1
            name: () => {},            // 名称，此选项可接收 function
            cacheGroups: {                // 缓存组会继承splitChunks的配置，但是test、priorty和reuseExistingChunk只能用于配置缓存组。
                priority: "0",              // 缓存组优先级 false | object |
                vendor: {                   // key 为entry中定义的 入口名称
                    chunks: "initial",        // 必须三选一： "initial"(初始化) | "all" | "async"(默认就是异步)
                    test: /react-router|axios|prop-types/,     // 正则规则验证，如果符合就提取 chunk
                    name: "vendor",           // 要缓存的 分隔出来的 chunk 名称
                    minSize: 0,
                    minChunks: 1,
                    enforce: true,
                    reuseExistingChunk: true   // 可设置是否重用已用chunk 不再创建新的chunk
                },
                codemirror: {                   // key 为entry中定义的 入口名称
                    chunks: "initial",        // 必须三选一： "initial"(初始化) | "all" | "async"(默认就是异步)
                    test: /codemirror|react-codemirror2/,     // 正则规则验证，如果符合就提取 chunk
                    name: "codemirror",           // 要缓存的 分隔出来的 chunk 名称
                    minSize: 0,
                    minChunks: 1,
                    enforce: true,
                    reuseExistingChunk: true   // 可设置是否重用已用chunk 不再创建新的chunk
                }
            }
        }
	}
};