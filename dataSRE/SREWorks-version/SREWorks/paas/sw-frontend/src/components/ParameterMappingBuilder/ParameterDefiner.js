/**
 * Created by caoshuaibiao on 2019/1/22.
 * 参数映射定义者,要求所定义的参数不允许同名
 */

import Parameter from './Parameter';
import FormElementType from '../FormBuilder/FormElementType';
import localeHelper from '../../utils/localeHelper';
import _ from 'lodash';

export default class ParameterDefiner {

    /**
     * 在工单中,数组中的每个元素是作业模板的摘要信息
     * bindingParamTree格式:[
     *    {
     *       label:"",//显示绑定的标签
     *       name:"",//原本的绑定名称
     *       value:"",
     *       id:"",//作业模板id
     *       operateKey:""//为参数树的唯一key
     *       runMode:1,// 1:串行  2:并行 (对于绑定执行的有意义,其他场景无意义冗余)
     *       startNode:2// 1、手工 2:自动  启动方式
     *       params:[
     *          {
     *            label:"",
     *            name:"",
     *            value:"",
     *            id:"",
     *          }
     *       ]
     *    }
     * ]
     *
     * paramMapping格式:[
     *    {
     *         parameter:{
     *
     *         },
     *         mapping:[
     *            ${bindingParamTree} 片段
     *         ]
     *    }
     *
     * ]
     *
     * paramRepeat:是否允许添加同名的参数,一般用在动态生成作业的场景,提单定制场景不允许同名参数,
     * noBinding:false/true 为true时表示不存在binding树只是参数定义
     *
     */

    constructor(paramDefData, runtimeContext) {
        this.bindingParamTree = paramDefData.bindingParamTree || [];
        this.paramMapping = [];
        this.paramRepeat = paramDefData.paramRepeat || false;
        this.noBinding = paramDefData.noBinding;
        this.extConfig = paramDefData.extConfig;
        let me = this;
        paramDefData.paramMapping.forEach(pm => {
            me.addParamMapping(new Parameter(pm.parameter, runtimeContext), pm.mapping)
        });
    }

    /**
     * 添加参数定义
     * @Parameter parameter
     */
    addParameter(parameter) {
        if (!this.getParamMapping(parameter.name) || this.paramRepeat) {
            this.paramMapping.push({
                parameter: parameter,
                mapping: []
            });
        }
    }

    /**
     * 添加默认参数
     * @paramName 参数名
     */
    addDefaultParam(paramName) {
        let defaultParameter = new Parameter({
            label: paramName,
            name: paramName,
            value: '',
        });
        this.addParameter(defaultParameter);
    }

    /**
     * 添加mapping
     * @string paramName 参数名称
     * @param treeFragment
     */
    addMapping(paramName, fragment) {
        let binding = this.getParamMapping(paramName);
        if (binding) {
            if (Array.isArray(fragment)) {
                binding.mapping = fragment;
            } else {
                binding.mapping.push(fragment);
            }
        }
    }

    /**
     * 添加参数及其映射
     * @Parameter param
     * @param treeFragment
     */
    addParamMapping(param, fragment) {
        this.addParameter(param);
        this.addMapping(param.name, fragment);
    }

    /**
     * 移除参数
     * @param paramName
     */
    removeParam(paramName) {
        for (let p = this.paramMapping.length - 1; p >= 0; p--) {
            if (this.paramMapping[p].parameter.name === paramName) {
                this.paramMapping.splice(p, 1);
            }
        }
    }

    /**
     * 清除指定参数映射
     * @param paramName
     */
    clearParamMapping(paramName) {
        for (let p = this.paramMapping.length - 1; p >= 0; p--) {
            if (this.paramMapping[p].parameter.name === paramName) {
                this.paramMapping[p].mapping = [];
            }
        }
    }

    /**
     * 获取参数映射
     * @param paramName
     */
    getParamMapping(paramName) {
        let paramMapping = false;
        this.paramMapping.forEach(pm => {
            if (pm.parameter.name === paramName) {
                paramMapping = pm;
            }
        });
        return paramMapping;
    }
    /**
     * 获取输入参数部件
     */
    getParameters() {
        let parameter = [], me = this;
        if (this.bindingParamTree.length === 0 && !this.noBinding) return parameter;
        //不存在的情况默认把参数树中的每个参数作为独立映射
        if (this.paramMapping.length === 0 && this.bindingParamTree.length > 0) {
            this.bindingParamTree.forEach(paramTree => {
                //默认增加一个节点选择器参数
                let defaultTarget = new Parameter({
                    label: localeHelper.get('common.targetNode', "目标节点"),
                    name: "target",
                    id: 0,
                    type: FormElementType.MACHINE_TARGET,
                });
                me.addParameter(defaultTarget);

                let targetsBings = [], initMapping = {
                    label: paramTree.label,
                    name: paramTree.name,
                    value: paramTree.value,
                    operateKey: paramTree.operateKey,
                    runMode: paramTree.runMode,
                    startMode: paramTree.startMode,
                    id: paramTree.id,
                    params: []
                };
                paramTree.params.forEach(param => {
                    //过滤目标选择类型参数
                    if (param.type !== FormElementType.MACHINE_TARGET) {
                        let initParamMapping = Object.assign({}, initMapping);
                        initParamMapping.params = [param];
                        me.addParamMapping(new Parameter(param), initParamMapping)
                    } else {
                        //绝大多数的作业公用一个目标
                        targetsBings.push(param);
                    }
                });
                //绑定目标参数与作业步骤目标
                let defaultTargetMapping = Object.assign({}, initMapping);
                defaultTargetMapping.params = targetsBings;
                me.addParamMapping(defaultTarget, defaultTargetMapping);

            });
        }

        this.paramMapping.forEach(pm => {
            parameter.push(pm.parameter);
        });
        return parameter;
    }


    /**
     * 获取参数显示定义
     */
    getParameterDef() {
        let parameters = this.getParameters(), parameterDef = [];
        parameters.forEach(parameter => {
            if (parameter.isVisible()) {
                parameterDef.push(parameter.getParamWidgetDef());
            }
        });
        return Promise.all(parameterDef);
    }

    getSyncParameterDef() {
        let parameters = this.getParameters(), parameterDef = [];
        parameters.forEach((parameter, index) => {
            if (parameter.isVisible()) {
                let item = parameter.getSyncParamWidgetDef();
                if (!item.order) {
                    item.order = index + 1;
                } else {
                    item.order = parseInt(item.order);
                }
                parameterDef.push(item);
            }
        });
        parameterDef.sort(function (a, b) { return a.order - b.order });
        return parameterDef;
    }

    /**
     * 获取参数定义器的默认显示组件模型定义
     * @returns {*}
     */
    getDefaultWidgetDef() {
        return { type: FormElementType.MAPPING_BUILDER, name: 'parameterDefiner', required: false, label: "", parameterDefiner: this, mode: this.noBinding };
    }

    /**
     * 更新参数定义器中的定义的参数值
     * @param paramsValue
     */
    updateParametersValue(paramsValue) {
        let parameters = this.getParameters();
        parameters.forEach(parameter => {
            //let paramValue=paramsValue[parameter.name];
            let paramValue = _.get(paramsValue, parameter.name);
            if (paramsValue.hasOwnProperty(parameter.name) || ((parameter.name.includes(".")) && paramValue)) {
                parameter.updateValue(paramValue);
            } else {
                //参数更新集合不存在值,但存在默认值的场景
                let initValue = Array.isArray(parameter.value) ? parameter.value.join(",") : parameter.value;
                if (initValue !== null && initValue != undefined) {
                    if (initValue.length === 0 && parameter.defaultValue && parameter.defaultValue.length > 0) {
                        parameter.updateValue(parameter.defaultValue);
                    }
                }
                //增加目标参数有默认值时的处理
                if (!parameter.isVisible() && parameter.type === 91) {
                    parameter.updateValue(initValue || parameter.defaultValue[0]);
                }

            }
        });
    }


    removeBindingHasDefaultValueParam() {
        for (let p = this.paramMapping.length - 1; p >= 0; p--) {
            //只有绑定参数的所有映射参数全部存在默认值才会移除
            let pm = this.paramMapping[p], noDefValue = false;
            pm.mapping.forEach(m => {
                let bindingParams = m.params;
                bindingParams.forEach(bp => {
                    if (!bp.value && bp.value !== 0) {
                        noDefValue = true;
                    }
                });
            });

            if (!noDefValue) {
                this.paramMapping.splice(p, 1);
            }
        }
    }
}