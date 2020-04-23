let path = require('path')
// 通过NODE_ENV来设置环境变量，如果没有指定则默认为生产环境
let env = process.env.NODE_ENV || 'production';
env = env.toLowerCase();

// var log4js = require('../util/log4js');
// var logger = log4js.logger('conf/index.js');

// logger.info("current env:",process.env.NODE_ENV,env)
// 载入配置文件
// let file = path.resolve(__dirname,env);
// let _config = require(file);

let file_dev = require('./development');
let file_prod = require('./production');
let _config = env == 'development' ? file_dev : file_prod;

//logger.info(_config);

module.exports = _config;
