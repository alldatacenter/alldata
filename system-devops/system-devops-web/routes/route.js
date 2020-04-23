/**
 * 后端API
 */
let express = require('express');
let route = express.Router();
let tokenAuth = require('../middleware/tokenAuth'); // 用户验证
let passThrough = require('../middleware/passThrough'); // 用户验证
let ctrl = require('../controller/controller'); // 业务层


/******************** API ********************/

// ------------------ 测试
// 测试数据
route.all('/api/test', tokenAuth.auth, ctrl.test.write);

// ------------------ 登录模块
route.post('/api/login', ctrl.login.login); // 登录
route.post('/api/logout', ctrl.login.logout); // 注销

// ------------------ 项目数据
route.all('/api/v1/*', tokenAuth.auth, passThrough.server); // 透传
route.all('/api/s2/v1/*', tokenAuth.auth, passThrough.server2); // 透传（机器配置）
 route.all('/api/s3/v1/*', tokenAuth.auth, passThrough.server3); // 透传（日志查询）














/******************* 页面路由 *******************/

route.get('*', function(req, res, next) {
	if (req.url.indexOf('api') < 0) {
		res.render('index');
	} else {
		next();
	}
});

module.exports = route;