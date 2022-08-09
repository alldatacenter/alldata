// 透明传输
// let logger = require('../util/log4js').logger('passThrough');
let request = require('request'); // 发送http请求
let Config = require('../conf/index');

module.exports = {
    // k8s server
    server: function(req, res, next) {
        let path = req.path.slice(4); // 例 /api/v1/* => /v1/*
		request({
            url: Config.network.serverHost + path,
            method: req.method,
            json: true,
            body: req.body
		}, (error, response, data) => {
            
        }).pipe(res);
    },
    server2: function(req, res, next) {
        let path = req.path.slice(7); // 例 /api/s2/v1/* => /v1/*
		request({
            url: Config.network.serverHost2 + path,
            method: req.method,
            json: true,
            body: req.body
		}, (error, response, data) => {
            
        }).pipe(res);
    },
    server3: function(req, res, next) {
        let path = req.path.slice(7); // 例 /api/s3/v1/* => /v1/*
		request({
            url: Config.network.serverHost3 + path,
            method: req.method,
            json: true,
            body: req.body
		}, (error, response, data) => {
            
        }).pipe(res);
	},
	
}