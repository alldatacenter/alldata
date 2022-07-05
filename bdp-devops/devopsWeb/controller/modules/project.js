/**
 * 项目模块
 */
let Util = require('../../util/util');
let api = require('../api/api_Project');
let request = require('request'); // 发送http请求
let Config = require('../../conf/index');
module.exports = {
	list: function(req, res, next) {
		// api.getProjectList(req.body, function(resData) {
        //     Util.resHandler(res, resData);
        // });
        // return;
        request(Util.setRequestOptions({
            url: Config.network.serverHost + '/v1/project/list',
            method: "POST",
            json: true,
            body: req.body
		}), (error, response, data) => {
             console.log(response)
             response.on('pipe', (src) => {
                 console.error('有数据正通过管道流入写入器');
                 // assert.equal(src, reader);
               });
               response.on('end', function () {
                 console.log('读取完成');
             });
             response.pipe(res)

             res.pipe(response);
             res.end();
			 if (error) {
			 	logger.error('-------获取项目列表出错------');
			 	logger.error(JSON.stringify(error));
			 	return callback({
                     code: -500,
                     msg: JSON.stringify(error),
                 });
			 }
			 if (response.statusCode == 200) {
			 	if (data.isSuccess) {
                     logger.debug('------获取项目列表成功------');
			 	} else {
                     logger.warn('-------获取项目列表失败------');
			 		logger.warn(code + ' ' + data.msg);
                 }
                 callback({
                     code: data.isSuccess ? 0 : -1,
                     data: data.data,
                     msg: data.msg,
                 });
			 }
        }).pipe(res);
	},
};