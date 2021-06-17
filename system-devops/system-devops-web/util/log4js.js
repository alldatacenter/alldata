var express = require('express');
var log4js = require('log4js');
var app = express();

console.log("当前环境",process.env.NODE_ENV);
var logLevel = process.env.NODE_ENV === 'development' ? "debug" : "info";

log4js.configure({
	appenders: {
	    console: { type: 'stdout' },
	    normal: {
	    	type: 'dateFile', 
    		filename: 'logs/application' ,
    		maxLogSize: 1024*1024*10, //只有type为file时生效
    		backups: 10,//只有type为file时生效，表示超过10个log文件时，删除旧的，保留最新的10个
    		pattern: "_yyyy-MM-dd.log",
    		alwaysIncludePattern: true
    	},
	    log_error: {
	    	type: "file",  
            filename: "logs/error",
			maxLogSize: 1024*1024*100, //只有type为file时生效
	    },
	    error: {
	    	type: 'logLevelFilter',
	    	level: 'warn',
	    	appender: 'log_error',
	    }
	},
	categories: {
	    default: { appenders: ['console', 'normal', 'error'], level: logLevel },
    },
	replaceConsole: true,
	pm2: process.env.NODE_ENV === 'production', // 正式环境
	pm2InstanceVar: 'INSTANCE_ID'
});

exports.logger=function(name){
	var logger = log4js.getLogger(name);
	return logger;
}

//解决replaceConsole配置还是无效的方案，将console.log()一并打包
var logger = log4js.getLogger('console');
console.log = logger.info.bind(logger);

// app.use(log4js.connectLogger(this.logger('normal'), {level: "debug"}));


