/**
 * Created by caoshuaibiao on 2019/1/22.
 * 简单表单,支持两种模式,一种是传入onSubmit函数自己处理通过验证后的值,一种是传入submitApi,表单自动生成提交按钮及提交至指定的api
 */

import React, { PureComponent } from 'react';
import { Form, Icon as LegacyIcon } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Card, Row, Col, Alert, Divider } from 'antd';
import FormElementFactory from './FormElementFactory'
import JSXRender from "../../components/JSXRender";
import * as util from '../../utils/utils';
import safeEval from '../../utils/SafeEval';
import debounce from 'lodash.debounce';

const defaultFormItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 6 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 16 },
    },
};

class SimpleForm extends PureComponent {

    constructor(props) {
        super(props);
        let { onSubmit } = props;
        if (onSubmit) {
            onSubmit.validate = this.validate;
        }
        this.state = {
            displayMode: 'base'
        }
        this.hasType = false;
        this.autoSubmit = debounce(this.autoSubmit, 500);
        this.prevSubmitValues = {};
    }


    validate = () => {
        let pass = false, { onSubmit, items } = this.props;
        this.props.form.validateFieldsAndScroll((err, values) => {
            if (!err) {
                let allValues = {};
                /*items.forEach((item,index)=> {
                    let {defModel={}}=item;
                    let {displayType}=defModel;
                    if(displayType==='advanced'){
                        allValues[item.name]=null;
                    }
                });*/
                Object.assign(allValues, values);
                this.prevSubmitValues = Object.assign({}, allValues);
                if (onSubmit) onSubmit(allValues);
                pass = allValues;
            }
        });
        return pass
    };

    handleKeyUp = (event) => {
        event.preventDefault();
        if (event.keyCode === 13) {
            this.validate();
        }
    };

    componentDidMount() {
        let { enterSubmit } = this.props;
        if (enterSubmit) {
            this.formContainer.addEventListener("keyup", this.handleKeyUp);
        }
    }

    componentWillUnmount() {
        let { enterSubmit } = this.props;
        if (enterSubmit) {
            this.formContainer.removeEventListener("keyup", this.handleKeyUp);
        }
    }

    handleDisplayChanged = () => {
        this.setState({
            displayMode: this.state.displayMode === 'base' ? 'advanced' : 'base'
        });
    };

    autoSubmit = () => {
        let submitValues = this.props.form.getFieldsValue(), needSubmit = false, prevValues = this.prevSubmitValues;
        //对比上次和本次的值发生改变了再进行查询,因为存在表单项个数不确定的场景(高级查询),因此需要相互对比。
        Object.keys(submitValues).forEach(key => {
            let subCompValue = submitValues[key], prevCompValue = prevValues[key];
            if (Array.isArray(subCompValue)) subCompValue = subCompValue.join("");
            if (Array.isArray(prevCompValue)) prevCompValue = prevCompValue.join("");
            if ((subCompValue && subCompValue !== prevCompValue) || (!subCompValue && prevCompValue)) {
                needSubmit = true;
            }
        });
        if (needSubmit) {
            this.validate();
        }
    };

    render() {
        //let formChildrens=this.buildingFormElements();
        let { colCount, items, form, formItemLayout, hintFunction, nodeParam = {}, extCol, advanced, autoSubmit, formLayout = 'horizontal' } = this.props;
        let { displayMode } = this.state, hasCategory = false, rows = false, normalItems = [], groupItems = [], categoryMapping = {}, categoryOrderMapping = {};
        //把高级查询条件放置到最后
        if (this.hasType) {
            let base = [], advs = [];
            items.forEach(ni => {
                let { defModel = {} } = ni;
                if (defModel.displayType === 'advanced') {
                    advs.push(ni);
                } else {
                    base.push(ni)
                }
            });
            items = base.concat(advs);
        }
        let initParams = {};
        items.forEach((item, index) => {
            let { layout, defModel = {} } = item;
            let isVisable = true, { displayType } = defModel;
            initParams[item.name] = item.initValue;
            try {
                if (item.visibleExp && item.visibleExp.length > 4) {
                    isVisable = safeEval(item.visibleExp, { formValues: Object.assign(initParams, nodeParam, form.getFieldsValue()) });
                }
            } catch (error) {
                isVisable = true;
                return true
            }
            if (displayType === 'advanced' && displayMode === 'base' && advanced !== false) {
                this.hasType = true;
            } else {
                if (isVisable) {
                    if (layout && layout.category) {
                        hasCategory = true;
                        if (categoryMapping[layout.category]) {
                            categoryMapping[layout.category].push(item);
                        } else {
                            categoryMapping[layout.category] = [item];
                            categoryOrderMapping[layout.category] = item.order || index
                        }
                    } else {
                        normalItems.push(item);
                    }
                }
            }
        });
        //存在分组展示的参数
        if (hasCategory) {
            Object.keys(categoryMapping).forEach(ckey => {
                let cgitems = categoryMapping[ckey];
                groupItems.push(
                    { type: 89, name: ckey, required: false, label: ' ', groups: [{ name: ckey, items: cgitems }], order: categoryOrderMapping[ckey] }
                )
            })
        }
        if (groupItems.length > 0) {
            normalItems = normalItems.concat(...groupItems);
            //normalItems.sort(function(a,b){return a.order-b.order});
        }
        let formChildrens = [];
        normalItems.forEach(function (item) {
            formChildrens.push(FormElementFactory.createFormItem(item, form, formItemLayout ? formItemLayout : defaultFormItemLayout));
        });
        let inRow = false;
        if (colCount) {
            let lastRow = [];
            rows = [];
            for (let i = 0; i < formChildrens.length / colCount; i++) {
                let row = [];
                for (let c = 0; c < colCount; c++) {
                    if (c < (formChildrens.length - i * colCount)) {
                        row.push(
                            <Col span={parseInt(24 / colCount)} key={i + '_' + c}>
                                {formChildrens[i * colCount + c] ? formChildrens[i * colCount + c] : null}
                            </Col>
                        );
                    }
                }
                lastRow = row;
                rows.push(
                    <Row gutter={24} key={i} style={{ marginRight: 8, marginLeft: 8 }}>{row}</Row>
                )
            }
            if (extCol) {
                inRow = true;
                let operRow = (
                    <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                        <div>
                            {extCol}
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column-reverse', marginLeft: 12 }}>
                            {
                                this.hasType ?
                                    <div>
                                        <a style={{ fontSize: 12 }} onClick={this.handleDisplayChanged}>{displayMode === 'base' ? "高级查询" : "普通查询"}<LegacyIcon style={{ marginLeft: 5 }} type={displayMode === 'base' ? "down" : "up"} /></a>
                                    </div>
                                    : null
                            }
                        </div>
                    </div>
                );
                if (lastRow.length === colCount) {
                    rows.push(
                        <Row gutter={24} key="__ext_col" style={{ marginRight: 8, marginLeft: 8 }}>
                            <Col span={24} key="__ext_col">
                                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                                    {operRow}
                                </div>
                            </Col>
                        </Row>
                    )
                } else {
                    lastRow.push(
                        <Col span={parseInt((24 / colCount) * (colCount - lastRow.length))} key="__ext_col" style={{ marginTop: 8 }}>
                            {operRow}
                        </Col>)
                }

            }
        }
        //模板函数有60个字符,可以优化为模板函数定义为常量进行对比
        let hasHint = hintFunction && hintFunction.length > 70, jsxResult;
        if (hasHint) {
            jsxResult = safeEval("(" + hintFunction + ")(nodeParams,formValues)", { nodeParams: nodeParam, formValues: form.getFieldsValue() });
        }
        if (autoSubmit) this.autoSubmit();
        return (
            <div ref={r => this.formContainer = r}>
                <Form key="_simpleForm" style={{ marginTop: 8 }} autoComplete="off" layout={formLayout}>
                    {rows ? rows : formChildrens}
                    {
                        !inRow && extCol ? <div style={{ display: 'flex', justifyContent: 'center' }}>{extCol}</div> : null
                    }
                </Form>
                {
                    hasHint && jsxResult.length > 5 ?
                        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 24 }}>
                            <Alert
                                message=""
                                description={<JSXRender jsx={jsxResult} />}
                                type="info"
                                showIcon
                            />
                        </div>
                        : null
                }
            </div>
        );
    }
}
export default Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(changedValues, allValues)
})(SimpleForm);