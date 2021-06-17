const webpack = require('webpack');
const path = require('path');
const config = require('./config.js');
const rm = require('rimraf');  //node环境下rm -rf的命令库

const fs = require('fs')

const nodeModules = {}
fs.readdirSync('node_modules')
    .filter(function (x) {
        return [ '.bin' ].indexOf(x) === -1
    })
    .forEach(function (mod) {
        nodeModules[ mod ] = 'commonjs ' + mod
    })

const webpackConfig  = {
	mode: 'production',
	target: 'node',
	node: {
		__filename: true,
		__dirname: true,
	},
	externals: nodeModules,
	entry: {
	    app: [
	      	//增加对es6 api的支持，如axios里的promise
	      	// 'babel-polyfill',
	      	path.resolve(__dirname, '../app.js')       // 定义入口文件
	    ],
  	},
	output: {											// 定义出口
		publicPath: config.prod.publicPath,
		path: config.prod.root,
		filename: '[name].js',
	},
	resolve: {                                    // 指定可以被 import 的文件后缀
	    extensions: ['.js'],
	    // alias: {                                    // 配置常用路径
	    //   	src: path.resolve(__dirname, '../src'),
	    //   	views: 'src/views',
	    // }
  	},
	module: {
		rules: [{
			test: /\.(js|jsx)$/,
			loader: 'babel-loader',
			include: [path.resolve(__dirname, '../src'), path.resolve(__dirname, '../server')],
		}, {
			test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
	        loader: 'url-loader',
	        options: {
	          	limit: 10000,
	        }
		}, {
	        test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
	        loader: 'url-loader',
	        options: {
	          	limit: 10000,
	        }
      	}, {
	        test: /\.(css|scss)$/,
	        use: [
	            'css-loader/locals',
	            'sass-loader',
	        ]
	    }]
	},
	
};

// 通过node删除旧目录，生成新目录
rm(path.join(config.prod.root, 'app.js'), err => {
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

