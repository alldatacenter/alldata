/**
 * Created by caoshuaibiao on 2020/12/3.
 * 页面的基础组成单元
 */
import uuidv4 from 'uuid/v4';
import _ from "lodash";
import BaseModel from './BaseModel';
import Constants from './Constants';
import * as util from '../../../utils/utils';
import safeEval from '../../../utils/SafeEval';
import { constants } from 'fs';

export default class WidgetModel extends BaseModel {

    static CREATE_DEFAULT_INSTANCE() {
        return new WidgetModel({
            type: "",
            name: "",
            id: "",
            info: {},
            config: {
                title: "New widget",
                //datasource:null,
                //toolbar:null,
                gridPos: { x: 0, y: 0, h: 36, w: 50, i: 'init' },
                //description:"",
                //links:[],
                dependParams: [],
                style: null,
                outputs: [],
                refreshInterval: null,
                visibleExp: null,
                //repeat:false,
                uniqueKey: uuidv4(),
            }
        })
    };

    constructor(modelJson) {
        super(modelJson);
        Object.assign(this, modelJson.config);
    }

    /**
     * 由meta信息初始化模型
     * @param widgetMeta
     */
    initFromMeta(widgetMeta) {
        let { type, configSchema, name, id, info } = widgetMeta;
        this.type = type;
        this.name = name;
        this.id = id;
        this.info = info
        Object.assign(this.config || {}, configSchema.defaults.config);
        Object.assign(this, configSchema.defaults.config);
    }

    /**
     * 关联页面
     * @param containerModel
     */
    setContainerModel(containerModel) {
        this.containerModel = containerModel;
    }

    /**
     * 所在容器模型
     * @param containerModel
     */
    getContainerModel() {
        return this.containerModel;
    }

    /**
     * 获取基础配置表单定义
     * @param widgetMeta
     */
    getCustomItems(widgetMeta) {
        let baseItems = [
            //{type:1,name:'name',initValue:this.name,required:true,label:"名称",tooltip:"组件在页面中的唯一标识,用于组件对外的交互,建议英文字母组成"},
            { type: 1, name: 'title', initValue: this.title, required: false, label: "标题" }
        ];
        let configSchema = widgetMeta.configSchema.schema, defaults = widgetMeta.configSchema.defaults;
        //用旧版本的组件方式暂时只支持json配置
        baseItems.push({
            type: 86, name: 'userConfig', initValue: { config: Object.assign({}, defaults, this.config || {}) }, required: false, label: "配置"
        });
        /*
        baseItems.push({
            type:79,name:'userConfig',initValue:{config:Object.assign({},defaults,this.config||{})},required:false,label:"配置",
            defModel:{
                schema:{
                    type:"object",
                    properties:{
                        config:JSON.parse(JSON.stringify(configSchema))
                    }
                }
            }
        });
        */
        return baseItems;
    }

    /**
     * 更新挂件配置
     * @param config
     */
    updateConfig(config) {
        this.config = config;
        Object.assign(this, config);
    }

    /**
     * 是否已经具体定义
     */
    isDefine() {
        return !!this.type;
    }

    /**
     * 更新widget布局位置
     * @param pos
     */
    updatePos(pos) {
        Object.assign(this.config.gridPos, pos);
        Object.assign(this.gridPos, pos);
    }

    /**
     * 序列化为jsonData
     */
    toJSON() {
        let jsonData = super.toJSON();
        //let {type,name,title,datasource,toolbar,gridPos,description,links,dependParams,style,outputs,refreshInterval,visibleExp,repeat,uniqueKey}=this;
        let { type, dataSourceMeta } = this;
        return {
            ...jsonData,
            type: type,
            //name:name,
            //id:uniqueKey,
            config: {
                ...this.config,
                dataSourceMeta: dataSourceMeta && dataSourceMeta.type && { ...dataSourceMeta }
            }
        }
    }

    /**
     * 是否为过滤器
     * @returns {boolean}
     */
    isFilter() {
        return Constants.FILTERS.includes(this.type);
    }
    isTabFilter() {
        return this.type === 'FILTER_TAB';
    }
    /**
     * 是否为操作
     * @returns {boolean}
     */
    isAction() {
        return Constants.ACTIONS.includes(this.type);
    }

    /**
     * 设置关联的节点模型
     * @param nodeModel
     */
    setNodeModel(nodeModel) {
        this.nodeModel = nodeModel;
    }

    /**
     * 设置关联的节点模型
     * @param nodeModel
     */
    getNodeModel() {
        return this.nodeModel || this.containerModel.nodeModel;
    }

    /**
     * 是否为分步
     * @returns {boolean}
     */
    isStepAction() {
        return Constants.STEP_ACTION === this.type;
    }

    /**
     * 是否为区块
     * @returns {boolean}
     */
    isBlock() {
        return Constants.BLOCK === this.type;
    }

    /**
     * 获取运行时挂件配置,挂件已经进行变量替换后的配置
     * @param paramsSet 运行时参数域
     */
    getRuntimeConfig(paramsSet) {
        return util.renderTemplateJsonObject(this.config, paramsSet, false);
    }

    /**
     * 判断是否需要重新加载渲染,主要逻辑是判断数据源中的变量是否改变,改变刷新,不改变不刷新
     * @param nodeParams 当前节点参数域
     * @param nextNodeParams 下一个参数域
     */
    needReload(nodeParams, nextNodeParams) {
        if (this.dataSourceMeta) {
            //变量位置不定,先把数据源定义字符串化然后进行提取变量,再进行取值对比
            let dsMetaString = JSON.stringify(this.dataSourceMeta);
            let matchVars = dsMetaString.match(/\$\(([^)]*)\)/g) || [];
            let reload = false;
            matchVars.forEach(varStr => {
                let paramKey = varStr.replace("$(", "").replace(")", "");
                let currValue = _.get(nodeParams, paramKey), nextValue = _.get(nextNodeParams, paramKey);
                if (!_.isEqual(currValue, nextValue)) {
                    reload = true;
                }
            });
            return reload;
        }
        return false;
    }

    /**
     * 是否所依赖的参数已经全部存在
     * @param nodeParams
     */
    isReady(nodeParams) {
        let { dependParams } = this;
        if (dependParams && dependParams.length > 0) {
            for (let i = 0; i < dependParams.length; i++) {
                let dependValue = _.get(nodeParams, dependParams[i], "___DEPEND_NOT_EXIST___");
                if (dependValue === '___DEPEND_NOT_EXIST___') {
                    return false
                }
            }
        }
        return true;
    }

    /**
     * 是否启用
     * @param nodeParams
     */
    isVisible(nodeParams) {
        let { visibleExp, display } = this;
        //兼容老的模型
        if (display === false || display === "false" || display === "False") {
            return false;
        } else if (display && display !== true) {
            return safeEval(display, { nodeParams: nodeParams });
        }
        if (visibleExp) {
            return safeEval(visibleExp, { nodeParams: nodeParams });
        }
        return true;
    }

    /**
     * 是否需要统一包装
     */
    getWrapperType() {
        let { type, hasWrapper = Constants.CARD_WRAPPER_DEFAULT, wrapper = true } = this;
        if (Constants.FILTERS.includes(type) || Constants.ACTIONS.includes(type)) {
            wrapper = Constants.CARD_WRAPPER_NONE;
        }
        if (Constants.BLOCK === type && hasWrapper === true) {
            wrapper = Constants.CARD_WRAPPER_NONE;
        }
        if (hasWrapper === Constants.CARD_WRAPPER_ADVANCED) {
            return Constants.CARD_WRAPPER_ADVANCED
        }
        if (hasWrapper === Constants.CARD_WRAPPER_DEFAULT) {
            return wrapper
        } else {
            return Constants.CARD_WRAPPER_NONE
        }
    }
    /**
     * 是否存在toolbar
     */
    hasToolbar() {
        let { toolbar = {} } = this;
        let { filter, type, actionList = [], docList = [], customRender } = toolbar;
        return filter || actionList.length > 0 || docList.length > 0 || customRender;
    }
    // 是否需要



}