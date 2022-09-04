/**
 * Created by caoshuaibiao on 2019/1/7.
 * 运维动作行为定义
 */
import React from 'react';
import { CloseCircleOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import {
    Spin,
    Button,
    Card,
    Modal,
    Tooltip,
    Drawer,
    message,
    Alert,
    Popover,
    Input,
    Radio,
} from 'antd';
import SimpleForm from "../../components/FormBuilder/SimpleForm";
import Action from './model/Action';
import APIAction from './model/APIAction';
import actionService from '../services/actionService';
import { connect } from 'dva';
import safeEval from '../../utils/SafeEval';
import FilterBar from './FilterBar';
import FilterForm from './FilterForm';
import TabFilter from './tabFilter';
import OamStepAction from './OamStepAction';
import MixFilterBar from './MixFilterBar';
import moment from 'moment';
import JSXRender from "../../components/JSXRender";
import localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";
import httpClient from '../../utils/httpClient';
import FormElementType from '../../components/FormBuilder/FormElementType';
import _ from 'lodash'
import cacheRepository from '../../utils/cacheRepository';

const sizeMapping = {
    "small": {
        percent: 0.4,
        min: 360,
        labelSpan: 6
    },
    "default": {
        percent: 0.6,
        min: 640,
        labelSpan: 4
    },
    "large": {
        percent: 0.8,
        min: 900,
        labelSpan: 3
    },
};


@connect(({ node, global }) => ({
    userParams: Object.assign({}, { __currentUser__: global.currentUser }, node.userParams),
    userInfo: global.currentUser,
    product: global.currentProduct,
    nodeParams: node.nodeParams
}))
class OamAction extends React.Component {

    constructor(props) {
        super(props);
        let { actionData, userInfo, product, nodeParams, userParams, actionParams } = props, action = null;
        let actionInitParams = Object.assign({}, nodeParams, util.getUrlParams(), userParams, actionParams);
        //config中存在变量占位,需要进行替换
        let configMetaConf = actionData.config, runtimeData = actionData.runtimeData, configMeta;
        if (configMetaConf) {
            configMeta = util.renderTemplateJsonObject(configMetaConf, actionInitParams, false);
        }
        if (configMeta && configMeta.actionType) {
            if (configMeta.actionType === Action.JOB) {

            } else if (configMeta.actionType === Action.API) {
                action = new APIAction(configMeta, userInfo, product, nodeParams);
            } else if (configMeta.actionType === Action.FLOW) {

            } else {
                action = new Action(configMeta, userInfo, product, nodeParams);
            }
        }
        action.elementId = actionData.elementId;
        action.id = actionData.id;
        action.nodeId = props.nodeId || actionInitParams.__nodeId__;
        //自定义的工单明细页面变量需要保持原样
        if (action.detailComponents && action.detailComponents.length) {
            action.detailComponents = configMetaConf.detailComponents;
        }
        let scenes = action.scenes || [], sceneTitle = '';
        if (runtimeData && runtimeData.scenes) {
            scenes = scenes.concat(runtimeData.scenes);
        }
        let urlParams = util.getUrlParams(), defaultScene = false, empty = false;
        if (scenes && scenes.length) {
            //重入时候的选中
            scenes.forEach(s => {
                let isDefault = true, svs = s.values;
                Object.keys(svs).forEach(sk => {
                    if (urlParams[sk] && svs[sk] !== urlParams[sk] || (!urlParams.hasOwnProperty(sk) && svs[sk])) {
                        isDefault = false
                    }
                });
                if (Object.keys(svs).length === 0) {
                    isDefault = false;
                    empty = s;
                }
                if (isDefault && !defaultScene) {
                    defaultScene = s;
                    sceneTitle = s.title;
                }
            });
        }
        if (!defaultScene && empty) {
            sceneTitle = empty.title;
        }
        this.runtimeData = runtimeData;
        this.state = {
            loading: false,
            action: action,
            dockVisible: false,
            dispalyItems: [],
            feedbackVisible: false,
            remoteParams: {},
            feedbacks: [],
            configMeta: configMeta,
            sceneTitle: sceneTitle,
            scenes: scenes,
            beforeOpenWait: configMeta.beforeHandler && configMeta.beforeHandler.length > 150,
            cacheEffectValue: {}
        };
    }

    componentDidMount() {
        let { mode } = this.props, { action } = this.state;
        //this.previewAction();
        if ((mode === 'custom' || mode === 'step') && action) {
            this.previewAction();
        }
    }

    componentWillReceiveProps(nextProps) {
        let { dockVisible } = this.state;
        if (nextProps.userParams !== this.props.userParams && dockVisible) {
            this.previewAction();
        }
    }

    previewAction = () => {
        let { nodeParams, userParams, actionParams } = this.props, { configMeta } = this.state;
        //配置有远程获取参数接口
        if (configMeta && configMeta.sourceApi) {
            this.setState({
                loading: true,
                dockVisible: true
            });
            let sourceApi = configMeta.sourceApi;
            //完整的指定了参数获取接口
            if (sourceApi.length > 3) {
                let preParamsSet = Object.assign({}, nodeParams, userParams, actionParams);
                let { beforeSourceHandler, afterSourceHandler } = configMeta;
                if (beforeSourceHandler && beforeSourceHandler.length > 80) {
                    preParamsSet = safeEval("(" + beforeSourceHandler + ")(nodeParams)", { nodeParams: preParamsSet });
                }
                actionService.actionPreview(sourceApi, preParamsSet, configMeta.sourceMethod).then(preview => {
                    if (afterSourceHandler && afterSourceHandler.length > 80) {
                        let tranPreview = safeEval("(" + afterSourceHandler + ")(resData)", { resData: preview });
                        Object.assign(preview, tranPreview);
                    }
                    this.handleBeforeOpen(preview.params).then(results => {
                        this.setState({
                            remoteParams: preview.params,
                            feedbacks: preview.feedbacks,
                            loading: false,
                            feedbackVisible: preview.feedbacks,
                            dynamicItems: preview.dynamicItems,
                            ...results
                        }, () => this.preExecute());
                    });
                })
            }
        } else {
            this.handleBeforeOpen().then(results => {
                this.setState({
                    ...results
                }, () => this.preExecute());
            });
        }
    };

    handleBeforeOpen = (remoteParams = {}) => {
        let { nodeParams, userParams, actionParams = {} } = this.props, { configMeta } = this.state;
        let { beforeHandler } = configMeta, preExecute = this.preExecute;
        //定义了前置处理器,因内置了自定义的模板函数,因此只有大于模板长度再进行计算
        if (beforeHandler && beforeHandler.length > 150) {
            let result = safeEval("(" + beforeHandler + ")(mergeNodeParams)", { mergeNodeParams: Object.assign({}, nodeParams, userParams, actionParams, remoteParams) });
            let { message, transformParams, pass, dynamicItems, feedbacks } = result, beforeResults = {};
            //存在参数转换或者动态表单项
            if (transformParams) {
                beforeResults.remoteParams = Object.assign({}, remoteParams, transformParams);
            }
            if (dynamicItems) {
                beforeResults.dynamicItems = dynamicItems;
            }
            if (feedbacks) {
                beforeResults.feedbacks = feedbacks;
            }
            if (message) {
                Modal[result.type]({
                    title: result.title,
                    zIndex: 9999,
                    width: result.width ? result.width : 480,
                    content: <JSXRender jsx={result.message} />,
                });
            }
            if (pass) {
                return Promise.resolve(beforeResults);
            } else {
                return Promise.reject({});
            }
        } else {
            return Promise.resolve({});
        }

    };

    onClose = () => {
        this.setState({
            // loading: false,
            dockVisible: false,
            feedbackVisible: false,
            showHistory: false
        });
        this.props.onClose && this.props.onClose();
    };

    closeAndRefresh = (refresh) => {
        let { action } = this.state;
        if (action.refresh === 'Y' || refresh) {
            this.setState({
                loading: false,
                dockVisible: false,
                showHistory: false
            }, () => {
                let { dispatch } = this.props;
                dispatch({ type: 'node/updateParams', paramData: { ___refresh_timestamp: (new Date()).getTime() } });
                this.onClose();
            });
        } else {
            this.onClose();
        }

    };

    onFeedbackClose = () => {
        this.setState({
            feedbackVisible: false
        });
    };

    executeAction = (params, actionButton) => {
        //如果是输出类型的直接调用输出
        let { action, remoteParams, dispalyItems } = this.state, { nodeParams, userParams, actionParams, execCallBack, history, mode, stepsFormValues = {} } = this.props;
        if (mode === 'step') {
            Object.assign(params, stepsFormValues);
        }
        if (action.actionType === Action.OUTPUT) {
            if (history) {
                //不能全清空,存在超链过来的链接,且传递的参数是页面需要的参数非filter中的参数定义,因此清空filter定义的即可
                let urlParams = util.getUrlParams();
                dispalyItems.forEach(item => {
                    //不能全删除,对于高级查询里面的不能删除
                    let { name, defModel = {} } = item;
                    if (urlParams.hasOwnProperty(name) && defModel.displayType !== "advanced") {
                        delete urlParams[item.name];
                    }
                });
                //把数组值为数组的转为以","分割的字符串
                Object.assign(urlParams, params);
                let keys = Object.keys(urlParams);
                for (let k = 0; k < keys.length; k++) {
                    let key = keys[k];
                    if (Array.isArray(urlParams[key])) {
                        if (urlParams[key][0] && urlParams[key][0].valueOf) {
                            urlParams[key] = urlParams[key].map(m => m.valueOf()).join(",");
                        } else {
                            urlParams[key] = urlParams[key].join(",");
                        }
                    }
                    if (urlParams[key] && urlParams[key].valueOf) {
                        urlParams[key] = urlParams[key].valueOf();
                    }
                    if (urlParams[key] === null || urlParams[key] === undefined || urlParams[key] === "") {
                        delete urlParams[key];
                    }
                }
                history.push({
                    pathname: history.location.pathname,
                    search: util.objectToUrlParams(urlParams)
                });
            }
            this.setState({
                loading: false,
                dockVisible: false,
                sceneTitle: false
            }, () => {
                let { dispatch } = this.props, paramData = { ___refresh_timestamp: (new Date()).getTime() }, outputName = action.outputName;
                //存在输出变量定义用变量名包装,不存在的话直接输出,按照操作的定义去输出参数,里面存在转换
                action.parameterDefiner.updateParametersValue(params);
                let outputData = { ...params }, parameters = action.parameterDefiner.getParameters();
                parameters.forEach(param => {
                    if (params.hasOwnProperty(param.name)) {
                        outputData[param.name] = param.getValue();
                    }
                });
                if (outputName) {
                    paramData[outputName] = outputData;
                } else {
                    Object.assign(paramData, outputData)
                }
                dispatch({ type: 'node/updateParams', paramData: paramData });
            });
            return;
        }
        this.setState({
            loading: true
        });
        //合并参数来源一同传递给作业,输入参数及自定义数据源优先级最高
        let allParams = Object.assign({}, nodeParams, userParams, actionParams, remoteParams, params), executeAction = this.executeAction, hiddenLoading = this.hiddenLoading, closeAndRefresh = this.closeAndRefresh;
        if (action.enableApproval) {
            action.__approveCountersign = params.__approveCountersign;
        }
        let detailDescription = {};
        dispalyItems.forEach(item => {
            //不能全删除,对于高label级查询里面的不能删除
            let { name, label } = item;
            if (name !== "__approveCountersign" && label) {
                let value = params[name];
                detailDescription[label] = (value instanceof moment) ? value.format(window.APPLICATION_DATE_FORMAT || "YYYY-MM-DD HH:mm:ss") : value;
            }
        });
        //action详情参数的kv
        action.__detailFormKv = JSON.stringify(detailDescription);
        if (action.planEnable) {
            action.__actionPlanExecutionTime = params.__actionPlanExecutionTime;
        }
        action.execute(allParams, params, actionButton).then(result => {
            let feedbacks = result && result.feedbacks, receiveParams = (result && result.params) || {}, { modalWidth } = actionButton || {}, feedbackTitle = result && result.feedbackTitle;
            if (feedbacks && feedbacks.length > 0) {
                let modalInfo = feedbacks[0];
                if (modalInfo.type === 'modal') {
                    //新增操作直接支持服务端二次确认功能
                    let mockButton = false;
                    if (action.confirmUrl) {
                        mockButton = {
                            url: action.confirmUrl,
                            method: action.method,
                        }

                    }
                    //无需提示确认的情况下直接提交
                    if (modalInfo.pass) {
                        if (result.closeAction) {
                            closeAndRefresh();
                        }
                        if (mockButton) {
                            return executeAction(Object.assign({}, params, receiveParams), mockButton);
                        }
                        if (actionButton && actionButton.confirmUrl) {
                            return executeAction(Object.assign({}, params, receiveParams), Object.assign({}, actionButton, { url: actionButton.confirmUrl }));
                        } else {
                            closeAndRefresh();
                            return Promise.resolve();
                        }
                    } else {
                        Modal[modalInfo.messageType]({
                            title: modalInfo.title,
                            width: modalWidth ? modalWidth : 480,
                            content: <JSXRender jsx={modalInfo.message} />,
                            onOk() {
                                if (mockButton) {
                                    return executeAction(Object.assign({}, params, receiveParams), mockButton);
                                }
                                if (actionButton && actionButton.confirmUrl) {
                                    return executeAction(Object.assign({}, params, receiveParams), Object.assign({}, actionButton, { url: actionButton.confirmUrl }));
                                } else {
                                    closeAndRefresh();
                                    return Promise.resolve();
                                }
                            },
                            onCancel() {
                                hiddenLoading();
                            }
                        })
                    }
                } else if (modalInfo.type === 'redirect') {
                    closeAndRefresh();
                    let jPath = modalInfo.path;
                    if (jPath.includes("http")) {
                        window.open(jPath, "_blank");
                    } else {
                        if (history) {
                            history.push(jPath)
                        }
                    }
                } else if (modalInfo.type === 'progress') {
                    Modal.info({
                        title: feedbackTitle ? <JSXRender jsx={feedbackTitle} /> : '',
                        content: <div>缺失组件</div>,
                        width: modalWidth ? modalWidth : 480,
                        onOk() {
                            closeAndRefresh();
                            return Promise.resolve();
                        },
                    });
                } else {
                    Modal.info({
                        title: feedbackTitle ? <JSXRender jsx={feedbackTitle} /> : '',
                        content: <Feedback feedbacks={feedbacks} />,
                        width: modalWidth ? modalWidth : 480,
                        onOk() {
                            return Promise.resolve();
                        },
                    });
                    if (action.refresh === 'Y' || result.closeAction) {
                        closeAndRefresh();
                    } else {
                        this.setState({
                            loading: false,
                            remoteParams: Object.assign({}, remoteParams, receiveParams)
                        });
                    }
                }
            } else {
                // 提交文案返回内容自定义函数
                let responseText = '';
                if (action.responseHandler && action.responseHandler.length > 53) {
                    responseText = safeEval("(" + action.responseHandler + ")(response)", { response: result});
                }
                message.success(responseText? responseText : localeHelper.get('Success', "操作已提交"));
                if (execCallBack) execCallBack(allParams);
                //增加执行后跳转
                let jumpPath = action.jumpPath;
                if (jumpPath && jumpPath.length > 5) {
                    //util.renderTemplateString(confString,actionInitParams)
                    if (jumpPath.includes("$")) {
                        jumpPath = util.renderTemplateString(jumpPath, result);
                    }
                    if (jumpPath.includes("http")) {
                        window.open(jumpPath, "_blank");
                    }
                    let { history } = this.props;
                    if (history) {
                        history.push(jumpPath)
                    }
                    if (action.refresh === 'Y') {
                        this.closeAndRefresh();
                    } else {
                        this.onClose();
                    }
                } else if (action.actionType === Action.JOB) {
                    //作业记录存储有一定的延迟,因此延迟1秒后再显示作业记录
                    setTimeout(() => {
                        this.setState({
                            loading: false,
                            dockVisible: true,
                            showHistory: true
                        });
                    }, 1000)
                } else {
                    if (action.refresh === 'Y') {
                        this.closeAndRefresh();
                    } else {
                        this.onClose();
                    }
                }
            }
        }).catch(err => {
            this.hiddenLoading();
        });
    };

    hiddenLoading = () => {
        this.setState({
            loading: false,
        });
    };

    preExecute = () => {
        this.setState({
            beforeOpenWait: false,
            loading: true,
            dockVisible: true
        });
        let { action, remoteParams, dynamicItems } = this.state, { nodeParams, userParams, actionParams, displayType } = this.props, urlParams = util.getUrlParams();
        let defaultParams = Object.assign({}, nodeParams, urlParams, userParams, actionParams, remoteParams);
        window.__DEBUGGER_CURRENT_ACTION_PARAMS = Object.assign({}, defaultParams);
        action.setDefaultParams(defaultParams);
        let { approveGroupName } = defaultParams;
        action.getInputParamDef(approveGroupName).then(defs => {
            let dispalyItems = defs, initParams = {}, effectItemMapping = false;
            if (dynamicItems && dynamicItems.length > 0) {
                dynamicItems.forEach(di => {
                    let diString = JSON.stringify(di);
                    //存在变量替换
                    if (diString.includes("$(")) {
                        di = JSON.parse(diString, function (key, value) {
                            if (typeof value === 'string' && value.includes("$")) {
                                return util.renderTemplateString(value, defaultParams)
                            }
                            return value;
                        });
                    }
                    let item = dispalyItems.filter(d => d.name === di.name)[0];
                    if (item) {
                        Object.assign(item, di);
                    } else {
                        //增加可能存在的初始值
                        let initValue = defaultParams[di.name];
                        if ([4, 11, 14, 17].includes(di.type) && initValue && !Array.isArray(initValue)) {
                            initValue = initValue.split(",");
                        }
                        if (initValue) {
                            di.initValue = initValue
                        }
                        dispalyItems.push(di);
                    }
                }
                )
            }
            dispalyItems.forEach((di, index) => {
                if (action.readonly === 'Y') {
                    di.readonly = 2;
                }
                if (!di.order) {
                    di.order = index + 1;
                }
                //添加元素表单元素定义扩展元素
                di.extensionProps = {
                    formItems: dispalyItems,
                    history: this.props.history,
                    formInitParams: defaultParams,
                    closeAction: this.closeAndRefresh
                };
                if (di.initValue) {
                    initParams[di.name] = di.initValue
                }
                //存在影响其他表单的函数
                if (di.effectHandler && di.effectHandler.length > 10) {
                    effectItemMapping = effectItemMapping || {};
                    effectItemMapping[di.name] = di.effectHandler;
                    // 兼容关联分组模式
                    if (di.type === 84 && di.defModel && di.defModel.items) {
                        di.defModel.items.forEach(lit => {
                            effectItemMapping[lit.name] = di.effectHandler;
                        })
                    }
                }
            });
            dispalyItems.sort(function (a, b) { return a.order - b.order });
            //label hidden
            dispalyItems.forEach(item => {
                if (item.isHidden && item.isHidden === 1) {
                    item.label = '';
                }
                if (item.type === FormElementType.HIDDEN_INPUT) {
                    item.label = '';
                }
            })
            this.setState({
                dockVisible: true,
                loading: false,
                dispalyItems: dispalyItems,
                effectItemMapping: effectItemMapping
            }, () => {
                //过滤器存在特定名称的输出,在存在默认值的情况下需要输出一次
                if (action.actionType === Action.OUTPUT && Object.keys(initParams)) {
                    if (['form', 'filter', 'filterBar', 'mixFilterBar'].includes(displayType)) {
                        this.executeAction(initParams);
                    }
                }
            });

        })

    };

    handleCustomButton = (params, button) => {
        if (!params) return;
        let verifyFunction = button.verifyFunction, executeAction = this.executeAction, closeAndRefresh = this.closeAndRefresh;
        //粗滤,初始化函数就有120的字符,因此认为小于120个字符的校验函数为无效函数
        if (verifyFunction && verifyFunction.length > 120) {
            let { remoteParams } = this.state, { nodeParams, userParams, actionParams } = this.props, urlParams = util.getUrlParams(), { modalWidth } = button || {};
            let result = safeEval("(" + verifyFunction + ")(nodeParams,formValues)", { nodeParams: Object.assign({}, nodeParams, urlParams, userParams, actionParams, remoteParams), formValues: params })
            Object.assign(params, result.transformParams || {});
            if (result.message) {
                Modal[result.type]({
                    title: result.title,
                    width: modalWidth ? modalWidth : 480,
                    content: <JSXRender jsx={result.message} />,
                    onOk() {
                        if (result.pass) {
                            return executeAction(params, button);
                        }
                        if (result.closeAction) {
                            closeAndRefresh();
                        }
                    },
                    onCancel() { }
                })
            } else {
                this.executeAction(params, button);
            }
        } else {
            this.executeAction(params, button);
        }
    };
    //一个空函数,只用于form表单的验证钩子函数
    preCustomButtonSubmit = (params) => {
        let { action } = this.state;
        if (action.actionType === Action.OUTPUT) {
            this.executeAction(params);
        }
        return params;
    };

    handleAddScene = (values) => {
        let { nodeParams } = this.props, sceneName = "", { action } = this.state, runtimeData = this.runtimeData, self = this;
        Modal.confirm({
            title: '输入场景名称',
            maskClosable: true,
            content: (
                <div>
                    <Input onChange={(e) => sceneName = e.target.value} />
                </div>
            ),
            onOk() {
                if (!sceneName) {
                    message.warn("场景名称不能为空");
                    return Promise.reject();
                }
                let inDefault = (action.scenes || []).filter(ac => ac.title === sceneName).length > 0;
                if (inDefault) {
                    message.warn("场景名与内置场景重复!");
                    return Promise.reject();
                }
                runtimeData = runtimeData || {};
                runtimeData.scenes = runtimeData.scenes || [];
                let hasExist = false, newScene = { title: sceneName, values: values };
                runtimeData.scenes.forEach(sc => {
                    if (sc.title === sceneName) {
                        sc.values = values;
                        hasExist = true;
                    }
                });
                if (!hasExist) {
                    runtimeData.scenes.push(newScene);
                }
                self.setState({ scenes: (action.scenes || []).concat(runtimeData.scenes) });
                let params = { elementId: action.elementId, nodeTypePath: nodeParams.__nodeTypePath__, runtimeData: runtimeData };
                return httpClient.put("gateway/v2/common/productops/frontend/nodeTypePaths/element/config", params);
            },
        });
    };

    handleSceneChanged = (sceneTitle) => {
        let scene = this.state.scenes.filter(sc => sc.title === sceneTitle)[0];
        if (scene.values) {
            if (this.action_form_container) {
                let values = this.action_form_container.getFieldsValue();
                Object.keys(values).forEach(k => values[k] = Array.isArray(values[k]) ? [] : null);
                this.action_form_container.setFieldsValue(Object.assign(values, scene.values))
            }
            this.executeAction(scene.values);
        }
        this.setState({ sceneTitle: sceneTitle });
    };

    handleDeleteScene = (sceneTitle) => {
        let cusScenes = this.runtimeData.scenes, { action } = this.state, { nodeParams } = this.props;
        this.runtimeData.scenes = cusScenes.filter(sc => sc.title !== sceneTitle);
        this.setState({ scenes: (action.scenes || []).concat(this.runtimeData.scenes) });
        let params = { elementId: action.elementId, nodeTypePath: nodeParams.__nodeTypePath__, runtimeData: this.runtimeData };
        return httpClient.put("gateway/v2/common/productops/frontend/nodeTypePaths/element/config", params);

    };

    handleFilterReset = () => {
        let values = this.action_form_container.getFieldsValue(), { dispalyItems } = this.state, newUrlParams = util.getUrlParams(), { history } = this.props;
        //重置要把地址栏上的以隐藏的过滤项的值完全重置
        dispalyItems.forEach(item => {
            if (newUrlParams.hasOwnProperty(item.name)) {
                delete newUrlParams[item.name]
            }
        });
        history && history.push({
            pathname: history.location.pathname,
            search: util.objectToUrlParams(newUrlParams)
        });
        Object.keys(values).forEach(k => values[k] = Array.isArray(values[k]) ? [] : null);
        this.action_form_container.setFieldsValue(values)

    };
    /**
     * 处理新版本操作支持提交前脚本
     * @param params
     */
    handlePreSubmitAction = (params) => {
        if (!params) {
            return false
        }
        let { action, remoteParams } = this.state, { nodeParams, userParams, actionParams } = this.props;
        //合并参数来源一同传递给作业,输入参数及自定义数据源优先级最高
        let allParams = Object.assign({}, nodeParams, userParams, actionParams, remoteParams, params), executeAction = this.executeAction, closeAndRefresh = this.closeAndRefresh;
        //增加操作上定义的提交前函数处理
        let { beforeSubmitHandler } = action;
        if (beforeSubmitHandler && beforeSubmitHandler.length > 150) {
            let result = safeEval("(" + beforeSubmitHandler + ")(nodeParams,formValues,httpClient)", { nodeParams: allParams, formValues: params, httpClient: httpClient });
            // promise类型兼容处理
            if (result instanceof Promise) {
                result.then(proResult => {
                    Object.assign(params, proResult.transformParams || {});
                    if (proResult.message) {
                        Modal[proResult.type]({
                            title: proResult.title,
                            width: proResult.width ? proResult.width : 480,
                            content: <JSXRender jsx={proResult.message} />,
                            onOk() {
                                if (proResult.pass) {
                                    return executeAction(params);
                                }
                                if (proResult.closeAction) {
                                    closeAndRefresh();
                                }
                            },
                            onCancel() { }
                        })
                    } else {
                        this.executeAction(params);
                    }
                })
            } else {
                Object.assign(params, result.transformParams || {});
                if (result.message) {
                    Modal[result.type]({
                        title: result.title,
                        width: result.width ? result.width : 480,
                        content: <JSXRender jsx={result.message} />,
                        onOk() {
                            if (result.pass) {
                                return executeAction(params);
                            }
                            if (result.closeAction) {
                                closeAndRefresh();
                            }
                        },
                        onCancel() { }
                    })
                } else {
                    this.executeAction(params);
                }
            }
        }
    };
    /**
     * 处理表单项值改变后影响其他表单值的Effect函数
     * @param changedValues
     * @param allValues
     */
    handleItemEffect = (changedValues, allValues) => {
        let { dispalyItems } = this.state;
        if (this.action_form_container) {
            let { remoteParams } = this.state, { nodeParams, userParams, actionParams } = this.props;
            let { effectItemMapping } = this.state, allNodeParams = Object.assign({}, nodeParams, userParams, actionParams, remoteParams);
            let changedFields = Object.keys(changedValues);
            changedFields.forEach(field => {
                //表单项存在effect函数进行函数执行
                if (effectItemMapping[field]) {
                    let result = safeEval("(" + effectItemMapping[field] + ")(nodeParams,formValues,httpClient)", { nodeParams: allNodeParams, formValues: allValues, httpClient: httpClient });
                    if (Object.prototype.toString.call(result) === "[object Promise]") {
                        result.then((value) => {
                            if (JSON.stringify(value) === JSON.stringify(this.state.cacheEffectValue)) {
                                return
                            } else {
                                this.action_form_container.setFieldsValue(value);
                                this.setState({ cacheEffectValue: value })
                            }
                        })
                    } else {
                        this.action_form_container.setFieldsValue(result);
                    }

                }
            })
        }
    };
    tabFilterSubmit = (data) => {
        this.setState({
            loading: true
        });
        this.setState({
            loading: false,
        }, () => {
            let { action } = this.state;
            let { dispatch } = this.props, paramData = { ___refresh_timestamp: (new Date()).getTime() }, outputName = action.outputName;
            if (outputName) {
                paramData[outputName] = data;
            } else {
                Object.assign(paramData, data)
            }
            dispatch({ type: 'node/updateParams', paramData: paramData });
        });
    }
    render() {
        let { actionData, mode, displayType, nodeParams, advanced, showScenes, addScene, showSearch, filterConfig, widgetConfig, ...contentProps } = this.props;
        let { action, dockVisible, dispalyItems, loading, feedbackVisible, feedbacks, showHistory, sceneTitle, scenes, beforeOpenWait, effectItemMapping } = this.state;
        if (beforeOpenWait) return null;
        if (!action || !dispalyItems) return <Spin />;
        let execInfo = action.getExecuteInfo();
        let sizeDef = sizeMapping[action.size];
        if (!sizeDef) {
            sizeDef = sizeMapping.default;
        }
        let defaultSize = document.body.clientWidth * sizeDef.percent;
        if (sizeDef.min > defaultSize) {
            defaultSize = sizeDef;
        }
        if (sizeDef.min > document.body.clientWidth) {
            defaultSize = document.body.clientWidth;
        }
        let labelSpan = sizeDef.labelSpan, { formLayout = 'horizontal' } = action;
        if (action.labelSpan) {
            labelSpan = parseInt(action.labelSpan);
        }
        let formItemLayout = {
            labelCol: {
                xs: { span: 24 },
                sm: { span: 24 },
                md: { span: labelSpan },
            },
            wrapperCol: {
                xs: { span: 24 },
                sm: { span: 24 },
                md: { span: 24 - labelSpan },
            },
        };
        let actionButtons = [], actionContent;
        let drawerWrapper = document.getElementsByClassName('ant-drawer-content-wrapper')[0]
        //PAGE_LAYOUT_TYPE_CUSTOM
        // let isCustom = document.getElementsByClassName('PAGE_LAYOUT_TYPE_CUSTOM')[0] ? true : false;
        let computedWidth = drawerWrapper ? drawerWrapper.style.width : 864;
        let computedHeight = window.innerHeight
            || document.documentElement.clientHeight
            || document.body.clientHeight;
        let baseStyle = { display: 'flex', justifyContent: 'center' }, fixedStyle = this.props.pageLayoutType === 'CUSTOM' ? { display: 'flex', justifyContent: 'center', position: 'fixed', borderTop: '1px solid #e8e8e7', top: computedHeight - 150, right: 0, width: computedWidth, height: 50, paddingTop: 14 } : { display: 'flex', justifyContent: 'center', position: 'fixed', borderTop: '1px solid #e8e8e7', bottom: 0, right: 0, width: computedWidth, height: 50, paddingTop: 14 }
        if (showHistory) {

        } else {
            if (displayType !== 'form' && displayType !== 'filter' && displayType !== 'filterBar' && mode !== 'step' && displayType !== 'mixFilterBar' && displayType !== 'tabFilter') {
                actionButtons.push(<Button size="small" key="_a_b_cancel" onClick={this.onClose}>{localeHelper.get('manage.taskplatform.common.cancel', '取消')}</Button>);
            }
            let onFormSubmit = this.executeAction, formVisible = true, { remoteParams } = this.state, { nodeParams, userParams, actionParams } = this.props, urlParams = util.getUrlParams();
            let { beforeSubmitHandler } = action;
            if (beforeSubmitHandler && beforeSubmitHandler.length > 150) {
                onFormSubmit = this.preCustomButtonSubmit;
            }
            //存在自定义的操作按钮定制
            let buttons = action.buttons, visibleExpression = action.visibleExpression, allNodeParams = Object.assign({}, nodeParams, urlParams, userParams, actionParams, remoteParams);
            if (displayType !== 'filter' && displayType !== 'filterBar' && displayType !== 'mixFilterBar' && displayType !== 'tabFilter' && buttons && buttons.length > 0) {
                onFormSubmit = this.preCustomButtonSubmit;
                buttons.forEach((button, index) => {
                    let expression = button.expression, visible = true;
                    if (expression) {
                        visible = safeEval(expression, { nodeParams: allNodeParams });
                    }
                    if (visible) {
                        actionButtons.push(
                            <Button size="small" key={index} style={{ marginLeft: 24 }} loading={loading} type="primary" onClick={() => { this.handleCustomButton(this.preCustomButtonSubmit.validate(), button) }}>{button.name}</Button>
                        );
                    }
                })
            } else {
                if (displayType === 'filter' || displayType === 'filterBar' || displayType === 'mixFilterBar') {
                    if (showSearch !== false && action.showSearch !== false) {
                        actionButtons.push(
                            <Tooltip key="_a_b_search" title={localeHelper.get('Search', '搜索')}>
                                <Button loading={loading} type="primary" icon={<SearchOutlined />} onClick={() => this.executeAction.validate()} />
                            </Tooltip>
                        );
                        if (displayType === 'filterBar') {
                            actionButtons.push(
                                <Tooltip key="_a_b_reset" title={localeHelper.get('manage.flow.reset', '重置')}>
                                    <Button style={{ marginLeft: 8, fontSize: 16 }} icon={<CloseCircleOutlined />} onClick={this.handleFilterReset} />
                                </Tooltip>
                            );
                        }
                    }
                } else {
                    //新的版本
                    if (beforeSubmitHandler && beforeSubmitHandler.length > 150) {
                        if (action.canExecute()) {
                            actionButtons.push(
                                <Button size="small" key="_n_a_b_exec" style={{ marginLeft: 12 }} loading={loading} type="primary"
                                    onClick={() => this.handlePreSubmitAction(this.preCustomButtonSubmit.validate())}>
                                    {
                                        action.submitLabel ?
                                            action.submitLabel
                                            : localeHelper.get('manage.taskplatform.common.execute', '执行')
                                    }
                                </Button>
                            );
                        }
                    } else {
                        if (action.canExecute()) {
                            actionButtons.push(<Button size="small" key="_a_b_exec" style={{ marginLeft: 12 }} loading={loading} type="primary" onClick={() => this.executeAction.validate()}>{action.actionType === Action.OUTPUT ? localeHelper.get('ButtonOK', '确定') : (action.submitLabel ? action.submitLabel : localeHelper.get('manage.taskplatform.common.execute', '执行'))}</Button>);
                        }
                    }
                }
            }
            if (action.actionType === Action.OUTPUT) {
                if (displayType !== 'filter' && displayType !== 'filterBar' && displayType !== 'mixFilterBar' && displayType !== 'tabFilter') {
                    actionButtons.push(<Button size="small" key="_a_b_reset" style={{ marginLeft: 12 }} onClick={this.handleFilterReset}>{localeHelper.get('manage.flow.reset', '重置')}</Button>);
                    if (addScene) actionButtons.push(<Button size="small" key="_a_b_quick" style={{ marginLeft: 12 }} onClick={() => { let values = this.action_form_container.getFieldsValue(); this.handleAddScene(values) }}>存为场景</Button>);
                }
            }
            try {
                if (visibleExpression && visibleExpression.length > 10) {
                    formVisible = safeEval(visibleExpression, { nodeParams: allNodeParams });
                }
            } catch (e) {
                formVisible = true;
                return true;
            }
            if (displayType === 'filter') {
                if (!dispalyItems.length) return null;
                return (
                    <FilterBar
                        items={dispalyItems}
                        formItemLayout={formItemLayout}
                        colCount={action.column ? parseInt(action.column) : false}
                        onSubmit={onFormSubmit}
                        nodeParam={allNodeParams}
                        hintFunction={action.hintFunction}
                        ref={r => this.action_form_container = r}
                        extCol={
                            action.actionType === Action.OUTPUT ? actionButtons : null
                        }
                        actionData={actionData}
                        action={action}
                        scenes={scenes}
                        enterSubmit={action.enterSubmit}
                        autoSubmit={action.autoSubmit || this.props.autoSubmit}
                        single={this.props.single}
                    />
                )
            }
            if (displayType === 'filterBar') {
                if (!dispalyItems.length) return null;
                return (
                    <FilterForm
                        items={dispalyItems}
                        formItemLayout={formItemLayout}
                        colCount={action.column ? parseInt(action.column) : false}
                        onSubmit={onFormSubmit}
                        nodeParam={allNodeParams}
                        hintFunction={action.hintFunction}
                        ref={r => this.action_form_container = r}
                        extCol={
                            action.actionType === Action.OUTPUT ? actionButtons : null
                        }
                        handleDeleteScene={this.handleDeleteScene}
                        action={action}
                        scenes={scenes}
                        actionData={actionData}
                        enterSubmit={action.enterSubmit}
                        autoSubmit={action.autoSubmit}
                    />
                )
            }
            if (displayType === 'mixFilterBar') {
                if (!dispalyItems.length) return null;
                return (
                    <MixFilterBar
                        items={dispalyItems}
                        formItemLayout={formItemLayout}
                        colCount={action.column ? parseInt(action.column) : false}
                        onSubmit={onFormSubmit}
                        nodeParam={allNodeParams}
                        hintFunction={action.hintFunction}
                        ref={r => this.action_form_container = r}
                        extCol={
                            action.actionType === Action.OUTPUT ? actionButtons : null
                        }
                        handleDeleteScene={this.handleDeleteScene}
                        actionData={actionData}
                        action={action}
                        scenes={scenes}
                        enterSubmit={action.enterSubmit}
                        autoSubmit={action.autoSubmit}
                        filterConfig={filterConfig}
                    />
                )
            }
            if (displayType === 'tabFilter') {
                if (!dispalyItems.length) return null;
                return (
                    <TabFilter
                        items={dispalyItems}
                        onSubmit={this.tabFilterSubmit}
                        nodeParam={allNodeParams}
                        actionData={actionData}
                        widgetData={actionParams}
                        action={action}
                        scenes={scenes} />
                )
            }
            //增加Action在分步Action的动作处理
            if (mode === 'step') {
                let { isFirst, isLast, onNext, onPrev } = this.props;
                let isMiddle = !isFirst && !isLast;
                //第一个在最后动态增加下一步button,中间步骤增加两端增加上一步下一步,最后一步增加上一步
                if (isFirst || isMiddle) {
                    actionButtons.push(<Button style={{ marginLeft: 12 }} size="small" type="primary" key="_a_b_next"
                        onClick={() => {
                            this.action_form_container.validateFieldsAndScroll((err, values) => { if (!err) { onNext(values) } })
                        }
                        }>{localeHelper.get('next', '下一步')}</Button>)
                }
                if (isLast || isMiddle) {
                    actionButtons.unshift(<Button style={{ marginRight: 12 }} size="small" key="_a_b_prev" onClick={() => { onPrev() }}>{localeHelper.get('previous', '上一步')}</Button>)
                }
            }
            actionContent = (
                <div>
                    {
                        feedbackVisible ? <Alert
                            description={<Feedback feedbacks={feedbacks} />}
                            closable
                            showIcon
                            banner={true}
                            type="info"
                            onClose={this.onFeedbackClose}
                        /> : null
                    }
                    {
                        formVisible ?
                            <div>
                                <Spin spinning={loading}>
                                    <SimpleForm
                                        items={dispalyItems}
                                        formItemLayout={formItemLayout}
                                        colCount={action.column ? parseInt(action.column) : false}
                                        onSubmit={onFormSubmit}
                                        nodeParam={allNodeParams}
                                        hintFunction={action.hintFunction}
                                        ref={r => this.action_form_container = r}
                                        extCol={
                                            action.actionType === Action.OUTPUT ? actionButtons : null
                                        }
                                        advanced={advanced}
                                        enterSubmit={action.enterSubmit}
                                        autoSubmit={action.autoSubmit}
                                        formLayout={formLayout}
                                        onValuesChange={effectItemMapping ? this.handleItemEffect : false}
                                    />
                                    {
                                        action.actionType === Action.OUTPUT ? null :
                                            <div className='globalBackground' style={action.isDrawer ? fixedStyle : baseStyle}>
                                                {action.canExecute() || mode === 'step' ? actionButtons : null}
                                            </div>
                                    }
                                    {
                                        (scenes.length === 0 || showScenes === false) ? null :
                                            <div style={{ marginTop: 12 }}>
                                                <Radio.Group size="small" defaultValue={sceneTitle} buttonStyle="solid" onChange={(e) => this.handleSceneChanged(e.target.value)}>
                                                    {
                                                        scenes.map((scene, index) => {
                                                            return (
                                                                <Radio.Button style={{ marginLeft: index === 0 ? 0 : 8 }} key={index} value={scene.title}>
                                                                    {scene.title}
                                                                    {
                                                                        sceneTitle === scene.title && (action.scenes || []).filter(ac => ac.title === scene.title).length === 0 ?
                                                                            <a onClick={() => this.handleDeleteScene(scene.title)}><DeleteOutlined style={{ color: 'red', marginRight: -12 }} /></a>
                                                                            : null
                                                                    }
                                                                </Radio.Button>
                                                            );
                                                        })
                                                    }
                                                </Radio.Group>
                                            </div>
                                    }
                                </Spin>
                            </div>
                            : null
                    }
                </div>
            );
        }
        if (displayType === 'panel') {
            return <Card size="small" bordered={false}>{actionContent}</Card>
        }
        if (displayType === 'form') {
            return <Card size="small">{actionContent}</Card>
        }
        if (mode === 'step') {
            return actionContent;
        }
        if (action.displayType === 'modal') {
            return (
                <div style={{ marginRight: 16 }}>
                    {mode !== 'custom' ? <a disabled={!execInfo.executeAble} onClick={this.previewAction}><LegacyIcon type={action.icon || 'tool'} style={{ marginRight: 8 }} />{action.label}</a> : null}
                    <Modal
                        title={action.label}
                        visible={dockVisible}
                        footer={null}
                        width={defaultSize}
                        maskClosable={true}
                        destroyOnClose={true}
                        onCancel={this.onClose}
                    >
                        {actionContent}
                    </Modal>
                </div>
            );
        }
        return (
            <div style={{ marginRight: 16 }}>
                {mode !== 'custom' ? <a disabled={!execInfo.executeAble} onClick={this.previewAction}><LegacyIcon type={action.icon || 'tool'} style={{ marginRight: 8 }} />{action.label}</a> : null}
                <Drawer
                    placement="right"
                    title={action.label}
                    width={showHistory ? '80%' : defaultSize}
                    destroyOnClose={true}
                    onClose={this.onClose}
                    visible={dockVisible}
                >
                    {actionContent}
                </Drawer>
            </div>
        );
    }
}

const Feedback = ({ feedbacks }) => {
    let feedbackItems = [];
    feedbacks.forEach((feedbackData, index) => {
        let feedback = null;
        if (feedbackData.type === 'link') {
            feedback = <LinkFeedback key={index} data={feedbackData} />
        } else if (feedbackData.type === 'kvlist') {
            feedback = <KVFeedback key={index} data={feedbackData} />
        }

        if (feedback) feedbackItems.push(feedback);
    });
    return <div>{feedbackItems}</div>;
};

const LinkFeedback = ({ data }) => {

    return null;
};

const KVFeedback = ({ data }) => {
    let kvData = {}, fdData = data.data;
    Object.keys(fdData).forEach(key => {
        let displayValue = fdData[key];
        if (typeof displayValue === 'string') {
            kvData[key] = {
                value: displayValue
            }
        } else {
            kvData[key] = displayValue;
        }
    });
    return (
        <div>
            <h3>{data.title ? data.title : ''}</h3>
        </div>
    );
};

export default class ActionAdaptor extends React.Component {
    render() {
        let actionType = this.props.actionData.config.actionType;
        if (actionType === 'STEPS') {
            return <OamStepAction {...this.props} />
        }
        return <OamAction {...this.props} />
    }
}
