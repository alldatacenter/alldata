module.exports = {
    /**
     * 返回值处理
     * res: (required)
     * resData: {
     *  code: (not required)，自定义返回代号，默认错误是 -1，成功是 0
     *  msg: (not required)，自定义返回描述，默认错误是 'error', 成功是 'success'
     *  data: (not required)，json数据
     * }
     * 
     */
    resHandler: function (res, resData) {
        let json = {
            isSuccess: resData.isSuccess,
            msg: resData.msg || (resData.isSuccess ? 'success' : 'request error')
        };
        if (resData.isSuccess && resData.data != undefined) json.data = resData.data;
        res.json(json);
    },

    // 设置request options
    setRequestOptions: (options) => {
        // options.headers = {
        //     "Connection": "keep-alive;",
        //     "Content-Type": "application/json;charset=UTF-8;",
        // };
        
        return options;
    },
};