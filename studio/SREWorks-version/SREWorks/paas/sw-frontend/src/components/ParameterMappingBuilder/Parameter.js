/**
 * Created by caoshuaibiao on 2019/1/22.
 * 参数模型
 */
import FormElementType from '../FormBuilder/FormElementType';
import DataSource from '../../core/framework/model/DataSource';
import moment from 'moment';
import localeHelper from '../../utils/localeHelper';
import * as util from '../../utils/utils';
import httpClient from '../../utils/httpClient';
import _ from 'lodash';

export default class Parameter {

    constructor(paramData, runtimeContext) {
        //参数名称
        this.name = paramData.name || '';
        //参数标题
        this.label = paramData.label || '';
        //默认值
        this.defaultValue = paramData.defaultValue || [];
        //参数的值
        this.value = paramData.value || paramData.defaultValue || [];
        //有效值(选择性参数时候存在)
        this.validValue = paramData.validValue || [];
        //参数展示类型类型同 FormElementType中的元素显示模型定义
        this.type = paramData.type || FormElementType.INPUT;
        //是否为动态获取,为false时表示是页面直接人工录入,否则为参数获取的url
        this.dynamic = paramData.dynamic || false;
        //是否必填
        this.required = paramData.required || 2;
        //验证类型 数字、整形、ip、日期等等,暂时未实现
        this.validateType = paramData.validateType || 'string';
        //是否启用
        this.visible = paramData.visible || 2;
        // 是否可见
        this.isHidden = paramData.isHidden || 2;
        //是否只读
        this.readonly = paramData.readonly || 1;
        //参数提示
        this.toolTip = paramData.toolTip || '';
        //参数模型补充,老数据类型是string,新数据类型为json对象,因此需要做个兼容
        this.defModel = ((typeof (paramData.defModel) === 'string') ? JSON.parse(paramData.defModel) : paramData.defModel) || {};
        //参数次序
        this.order = paramData.order || 0;
        //参数初始值和可选值URL定义
        this.initApi = paramData.initApi || '';
        //公共配置属性在此解析
        if (this.defModel.hasOwnProperty("defaultValue")) {
            this.value = this.defModel.defaultValue;
        }
        if (this.defModel.hasOwnProperty("layout")) {
            this.layout = this.defModel.layout;
        }
        //校验正则表达式
        this.validateReg = paramData.validateReg || '';
        //可见表达式
        this.visibleExp = paramData.visibleExp || '';
        //新的数据源定义
        this.dataSourceMeta = paramData.dataSourceMeta;
        //影响函数
        this.effectHandler = paramData.effectHandler || this.defModel.effectHandler;
        //运行时上下文,当前存在值时为NodeParams
        this.runtimeContext = runtimeContext;
    }

    /**
     * 获取参数元数据定义显示模型
     */
    getMetaParamDef() {
        let paramMetaDef = [];
        paramMetaDef.push({ type: FormElementType.INPUT, name: 'name', initValue: this.name, required: true, label: localeHelper.get('paramMap.paramName', "参数名称"), tooltip: localeHelper.get('paramMap.paramNameRule', "参数名,需为英文") });
        paramMetaDef.push({ type: FormElementType.INPUT, name: 'label', initValue: this.label, required: false, label: localeHelper.get('paramMap.paramTitle', "参数标题"), tooltip: localeHelper.get('paramMap.paramShowName', "参数在表单页面显示的标签名") });
        paramMetaDef.push({ type: FormElementType.SELECT, name: 'type', initValue: this.type, required: true, label: localeHelper.get('paramMap.inputType', "输入方式"), optionValues: FormElementType.SIMPLE_TYPE_LABEL_MAPPING_OPTIONS, tooltip: localeHelper.get('paramMap.enterShowType', "用户录入参数的显示方式") });
        paramMetaDef.push({ type: FormElementType.RADIO, name: 'required', initValue: this.required, required: false, label: localeHelper.get('paramMap.isnotMustFill', "是否必填"), optionValues: [{ value: 1, label: localeHelper.get('no', '否') }, { value: 2, label: localeHelper.get('is', '是') }] });
        paramMetaDef.push({ type: FormElementType.RADIO, name: 'visible', initValue: this.visible, required: false, label: localeHelper.get('paramMap.isnotView', "是否启用"), tooltip: localeHelper.get('paramMap.billIsnotView', "用户提单页面是否启用"), optionValues: [{ value: 1, label: localeHelper.get('no', '否') }, { value: 2, label: localeHelper.get('is', '是') }] });
        paramMetaDef.push({ type: FormElementType.RADIO, name: 'readonly', initValue: this.readonly, required: false, label: localeHelper.get('paramMap.isnotOnlyRead', "是否只读"), tooltip: localeHelper.get('paramMap.billIsnotEdit', "用户提单页面是不可修改"), optionValues: [{ value: 1, label: localeHelper.get('no', '否') }, { value: 2, label: localeHelper.get('is', '是') }] });
        paramMetaDef.push({ type: FormElementType.RADIO, name: 'validateType', initValue: this.validateType, required: true, label: localeHelper.get('paramMap.verificationType', "校验类型"), optionValues: [{ value: 'string', label: 'string' }, { value: 'number', label: 'number' }] });
        if (this.type === FormElementType.MACHINE_TARGET) {
        } else {
            paramMetaDef.push({ type: FormElementType.SELECT_TAG, name: 'defaultValue', initValue: this.defaultValue.length > 0 ? this.defaultValue : this.value, required: false, label: localeHelper.get('paramMap.defaultValue', "默认值"), inputTip: localeHelper.get('paramMap.inputDefaultValue', "直接输入默认值") });
        }
        paramMetaDef.push({ type: FormElementType.SELECT_TAG, name: 'validValue', initValue: this.validValue, required: false, label: localeHelper.get('paramMap.effectiveValue', "有效值"), inputTip: localeHelper.get('paramMap.selectMsg', '定义单选/多选形式的可选值,可直接输入选项') });
        paramMetaDef.push({ type: FormElementType.INPUT, name: 'toolTip', initValue: this.toolTip, required: false, label: localeHelper.get('paramMap.paramHint', "参数提示") });
        paramMetaDef.push({ type: FormElementType.DISABLED_INPUT, name: 'order', initValue: this.order, required: false, label: localeHelper.get('paramMap.paramSerial', "参数序号"), validateType: "number", inputTip: localeHelper.get('paramMap.paramSerialMsg', "数字类型,定义此参数在表单中的顺序位置,不存在时按照tab页顺序") });
        paramMetaDef.push({ type: FormElementType.INPUT, name: 'validateReg', initValue: this.validateReg, required: false, label: "校验正则", inputTip: '合法值的正则表达式' });
        paramMetaDef.push({ type: FormElementType.INPUT, name: 'initApi', initValue: this.initApi, required: false, label: "参数接口", inputTip: '定义参数的初始值和下拉值的接口地址,返回格式{initValue:xxx,options:[...]}' });
        paramMetaDef.push({ type: FormElementType.INPUT, name: 'visibleExp', initValue: this.visibleExp, required: false, label: "可见表达式", inputTip: "直接用formValues.参数名来引用表单其他参数值" });
        paramMetaDef.push({ type: 86, name: 'defModel', initValue: this.defModel, required: false, label: "参数JSON配置", height: 200, showDiff: false });
        paramMetaDef.push({ type: FormElementType.RADIO, name: 'isHidden', initValue: this.isHidden, required: false, label: localeHelper.get('paramMap.isnotView', "是否可见"), tooltip: localeHelper.get('paramMap.billIsnotView', "用户提单页面是否可见"), optionValues: [{ value: 1, label: localeHelper.get('no', '否') }, { value: 2, label: localeHelper.get('is', '是') }] });
        return paramMetaDef;

    }
    /**
     * 更新参数类型
     */
    changeType(type) {
        if (type === FormElementType.MACHINE_TARGET) {
        }
        this.type = type
    }

    /**
     * 获取直接参数实例值定义
     */
    getInstanceParamDef() {
        let paramInstanceDef = [];
        paramInstanceDef.push({ type: FormElementType.INPUT, name: 'name', initValue: this.name, required: true, label: localeHelper.get('paramMap.paramName', "参数名称"), tooltip: localeHelper.get('paramMap.paramNameRule', "参数名,需为英文") });
        paramInstanceDef.push({ type: FormElementType.INPUT, name: 'label', initValue: this.label, required: true, label: localeHelper.get('paramMap.paramTitle', "参数标题"), tooltip: localeHelper.get('paramMap.paramShowName', "参数在表单页面显示的标签名") });
        paramInstanceDef.push({
            type: FormElementType.SELECT, name: 'type', initValue: this.type, required: true, label: localeHelper.get('paramMap.inputType', "参数类型"),
            optionValues: [{ value: 1, label: "文本" }, { value: FormElementType.MACHINE_TARGET, label: "机器选择" }],
            tooltip: localeHelper.get('paramMap.enterShowType', "用户录入参数的显示方式")
        });
        if (this.type === FormElementType.MACHINE_TARGET) {
        } else {
            paramInstanceDef.push({ type: FormElementType.TEXTAREA, name: 'defaultValue', initValue: this.defaultValue.length > 0 ? this.defaultValue : this.value, required: true, label: localeHelper.get('paramMap.defaultValue', "参数值"), inputTip: localeHelper.get('paramMap.inputDefaultValue', "直接输入参数值") });
        }
        return paramInstanceDef;
    }

    /**
     * 更新参数元数据
     */
    updateMetaData(defValues) {
        Object.assign(this, defValues);
        //更新参数定义的时候需要把默认值更新至value中
        this.value = this.defaultValue;
    }
    getSyncParamWidgetDef() {
        //首先把确定的元素值进行定义,后续再根据类型的不同增加不同的模型值,只处理普通输入类和选择类初始值
        let widgetDef = {
            type: this.type, name: this.name, required: this.required > 1, label: this.label, tooltip: this.toolTip, defModel: this.defModel, api: this.initApi,
            validateType: this.validateType, order: this.order, readonly: this.readonly, layout: this.layout, validateReg: this.validateReg, visibleExp: this.visibleExp,
            effectHandler: this.effectHandler, isHidden: this.isHidden
        };
        let initValue = "";
        if (Array.isArray(this.value)) {
            initValue = this.value.length > 0 ? this.value : this.defaultValue;
        } else {
            initValue = (this.value + "").length > 0 ? this.value : this.defaultValue;
        }
        //this.type值一定为FormElementType.SIMPLE_TYPE_LABEL_MAPPING_OPTIONS中的一个,需要扩充类型请直接扩充FormElementType.SIMPLE_TYPE_LABEL_MAPPING_OPTIONS
        if (this.type === FormElementType.INPUT || this.type === FormElementType.TEXTAREA) {//普通输入类
            widgetDef.initValue = Array.isArray(initValue) ? initValue.join(",") : initValue;
        } else if (this.type === FormElementType.GRID_CHECKBOX || this.type === FormElementType.SELECT || this.type === FormElementType.MULTI_SELECT ||
            this.type === FormElementType.RADIO || this.type === FormElementType.CHECKBOX || this.type === FormElementType.SELECT_TAG
            || this.type === 14 || this.type === 17 || this.type === 87) {//选择类
            //存在自定义的options
            let options = this.options, cusDefValue = null;
            if (this.defModel.options) {
                options = this.defModel.options;
            }
            if (this.defModel.hasOwnProperty("defaultValue")) {
                cusDefValue = this.defModel.defaultValue;
            }
            if (cusDefValue !== null) {
                initValue = cusDefValue;
            }
            if (!options) {
                widgetDef.optionValues = this.validValue.map(vv => {
                    return { value: vv, label: vv }
                });
            } else {
                widgetDef.optionValues = options;
            }
            //type 14 radio.button只有单选模式
            if (this.type === FormElementType.SELECT || this.type === FormElementType.RADIO || this.type === 14) {//单选值
                widgetDef.initValue = Array.isArray(initValue) ? initValue[0] : initValue;
            } else {
                widgetDef.initValue = initValue;
                if (typeof widgetDef.initValue === "string") {
                    widgetDef.initValue = widgetDef.initValue.split(",")
                }
            }
            //对选项进行校验,发现没有value和label的直接把本身值作为value
            let firstOption = widgetDef.optionValues[0];
            if (firstOption && (typeof firstOption) === 'string') {
                widgetDef.optionValues = widgetDef.optionValues.map(op => {
                    return { value: op, label: op };
                })
            }
            //对下拉值是数字类型的进行处理
            if (firstOption && (typeof firstOption.value) === 'number' && (typeof widgetDef.initValue) === 'string' && isFinite(widgetDef.initValue)) {
                widgetDef.initValue = parseInt(widgetDef.initValue);
            }
        } else if (this.type === FormElementType.MACHINE_TARGET) {//目标选择器
            let tInitValue = this.value || this.defaultValue;
            tInitValue = Array.isArray(tInitValue) ? tInitValue[0] : tInitValue;
            //widgetDef.targetDefiner=new TargetDefiner(tInitValue);
        } else if (this.type === FormElementType.DATE) {
            if (initValue instanceof moment) {
                widgetDef.initValue = initValue;
            } else {
                if (Array.isArray(initValue)) initValue = initValue.join("");
                if (isFinite(initValue) && initValue) {
                    widgetDef.initValue = moment(parseInt(initValue));
                } else {
                    if (initValue.length > 4) widgetDef.initValue = moment(initValue);
                }
            }
        } else if (this.type === FormElementType.DATA_RANGE) {
            let { defaultTime } = this.defModel;
            if (defaultTime) {
                let { stimeOffsetFromNow, etimeOffsetFromNow } = defaultTime, stime = moment().subtract(2, 'h'), etime = moment();
                if (stimeOffsetFromNow) {
                    stime = moment().subtract(stimeOffsetFromNow[0], stimeOffsetFromNow[1]);
                }
                if (etimeOffsetFromNow) {
                    etime = moment().subtract(etimeOffsetFromNow[0], etimeOffsetFromNow[1]);
                }
                widgetDef.initValue = [stime, etime]
            } else {
                widgetDef.initValue = initValue.map(time => {
                    if (time instanceof moment) {
                        return time;
                    }
                    return isFinite(time) && time ? moment(parseInt(time)) : moment(time);
                })
            }
        } else if (this.type === FormElementType.TIME_PICKER) {
            if (Array.isArray(initValue)) initValue = initValue.join("");
            widgetDef.initValue = moment(initValue, 'HH:mm:ss');
        } else if (this.type === 16) {
            if (Array.isArray(initValue)) {
                initValue = initValue[0];
            }
            if (initValue === "false") {
                initValue = false;
            } else if (initValue === "true") {
                initValue = true;
            }
            if (typeof initValue === "boolean") {
                widgetDef.defaultChecked = initValue
            }
            widgetDef.initValue = initValue;
        } else if (this.type === 83) {//ace编辑器
            if (Array.isArray(initValue)) {
                initValue = initValue.join("");
            }
            widgetDef.initValue = initValue;
        } else if (this.type === FormElementType.SELECT_TREE || this.type === 12) {//选择树
            widgetDef.treeData = this.options || this.defModel.options || initValue;
        } else {
            widgetDef.initValue = initValue;
        }
        return widgetDef;
    }
    /**
     * 获取参数显示组件模型,注意此方法为异步方法,返回的是Promise
     */
    getParamWidgetDef() {
        return this.loadInitData().then(result => {
            return this.getSyncParamWidgetDef();
        });
    }

    loadInitData() {
        let { transform, optionMapping, remote } = this.defModel, loader = false;
        //增加数据源支持
        if (this.dataSourceMeta) {
            let dataSource = new DataSource(this.dataSourceMeta);
            //此处的变量已经在生产参数项的时候已经被渲染了,暂时不需要取取运行时节点变量
            loader = dataSource.query(this.runtimeContext || {});
        } else if (this.initApi && remote !== true) {
            loader = httpClient.get(this.initApi);
        }
        if (loader) {
            return loader.then(result => {
                //最大程度的兼容老接口
                let initValue = result.initValue || result.defaultValue || '';
                //存在数据转换
                if (transform) {
                    this.value = _.get(result, transform.initValue);
                    if (transform.options) this.options = _.get(result, transform.options);
                } else {
                    if (result.options) {
                        this.options = result.options;
                    }
                    //后端接口返回有初始值,只当前参数定义不存在值时才进行赋值
                    if (initValue && (!this.value || !this.value.length)) {
                        this.value = initValue;
                    }
                    if (!initValue && !result.options) {
                        if (Array.isArray(result)) {
                            this.options = result;
                        } else {
                            this.value = result;
                        }
                    }
                }
                if (optionMapping) {
                    this.options = this.options.map(o => {
                        return Object.assign({}, o, { label: _.get(o, optionMapping.label), value: _.get(o, optionMapping.value) })
                    });
                }
                return {};
            });
        } else {
            return new Promise(
                function (resolve, reject) {
                    return resolve({});
                }
            );
        }
    };


    isVisible() {
        return this.visible === 2;
    }

    updateValue(value) {
        //对时间类型的处理需要进行字符串化
        if (this.type === FormElementType.DATE) {
            let { timestamp } = this.defModel;
            if (value instanceof moment) {
                if (timestamp) {
                    this.value = value.valueOf();
                } else {
                    this.value = value;
                }
            } else if (isFinite(value)) {
                if (timestamp) {
                    this.value = parseInt(value);
                } else {
                    this.value = moment(parseInt(value));
                }
            } else {
                if (typeof (value) === 'string') value = moment(value, 'YYYY-MM-DD HH:mm:ss');
                this.value = value && value.format && value.format('YYYY-MM-DD HH:mm:ss');
            }
        } else if (this.type === FormElementType.TIME_PICKER) {
            if (typeof (value) === 'string') value = moment(value, 'HH:mm:ss');
            this.value = value && value.format && value.format('HH:mm:ss');
        } else if (this.type === FormElementType.DATA_RANGE) {
            if (!value.map && value.indexOf && value.indexOf(",") > 0) {
                value = value.split(",");
            }
            let { timestamp } = this.defModel;
            this.value = value.map(time => {
                if (typeof (time) === 'string' || typeof (time) === 'number') return time;
                if (time instanceof moment) {
                    if (timestamp) {
                        return time.valueOf();
                    }
                    return time.format('YYYY-MM-DD HH:mm:ss');
                }
            });
        } else if (this.type === FormElementType.MACHINE_TARGET) {
            this.value = value;
        } else if (this.type === FormElementType.INPUT || this.type === FormElementType.TEXTAREA || this.type === FormElementType.HIDDEN_INPUT) {
            this.value = Array.isArray(value) ? value.join(",") : value;
        } else {
            this.value = value;
        }
        //增加外界显式调用更新参数值时,需要更新配置中的默认值
        if (this.defModel.hasOwnProperty("defaultValue")) {
            this.defModel.defaultValue = this.value;
        }
    }

    getValue() {
        //提交值类型
        let { submitType } = this.defModel;
        if (submitType === 'string' && Array.isArray(this.value)) {
            return this.value.join(",");
        }
        if (submitType === 'array' && (typeof (this.value) === 'string')) {
            return this.value.split(",");
        }
        if ((this.type === FormElementType.INPUT || this.type === FormElementType.TEXTAREA) && Array.isArray(this.value)) {
            return this.value.join(",");
        } else if ((this.type === FormElementType.GRID_CHECKBOX || this.type === FormElementType.MULTI_SELECT || this.type === FormElementType.CHECKBOX) && (typeof (this.value) === 'string')) {
            return [this.value];
        } else if (this.type === 16 && Array.isArray(this.value)) {
            return !!this.value.join(",");
        } else if (this.type === FormElementType.SELECT && Array.isArray(this.value) && this.value.length === 0) {
            return "";
        }
        return this.value;
    }

    /**
     * 获取经过处理后的显示值
     */
    getDisplayValue() {
        if (this.type === FormElementType.MACHINE_TARGET) {
        } else if (this.type === FormElementType.DATA_RANGE) {
            return this.value[0] + " - " + this.value[1];
        } else if (this.type === FormElementType.USER_SELECTOR) {
            let users = [];
            this.value.forEach(user => {
                users.push(user.nickNameCn + "(" + user.name + ")");
            });
            return users.join(" , ");
        } else {
            return Array.isArray(this.value) ? this.value.join(" , ") : this.value;
        }
    }
}