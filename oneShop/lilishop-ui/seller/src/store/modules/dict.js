const dict = {
    state: {
        // 经常需要读取的数据字典
        sex: [],
        messageType: [],
        priority: [],
        leaveType: []
    },
    mutations: {
        // 设置值的改变方法
        setSex(state, list) {
            state.sex = list;
        },
        setMessageType(state, list) {
            state.messageType = list;
        },
        setPriority(state, list) {
            state.priority = list;
        },
        setLeaveType(state, list) {
            state.leaveType = list;
        },
    }
};

export default dict;
