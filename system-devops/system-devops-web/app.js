if (process.env.NODE_ENV == 'development') {
	require('babel-polyfill');

	// Javascript required hook
	// require('babel-register')({
	// 	presets: ['es2015', 'stage-2']
	// });
}


let express = require('express');
let path = require('path');
let bodyParser = require('body-parser');
let cookieParser = require('cookie-parser');//cookie
let ejs = require('ejs');
let logger = require('./util/log4js').logger('app.js');
let Config = require('./conf/index');

logger.debug(process.env.NODE_ENV);

let app = express();

/* 允许跨域 ，开发环境可以把它允许跨域*/
if(process.env.NODE_ENV == 'development'){
	logger.debug('开发环境允许跨域');
	app.all('*', function(req, res, next) {  
	    res.header("Access-Control-Allow-Origin", "*");  
	    res.header("Access-Control-Allow-Headers", "X-Requested-With,Content-Type");  
	    res.header("Access-Control-Allow-Methods","PUT,POST,GET,DELETE,OPTIONS");  
	//  res.header("Content-Type", "application/json;charset=utf-8");  
	    next();  
	});
}

app.set('views', path.join(__dirname, './dist'));	// 页面目录
app.engine('html', ejs.__express);	// 模板引擎
app.set('view engine', 'html');

app.use(cookieParser());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: false}));	// 用于提交表单
app.use(express.static(path.join(__dirname, './dist'))); // 静态文件目录

app.use(require('./routes/route'));	// API接口


let port = Config.network.port || process.env.PORT || 3000;
app.listen(port, function () {
	console.log('server start on port ' + port);
});




module.exports = app;