let path = require('path')
// 通过NODE_ENV来设置环境变量，如果没有指定则默认为生产环境
let env = process.env.NODE_ENV || 'production';
env = env.toLowerCase();

let file_dev = require('./development');
let file_prod = require('./production');
let _config = env == 'development' ? file_dev : file_prod;
module.exports = _config;
