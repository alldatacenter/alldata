// let tokenAuth = require('../middlewares/tokenAuth')
let request = require('request'); //发送http请求,login请求
let Async = require('async');
let logger = require('../../util/log4js').logger('login');
let Util = require('../../util/util');
let tokenAuth = require('../../middleware/tokenAuth')


module.exports = {
	// 登录验证
	login: function(req, res) {
		logger.info("登录接口参数", req.body);
		let method = req.method.toUpperCase(),
			username = req.body.username,
			password = req.body.password;
		let proxy_url = 'http://www.wulinhao.top/auth';
		let options = {
			headers: {
				"Connection": "keep-alive;",
				"Content-Type": "application/json;charset=UTF-8;",
			},
			url: proxy_url,
			method: method,
			json: true,
			body: req.body,
		};
		request(options, callback);

		function callback(error, response, data) {
			logger.info(data, response.statusCode); //http请求状态码
			if(!error && response.statusCode == 200) {
				logger.debug('------login接口返回成功------');
				var state = data.state;
				if(data.state.code == 0) {
                    // 获取用户权限 todo 。。。

					let token = tokenAuth.setToken({username: username}); //生成token
                    let resData = {
                        "token": token,
                        "username": username,
                    };
                    res.cookie('username', resData.username, {
                        expires: new Date(Date.now() + 1000 * 60 * 60 * 24 * 7)
                    }); //设置cookie
                    res.cookie('token', resData.token, {
                        expires: new Date(Date.now() + 1000 * 60 * 60 * 24 * 7)
                    }); //设置cookie
						
					Util.resHandler(res, {
                        isSuccess: true,
                        data: resData,
                    });
                    
				} else {
					//登录失败，密码错误或账号不存在
					logger.error('-------登录失败------');
                    logger.error('用户账号：', username);
                    Util.resHandler(res, {
                        isSuccess: false,
                        msg: "登录失败，账号或密码错误"
                    });
				}
			} else {
				logger.error('-------登录出错------');
				logger.error(JSON.stringify(error, response, data));
				logger.error('-------登录出错end------');
				Util.resHandler(res, {
                    isSuccess: false,
                    msg: "登录失败，账号或密码错误"
                });
			}
		}
    },
    // 退出登录
    logout: function(req, res) {
        res.cookie('username', '', {
            expires: new Date(Date.now() - 1000)
        }); //设置cookie
        res.cookie('token', '', {
            expires: new Date(Date.now() - 1000)
        }); //设置cookie
        // res.status(203);
        Util.resHandler(res, {
            isSuccess: true,
            msg: "已退出登录"
        });
    }
}