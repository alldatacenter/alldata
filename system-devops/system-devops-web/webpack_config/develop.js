const webpack = require('webpack');
const path = require('path');
const merge = require('webpack-merge');
const autoprefixer = require('autoprefixer');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const OpenBrowserPlugin = require('open-browser-webpack-plugin');
const baseWebpackConfig = require('./webpack.config.js');
const config = require('./config.js');
const hostPort = 8040;

module.exports = merge(baseWebpackConfig, {
	entry: {
		main: [
			// 开启 React 代码的模块热替换(HMR)
	      	'react-hot-loader/patch',

	      	// 为 webpack-dev-server 的环境打包代码
	      	// 然后连接到指定服务器域名与端口
	      	'webpack-dev-server/client?http://0.0.0.0:' + hostPort,

	      	// 为热替换(HMR)打包好代码
	      	// only- 意味着只有成功更新运行代码才会执行热替换(HMR)
	      	'webpack/hot/only-dev-server',
	      	// 我们 app 的入口文件
	      	path.resolve(__dirname, '../src/main.js')
		],
    },
    output: {											// 定义出口
		publicPath: config.prod.publicPath,
		path: config.prod.root,
		filename: path.posix.join(config.prod.subDirectory, 'js/[name].js'),
	    chunkFilename: path.posix.join(config.prod.subDirectory, 'js/[name].chunk.js'), //注意这里，用[name]可以自动生成路由名称对应的js文件
    },
	mode: 'development',
	devtool: 'source-map',
	module: {
	    rules: [{
	        test: /\.(css|scss)$/,
	        use: [
	          	'style-loader',
	          	'css-loader',
		        {
		            loader: 'postcss-loader',
		            options: {
		              	plugins: function () {
		                	return [autoprefixer({ browsers: ["> 1%","last 2 versions","not ie <= 8"] })];
		              	}
		            }
		        },
	          	{
		            loader: 'sass-loader',
		            options: {
		              	// includePaths: [path.resolve(__dirname, "../node_modules/compass-mixins/lib")]
		            }
	          	}
	        ]
	    }]
	},
	plugins: [
	    new webpack.HotModuleReplacementPlugin(), //热更新插件
	    new HtmlWebpackPlugin({
	      	template: path.resolve(__dirname, '../src/index.html'), //依据的模板
	      	filename: 'index.html', //生成的html的文件名
	      	inject: true, //注入的js文件将会被放在body标签中,当值为'head'时，将被放在head标签中
	    }),
	   
	    new OpenBrowserPlugin({ url: 'http://localhost:' + hostPort })
	],
	devServer: {
		historyApiFallback: true,
		contentBase: path.resolve(__dirname, '../src'),	// 提供静态文件路径
		publicPath: '/',
		host: '0.0.0.0',
		port: hostPort,
		compress: true, // gzip压缩
		// 热替换配置
	  	hot: true,
	  	inline: true,
		// 后台日志文字颜色开关
	  	stats: { 
	    	colors: true
        },
        // 反向代理api设置
		proxy: [
			{
			    context: ["/api"],
			    target: "http://120.77.155.220:3000", // 测试服务器
			    changeOrigin: true // 必须配置为true，才能正确代理
			},
			// {
			//     context: ["/api"],
			//     target: "http://127.0.0.1:3000", // 测试服务器
			//     changeOrigin: true // 必须配置为true，才能正确代理
            // }
			
		],
	  	after: function (app) {
	  		console.log('Listening at 0.0.0.0:' + hostPort + '...');
	  	}
	}

});

