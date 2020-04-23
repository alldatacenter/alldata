// 连接MySQL
var mysql = require('mysql');
var config = require('./index')
var pool = mysql.createPool(config.database);
var Async = require('async');

// var log4js = require('../util/log4js');
// var logger = log4js.logger('db');
console.log("数据库环境",config.database);

module.exports = {
	// 处理一个数据库事务
	transactionHandler: (callback) => {
		pool.getConnection(function(err, connection) {
			if (err) {
				console.log("getConnection error", err);
				return callback(err, null);
			}

			connection.beginTransaction(function(err) {
				if (err) {
					console.log("beginTransaction error", err);
					return callback(err, null);
				}

				console.log("开始执行transaction");
				callback(null, connection);
			})
		});
	},

	// 处理事务的结果
	transactionResultHandler: (err, connection, callback) => {
		if (err) {
			connection.rollback(() => {
				console.log("某事务出错，回滚成功, Error: ", err);
				connection.release();
			});
			return;
		}

		connection.commit(err => {
			if (err) {
				connection.rollback(() => {
					console.log("commit出错，回滚成功, Error: ", err);
					connection.release();
				});
				return;
			}
			connection.release();
			callback();
		});
	},

	// 旧版数据库连接
	query: (sql, callback) => {
		pool.getConnection(function(err,connection){
			if(err){
				console.log("db err", err)
				console.log("--------db 连接失败 ----------");
				logger.error("DB CONNECTION ERROR",err);
				
				console.log("--------db 连接失败 error log end ----------");
			}
			connection.beginTransaction(function(err) {
				if(err){
					console.log("db connection error",err);
					callback(err,null);
				}else{
					connection.query(sql,function(qerr,rows){
						//释放连接
						connection.release();
						//事件驱动回调
						if (qerr) {
							connection.rollback(function() {
								console.log("sql回滚成功1",qerr)
								callback(qerr,null);
								throw qerr;
							});
						}
						connection.commit(function(err) {
							if (err) {
								connection.rollback(function() {
									console.log("sql回滚成功")
									throw err;
								});
							}
							callback(qerr,rows);
						});
					});
				}
			})
			
		});
	},
}