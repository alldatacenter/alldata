const webpack = require('webpack');
const path = require('path');
const merge = require('webpack-merge');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const autoprefixer = require('autoprefixer');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const OptimizeCSSPlugin = require('optimize-css-assets-webpack-plugin');
const rm = require('rimraf');  //node环境下rm -rf的命令库
const baseWebpackConfig = require('./webpack.config.js');
const config = require('./config.js');

const webpackConfig = merge(baseWebpackConfig, {
  	entry: {
        // vendor: ['react-router', 'axios', 'prop-types'],
        // until: ['codemirror', 'react-codemirror2'],
	    main: [
	      	//增加对es6 api的支持，如axios里的promise
	      	// 'babel-polyfill',
	      	path.resolve(__dirname, '../src/main.pro.js')       // 定义入口文件
	    ],
    },
    output: {											// 定义出口
		publicPath: config.prod.publicPath,
		path: config.prod.root,
		filename: path.posix.join(config.prod.subDirectory, 'js/[name].[contenthash:5].js'),
	    chunkFilename: path.posix.join(config.prod.subDirectory, 'js/[name].[chunkhash:5].chunk.js'), //注意这里，用[name]可以自动生成路由名称对应的js文件
    },
  	mode: 'production',
  	module: {
	    rules: [{
	        test: /\.(css|scss)$/,
	        use: ExtractTextPlugin.extract({
		        use: [
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
			                includePaths: [path.resolve(__dirname, "../node_modules/compass-mixins/lib")]
			            }
		            }
		        ],
		        fallback: 'style-loader'
	        })
	    }]
  	},
	plugins: [
	    //将js中引入的css分离的插件
	    new ExtractTextPlugin({
              filename: path.posix.join(config.prod.subDirectory, 'css/[name].[chunkhash:5].css'),
            //   allChunks: false // 指明为false，否则会包括异步加载的 CSS
	    }),

	    //压缩提取出的css，并解决ExtractTextPlugin分离出的js重复问题(多个文件引入同一css文件)
	    new OptimizeCSSPlugin(), 

	    //html模板配置
	    new HtmlWebpackPlugin({
			template: path.resolve(__dirname, '../src/index.html'),
			filename: 'index.html', //生成的html的文件名
			inject: true, //注入的js文件将会被放在body标签中,当值为'head'时，将被放在head标签中
			// hash: true, //为静态资源生成hash值
			// minify: {  //压缩配置
			//   removeComments: true, //删除html中的注释代码
			//   collapseWhitespace: false,  //删除html中的空白符
			//   removeAttributeQuotes: false  //删除html元素中属性的引号
			// },
			// chunksSortMode: 'dependency' //按dependency的顺序引入
	    }),
	  
	]
});

// 通过node删除旧目录，生成新目录
rm(path.join(config.prod.root, config.prod.subDirectory), err => {
	if (err) throw err;
	webpack(webpackConfig, function (err, stats) {
		if (err) throw err;
		process.stdout.write(stats.toString({
			colors: true,
			modules: false,
			children: false,
			chunks: false,
			chunkModules: false
		}) + '\n\n');
		console.log(' Build complete.\n');
	});
});