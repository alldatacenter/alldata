require('es6-promise').polyfill(); // 支持axios里的promise（兼容ie）
import axios from 'axios';
import { message } from 'antd';
import { browserHistory } from 'react-router';
import md5 from 'md5';
/**
 * interface function
 *
 */
const InterfaceFn = {
	origin: '', // 生产环境修改接口origin
	handleResponse: function (res, cb200) {
		switch (res.status) {
			case 200:
				cb200(res.data);
				break;
			case 203:
                message.destroy();
                browserHistory.push('/login');
                message.error('登录失效，请重新登录');
				break;
			default:
                message.destroy();
                message.error('ERROR ' + res.status);
				break;
		}
	},
	handleCatchError: function (res, api, cb) {
		cb();
		if (res.response) {
			message.error(api + ' ERROR: ' + res.response.status);
			console.log("%c " + api + " ERROR", "background: red; color: #fff", res.response.status);
			return;
		}
		console.log("%c CATCH ERROR", "background: red; color: #fff", res);
    },
    
    // axios回调抽取
	axiosThen: function (response, succCallback, errorCallback, apiName, msgHide) {
		msgHide();
		this.handleResponse(response, (resData) => {
			console.log("%c " + apiName + " response", "background: green; color: #fff", resData);
			if (resData.isSuccess) {
				succCallback(resData.data);
			} else {
				message.error(resData.msg);
				if (errorCallback) errorCallback(resData);
			}
		});
	},
	axiosCatch: function (error, apiName, msgHide) {
		this.handleCatchError(error, apiName, () => {
			msgHide();
		});
	},

	// 登入
	login: function (reqData, succCallback, errorCallback) {
        let data = {
			username: reqData.username,
			password: md5(reqData.password)
			// pass: md5(reqData.password)
		};
        let apiName = 'login';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('登录中...', 0);

		axios({
			url: this.origin + '/api/login',
			method: 'POST',
			data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},

	// 登出
	logout: function (succCallback, errorCallback) {
        let apiName = 'logout';
        console.log(`%c ${apiName} request`, "background: blue; color: #fff");
		const msgHide = message.loading('登出中...', 0);

		axios({
			url: this.origin + '/api/logout',
			method: 'POST'
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},

    // 获取项目列表
	getProjectList: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'getProjectList';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/v1/project/list',
			method: 'POST',
			data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
    },
    // 获取App列表
	getAppList: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'getAppList';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/v1/app/list',
			method: 'POST',
			data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	// 获取 cluster
    getCluster: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'getCluster';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/v1/cluster/list',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
    },
	
	//获取全部机器列表
	getAllMachine: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'getAllMachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/s2/v1/machine/list',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	//删除机器
	deleteMachine: function (reqData, succCallback, errorCallback){
		let data = reqData;
        let apiName = 'deleteMachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);
		axios({
			url: this.origin + `/api/s2/v1/machine/delete/${data.id}`,
            method: 'POST',
            data: data.id
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},

	//添加机器
	addMachine: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'addMachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/s2/v1/machine/create',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	
	//更新机器
	updateMachine: function (reqData, succCallback, errorCallback){
		let data = reqData;
        let apiName = 'updateMachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);
		axios({
			url: this.origin + `/api/s2/v1/machine/update/${data.id}`,
            method: 'POST',
            data: data.id
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},

	//获取项目机器列表
	getProjectMachine: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'getProjectMachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/s2/v1/projectmachine/list',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	//添加项目机器
	addProjectmachine: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'addProjectmachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/s2/v1/projectmachine/create',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	//移除项目机器
	deleteProjectMachine: function (reqData, succCallback, errorCallback) {
		let data = reqData.id;
        let apiName = 'deleteProjectMachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + `/api/s2/v1/projectmachine/delete/${data}`,
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
    // 获取App appcluster
    getAppcluster: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'getAppcluster';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/v1/appcluster/list',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
    },
	//获取app机器列表
	getAppMachine: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'getAppMachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/s2/v1/appmachine/list',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	//添加App机器
	addAppMachine: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'addAppMachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/s2/v1/appmachine/create',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	//移除APP机器
	deleteAppMachine: function (reqData, succCallback, errorCallback) {
		let data = reqData.id;
        let apiName = 'deleteAppMachine';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + `/api/s2/v1/appmachine/delete/${data}`,
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	//获取非继承机器的App列表
	getAppNotExtendsList: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'getAppNotExtendsList';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/s2/v1/appnotextends/list',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	//继承机器
	cancelAppNotExtends: function (reqData, succCallback, errorCallback) {
		let data = reqData.id;
        let apiName = 'cancelAppNotExtends';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + `/api/s2/v1/appnotextends/delete/${data}`,
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
	//取消继承
	addAppNotExtends: function (reqData, succCallback, errorCallback) {
		let data = reqData;
        let apiName = 'addAppNotExtends';
		console.log(`%c ${apiName} request`, "background: blue; color: #fff", data);
		const msgHide = message.loading('加载中...', 0);

		axios({
			url: this.origin + '/api/s2/v1/appnotextends/create',
            method: 'POST',
            data: data
        })
        .then((response) => this.axiosThen(response, succCallback, errorCallback, apiName, msgHide))
		.catch((error) => this.axiosCatch(error, apiName, msgHide));
	},
};

export default InterfaceFn;