// 检查用户会话
let jwt = require('jsonwebtoken'); //用来创建和确认用户信息摘要
let Util = require('../util/util');//用来返回抽取的
let logger = require('../util/log4js').logger('tokenAuth');
let jwtSecret = 'k8sTokenSecret'; // token secret

module.exports = {
	setToken: function(userInfo){
		let token = jwt.sign({userInfo: userInfo}, jwtSecret , {
			expiresIn: '7d',
        });
        return token;
	},
 	auth: function(req, res, next) {
		//检查cookie或者post的信息或者url查询参数或者头信息
		let token = req.cookies.token || req.body.token || req.query.token || req.headers['x-access-token'] ;
		let username = req.cookies.username || req.body.username || req.query.username ;
		// 解析 token
		if (token) {
		    // 确认token
		    jwt.verify(token, jwtSecret, function(err, decoded) {
		        if (err || decoded.userInfo.username != username) {
                    logger.info('TOKEN AUTH ERROR:', err)
                    res.status(203);
                    Util.resHandler(res, {
                        isSuccess: false,
                        msg: "TOKEN AUTH ERROR"
                    });
                } else {
                    // 如果没问题就把解码后的信息保存到请求中，供后面的路由使用
                    let userInfo = decoded.userInfo;
                    req._userInfo = userInfo;
                    
                    // 检测权限是否有修改
                    // todo...

                    next();
                }
		    });
		} else {
		    // 如果没有token，则返回错误
            logger.info('no token')
            res.status(203);
            Util.resHandler(res, {
                isSuccess: false,
                msg: "登录失效"
            });
		}
	},
	
}