/**
 * Created by caoshuaibiao on 2019/1/7.
 * 运维挂载组件,其中可包一个显示Widget及0到1个OamActionBar
 */
import React from 'react';
import { Spin, Button, Card, Modal, Tooltip, List, Drawer } from "antd";
import WidgetFactory from './legacy/widgets/WidgetFactory'
import oamTreeService from '../services/oamTreeService';
import * as util from '../../utils/utils';
import { connect } from 'dva';
import _ from 'lodash';
import safeEval from '../../utils/SafeEval';
import ErrorBoundary from "../../components/ErrorBoundary";
import httpClient from '../../utils/httpClient';
import Constants from './model/Constants';

@connect(({ global, node }) => ({
    currentProduct: global.currentProduct,
    currentUser: global.currentUser,
    nodeParams: node.nodeParams
}))
export default class OamWidget extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            mode: false,
        };
        this.cardWrapper = false;
        this.refreshTimerId = 0;
    }

    componentWillMount() {
        let { widget, nodeId, parameters, nodeParams } = this.props;
        if (widget.elementId) {
            oamTreeService.getWidgetMeta(nodeId || nodeParams.__nodeId__, widget.elementId, parameters).then(widgetDef => {
                this.setState({
                    modeTemplate: widgetDef,
                    mode: this.getMode(widgetDef)
                });
            });
        } else {
            this.setState({
                modeTemplate: widget,
                mode: this.getMode(widget)
            });
        }
    }

    componentWillReceiveProps(nextProps) {
        if (!_.isEqual(nextProps.nodeParams, this.props.nodeParams)) {
            if (this.state.modeTemplate) {
                let mode = this.getMode(this.state.modeTemplate, nextProps);
                this.setState({
                    mode: mode
                });
            }
        }
    }

    startAutoRefresh = (autoRefreshInterval) => {
        clearInterval(this.refreshTimerId);
        let { handleParamsChanged } = this.props;
        let timeChanged = () => {
            this.refreshTimerId = setTimeout(timeChanged, autoRefreshInterval);
            handleParamsChanged && handleParamsChanged({ ___refresh_timestamp: (new Date()).getTime() });
        };
        timeChanged();
    };

    componentWillUnmount() {
        if (this.refreshTimerId) {
            clearInterval(this.refreshTimerId);
        }
    }

    loadData = (widgetDataSource, mode) => {
        if (widgetDataSource.type && widgetDataSource.type === 'API' && Constants.EXCLUDE_COMP_TYPES.includes(mode.type)) {
            return false
        }
        let { nodeParams = {}, nodeData = {} } = this.props, urlParams = util.getUrlParams();
        let paramsSet = Object.assign({}, nodeParams, urlParams), { dependParams } = mode.config;
        if (dependParams && dependParams.length > 0) {
            for (let i = 0; i < dependParams.length; i++) {
                if (!paramsSet.hasOwnProperty(dependParams[i])) {
                    return;
                }
            }
        }
        let { api, params = {}, method = 'get' } = widgetDataSource, req = null;
        if (api && api.includes('$(')) {
            api = util.renderTemplateString(api, { ...nodeParams, ...nodeData });
        }
        if (method === 'get' || method === 'GET') {
            req = httpClient.get(api, { params: params });
        } else if (method === 'post' || method === 'POST') {
            req = httpClient.post(api, params);
        }
        if (req) {
            req.then(data => {
                this.setState({
                    widgetData: data
                });
            })
        }

    };

    getMode = (modeTemplate, props) => {
        //复合组件的情况下不进行统一变量拦截替换,因为在节点已经存在子组件中变量时,会改变子组件原始定义的问题
        const excludeType = ['CHART_CASCADER_AGTABLE'];
        if (excludeType.includes(modeTemplate.type)) {
            return modeTemplate;
        }
        if (!props) props = this.props;
        let { nodeParams, widgetParams = {} } = props, urlParams = util.getUrlParams();
        let paramsSet = Object.assign({}, nodeParams, urlParams, widgetParams);
        let keys = Object.keys(paramsSet);
        /*let keys=Object.keys(paramsSet);
        let modeString=JSON.stringify(modeTemplate);
        keys.forEach(key=>{
            let varTmp="\\$\\("+key+"\\)";
            let rex=new RegExp(varTmp,'g');
            modeString=modeString.replace(rex, paramsSet[key]);
        });

        //console.log("modeTemplate----->",modeTemplate);
        let mode=JSON.parse(modeString);
        */
        //console.log("mode----->",mode);
        //替换上面的挂件变量渲染拦截逻辑,变量值存在时进行渲染,不存在时放过,此渲染代替上面全量替换会解决值中有特殊字符无法反序列化为json的问题
        //存在 components的嵌套组件时,不进行嵌套组件内的替换放置参数替换过早污染问题
        let cloneTemplate = JSON.parse(JSON.stringify(modeTemplate)), children;
        //是复合组件,children无需在此进行变量替换
        if (cloneTemplate.children) {
            children = cloneTemplate.children;
            delete cloneTemplate.children;
        }
        let mode = JSON.parse(JSON.stringify(cloneTemplate), function (key, value) {
            if (typeof value === 'string' && value.includes("$")) {
                if (cloneTemplate.config.replaceType === "common") {
                    return util.renderTemplateString(value, paramsSet)
                }
                let paramKey = value.replace("$(", "").replace(")", "");
                let renderValue = _.get(paramsSet, paramKey);
                //数组类型的值忽略,放置到模板值渲染时进行渲染
                if ((typeof renderValue) === "object" && !Array.isArray(renderValue)) {
                    return renderValue;
                }

                //提取变量占位符,存在的变量进行替换不存在的保持原样
                let replaceVars = value.match(/\$\(([^)]*)\)/g) || [];
                replaceVars.forEach(varStr => {
                    let paramKey = varStr.replace("$(", "").replace(")", "");
                    let renderValue = _.get(paramsSet, paramKey);
                    //临时把"$(row."作为运行时内置替换变量,进行规避，防止节点中出现table等其他含有的运行时变量进行了脏替换。
                    if ((typeof renderValue) !== "undefined" && !varStr.includes("$(row.")) {
                        let varTmp = "\\$\\(" + paramKey + "\\)";
                        let rex = new RegExp(varTmp, 'g');
                        if (renderValue === null) {
                            renderValue = "";
                        }
                        value = value.replace(rex, renderValue);
                    }
                });
                return value;

                /*keys.forEach(paramKey=>{
                    let varTmp="\\$\\("+paramKey+"\\)";
                    let rex=new RegExp(varTmp,'g');
                    let paramValue=paramsSet[paramKey];
                    if(paramValue===null || typeof (paramValue)==="undefined"){
                        paramValue="";
                    }
                    if(typeof paramsSet[paramKey] !=='object') value=value.replace(rex, paramValue);
                });
                return value*/
            }
            return value;
        });
        if (children) mode.children = children;
        mode.__template = modeTemplate;
        mode.config = mode.config || {};
        let cardWrapper = mode.config.cardWrapper;
        if (cardWrapper) {
            this.cardWrapper = cardWrapper;
            //删除包装避免出现循环
            delete mode.config.cardWrapper;
            //存在card包装器配置直接根据定义生成复合card模型
            if (mode.elementId) {
                delete mode.elementId;
            }
            mode = {
                "type": "COMPOSITION_CARD",
                "config": this.cardWrapper,
                "children": [{ "components": [mode] }]
            };
        }
        if (mode.config.widgetDataSource || mode.config.dataSourceMeta) {
            let dataSouce = mode.config.widgetDataSource || mode.config.dataSourceMeta
            this.loadData(dataSouce, mode)
        }
        if (mode.config.autoRefreshInterval && this.refreshTimerId === 0) {
            this.startAutoRefresh(mode.config.autoRefreshInterval);
        }
        return mode;
    };

    openFlowInstanceDrawer = (id) => {
        this.setState({
            instanceId: id,
            visible: true,
        });
    };
    onClose = () => {
        this.setState({
            visible: false,
        });
    };

    render() {
        let { widget, widgetParams, ...contentProps } = this.props, { mode, widgetData } = this.state;
        if (!mode || (mode.config && mode.config.widgetDataSource && !widgetData)) {
            return <Spin />
        }
        let { dependParams } = mode.config;
        let { nodeParams } = this.props;
        let paramsSet = Object.assign({}, nodeParams, util.getUrlParams(), widgetParams);
        if (dependParams && dependParams.length > 0) {
            for (let i = 0; i < dependParams.length; i++) {
                let dependValue = _.get(paramsSet, dependParams[i], "___DEPEND_NOT_EXIST___");
                if (dependValue === '___DEPEND_NOT_EXIST___') {
                    return <div />
                }
            }
        }
        let { align, display, fill } = mode.config || {}, dynamicStype = mode.config.cStyle || {};
        if (display === false || display === "false" || display === "False") {
            return <div />;
        } else if (display && display !== true) {
            let dynaDisplay = safeEval(display, { nodeParams: paramsSet });
            if (!dynaDisplay) {
                return <div />;
            }
        }
        if (align) {
            dynamicStype.display = 'flex';
            dynamicStype.justifyContent = align;
        }
        //组件统一增加默认值设定
        let widgetDefaultParams = Object.assign({}, _.get(contentProps, "tabMate.config.defaultParams"), _.get(mode, "config.defaultParams"));
        //增加流程组件回调
        contentProps.openFlowInstanceDrawer = this.openFlowInstanceDrawer;
        return (
            <div style={dynamicStype} className={dynamicStype.fill || fill ? 'globalBackground abm-widget-container' : 'abm-widget-container'}>
                <ErrorBoundary>
                    {
                        WidgetFactory.createWidget(mode, {
                            ...contentProps,
                            widgetParams: widgetParams,
                            widgetDefaultParams: widgetDefaultParams,
                            widgetData: widgetData ? (_.isString(widgetData) ? widgetData : Object.assign({}, widgetParams, widgetData)) : (contentProps.widgetData || false),
                            auths: mode.defAuths || []
                        })
                    }
                </ErrorBoundary>
            </div>
        )
    }
}