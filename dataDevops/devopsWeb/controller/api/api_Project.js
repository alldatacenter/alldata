/**
 * Desc: 获取项目数据
 */

let Util = require('../../util/util'); // 定义一些常用方法
let request = require('request'); // 发送http请求
let Config = require('../../conf/index');
let logger = require('../../util/log4js').logger('api_Project.js');

module.exports = {
   	// 获取项目列表
	getProjectList: (reqData, callback) => {
        let options = Util.setRequestOptions({
            url: Config.network.serverHost + '/v1/project/list',
            method: "POST",
            json: true,
            body: reqData
		});
		request(options, (error, response, data) => {
			if (error) {
                logger.warn(JSON.stringify(options));
                logger.error(JSON.stringify(error));
				return callback({
                    code: -500,
                    msg: JSON.stringify(error),
                });
			}
			if (response.statusCode == 200) {
				if (!data.isSuccess) {
                    logger.warn(JSON.stringify(options));
                    logger.warn(JSON.stringify(data));
                }
                callback({
                    code: data.isSuccess ? 0 : -1,
                    data: data.data,
                    msg: data.msg,
                });
			}
		});
    },
};