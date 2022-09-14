/**
 * Created by caoshuaibiao on 19-3-5.
 * 节点数据模型
 **/
import _ from 'lodash';
const DEBUGGER_KEY = "__DEBUGGER_NODE_PARAMS";
export default {
    namespace: 'node',
    state: {
        //节点参数,改变后会刷新相关页面
        nodeParams: {},
        //用户参数,用户操作参数,不刷新当前页面,只是存储用户操作的参数(可能影响非展示类的组件数据)
        userParams: {}
    },
    effects: {

    },
    reducers: {

        resetParamContext(state) {
            return {
                nodeParams: {},
                userParams: {}
            }
        },

        initParams(state, { initParams }) {
            let paramsSet = Object.assign({}, state.nodeParams, initParams.nodeParams);
            window[DEBUGGER_KEY] = {
                nodeParams: paramsSet,
                userParams: {}
            };
            return {
                nodeParams: paramsSet,
                userParams: {}
            }
        },
        updateParams(state, { paramData, outputs }) {
            let isNodeParams = true, params = {};
            //存在输出参数映射,进行参数映射解析
            if (outputs) {
                let isList = false;
                outputs.forEach(output => {
                    let { name, valueIndex, targetType, reload, valueType } = output;
                    let tparams = Array.isArray(paramData) ? paramData : [paramData], targetVaules = [];
                    //action对接作业的目标参数的特殊定义
                    if (targetType) {
                        tparams.forEach(param => {
                            //一个目标定义全集
                            targetVaules.push({
                                value: _.get(param, valueIndex),
                                label: _.get(param, valueIndex),
                                path: _.get(param, valueIndex),
                                id: _.get(param, valueIndex),
                                key: _.get(param, valueIndex),
                            });
                        });
                        params[name] = { [targetType]: targetVaules };
                        isNodeParams = false;
                    } else if (valueType === 'List') {
                        params[name] = tparams;
                        isList = true;
                    } else {
                        tparams.forEach(param => {
                            targetVaules.push(_.get(param, valueIndex));
                        });
                        params[name] = targetVaules;
                    }
                    if (reload === false || reload === "false") {
                        isNodeParams = false;
                    }
                });
                if (!isList) {
                    Object.keys(params).forEach(key => {
                        let pv = params[key];
                        if (Array.isArray(pv) && pv.length === 1) {
                            params[key] = pv[0];
                        }
                    });
                }
                if (!Array.isArray(paramData)) {
                    _.assignIn(params, paramData);
                }
            } else {
                if (Array.isArray(paramData)) {
                    /*if(paramData.length===1){
                        params=paramData[0];
                    }else if(paramData.length>1){
                        params={"__selected":paramData}
                    }*/
                    params = { "__selected": paramData }
                } else {
                    params = paramData;
                }
            }
            let paramsSet = Object.assign({}, state.nodeParams, params), userParamsSet = Object.assign({}, state.userParams, params);
            window[DEBUGGER_KEY] = {
                nodeParams: paramsSet,
                userParams: userParamsSet
            };
            if (isNodeParams) {
                return {
                    ...state,
                    nodeParams: paramsSet,
                }
            } else {
                return {
                    ...state,
                    userParams: userParamsSet,
                }
            }
        },

    },
}
