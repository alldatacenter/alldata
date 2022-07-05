/**
 * 测试模块
 */
module.exports = {
	write: function(req, res, next) {
		console.log(req.url)
		res.send(200, {code: -1, data: 'data', msg: 'error'});
	},
};