/**
 * @author caoshuaibiao
 * @date 2021/6/23 16:42
 * @Description:过滤器属性定义
 */
import React from 'react';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import './formCommon.less'
import { Row, Col, } from 'antd';
import Constants from "../../../framework/model/Constants";
import FormElementType from "../../../../components/FormBuilder/FormElementType";
import FormElementFactory from "../../../../components/FormBuilder/FormElementFactory";

const formItemLayout = {
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



class FilterSetting extends React.Component {

    constructor(props) {
        super(props);
        let { widgetModel, config } = props;
        this.isFilterForm = widgetModel.type === 'FILTER_FORM';
        this.isTabFilter = widgetModel.type === 'FILTER_TAB';
        let paramsDef = [], { outputName = '', form, column, labelSpan, autoSubmit, enterSubmit, hasBackground = false, showSearch = true, tabType = "Button", tabSize = "default", tabPosition = 'top-left', scenes = [],visibleExpression='' } = config;
        paramsDef.push({ type: FormElementType.INPUT, name: 'outputName', initValue: outputName, required: false, label: "输出对象名称", tooltip: "过滤器表单值输出到填写的参数名对象中,为空时表单项值全部独立输出到节点参数域中" });
        if (this.isFilterForm) {
            paramsDef.push({
                type: FormElementType.SELECT, name: 'column', initValue: column, required: false, label: "一行几列", inputTip: "一行放置几个表单项",
                optionValues: [{ value: "2", label: '2' }, { value: "3", label: '3' }, { value: "4", label: '4' }, { value: "5", label: '5' }, { value: "6", label: '6' }]
            });
            paramsDef.push({
                type: FormElementType.SELECT, name: 'labelSpan', initValue: labelSpan, required: false, label: "label占比", inputTip: "参数标签宽度占比,总24",
                optionValues: [{ value: "2", label: '2' }, { value: "3", label: '3' }, { value: "4", label: '4' }, { value: "5", label: '5' }, { value: "6", label: '6' }, { value: "7", label: '7' }, { value: "8", label: '8' }]
            });
        }
        paramsDef.push({
            type: FormElementType.RADIO, name: 'enterSubmit', initValue: enterSubmit === true || false, required: false, label: "Enter触发查询",
            optionValues: [{ value: true, label: '是' }, { value: false, label: '否' }]
        });
        paramsDef.push({
            type: FormElementType.RADIO, name: 'autoSubmit', initValue: autoSubmit === true || false, required: false, label: "值改变触发查询",
            optionValues: [{ value: true, label: '是' }, { value: false, label: '否' }]
        });
        paramsDef.push({
            type: FormElementType.RADIO, name: 'showSearch', initValue: showSearch === true || false, required: false, label: "显示查询按钮",
            optionValues: [{ value: true, label: '是' }, { value: false, label: '否' }]
        });
        paramsDef.push({
            type: FormElementType.RADIO, name: 'hasBackground', initValue: hasBackground, required: false, label: "具有包装容器",
            optionValues: [{ value: true, label: '是' }, { value: false, label: '否' }]
        });
        paramsDef.push({
            type: 83, name: 'scenes', initValue: scenes || "[]", required: false, label: "快捷过滤定义",
            height: "calc(50vh - 100px)",
            showDiff: false,
            defModel: { height: "calc(50vh - 100px)", disableShowDiff: true, mode: "json", width: 360 },
            tooltip: "预先设定好过滤器的过滤值,进行快捷查询。数组的每个元素包含 values、title、category(可选用于分类)三个key，values为过滤器中的表单项值,title为过滤项名称,category启用场景分组"
        });
        paramsDef.push({
            type: FormElementType.INPUT, name: 'visibleExpression', initValue: visibleExpression, required: false, label: "可见表达式",
            tooltip: "通过表达式条件来实现对过滤器区块进行隐藏/展示",inputTip: "直接用nodeParams.参数名或$(参数名)来引用参数阈其他参数值" 
        })
        if (this.isTabFilter) {
            paramsDef = [
                { type: FormElementType.INPUT, name: 'outputName', initValue: outputName, required: false, label: "输出对象名称", tooltip: "过滤器表单值输出到填写的参数名对象中,为空时表单项值全部独立输出到节点参数域中" },
                {
                    type: FormElementType.RADIO, name: 'tabPosition', initValue: tabPosition, required: false, label: "tab展示位置",
                    optionValues: [{ value: 'top-left', label: '左上' }, { value: 'top-right', label: '右上' },{ value: 'left', label: '左' }],
                    tooltip: "tab的展示位置,左上表示紧随标题,右上表示居右，左下表示竖向过滤器"
                },
                {
                    type: FormElementType.RADIO, name: 'tabSize', initValue: tabSize, required: false, label: "tab展示大小",
                    optionValues: [{ value: 'small', label: 'small' }, { value: 'default', label: 'default' }, { value: 'large', label: 'large' }],
                    tooltip: "tab的Size大小"
                },
                {
                    type: FormElementType.RADIO, name: 'tabType', initValue: tabType, required: false, label: "tab展示类型",
                    optionValues: [{ value: 'Button', label: 'Button' }, { value: 'Tab', label: 'Tab' }, { value: 'Switch', label: 'Switch' }],
                    tooltip: "tab过滤器呈现的渲染形式"
                },
                {
                    type: FormElementType.INPUT, name: 'visibleExpression', initValue: visibleExpression, required: false, label: "可见表达式",
                    tooltip: "直接用nodeParams.参数名或$(参数名)来引用参数阈其他参数值",inputTip: "直接用nodeParams.参数名或$(参数名)来引用参数阈其他参数值" 
                }
            ]
        }
        this.itemDef = paramsDef;

    }


    buildingFormElements = () => {
        let { form } = this.props;
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, formItemLayout));
        if (this.isTabFilter) {
            return <Row>
                <Col span={9}>
                    {formChildrens}
                </Col>
                <Col span={9}></Col>
            </Row>
        }
        if (this.isFilterForm) {
            return <Row>
                <Col span={9}>
                    {
                        [
                            formChildrens[0],
                            formChildrens[1],
                            formChildrens[2],
                            formChildrens[3],
                            formChildrens[4],
                            formChildrens[5]
                        ]
                    }
                </Col>
                <Col span={9}>
                    {
                        [
                            formChildrens[6],
                            formChildrens[8],
                            formChildrens[7],
                        ]
                    }
                </Col>
            </Row>
        }
        return (
            <Row>
                <Col span={9}>
                    {
                        [
                            formChildrens[0],
                            formChildrens[1],
                            formChildrens[2],
                            formChildrens[3],
                            formChildrens[4],
                        ]
                    }
                </Col>
                <Col span={9}>
                    {
                        [
                            formChildrens[6],
                            formChildrens[5]
                        ]
                    }
                </Col>
            </Row>

        )
    };

    render() {

        let formChildrens = this.buildingFormElements();
        return (
            <Form>
                {formChildrens}
            </Form>
        );
    }

}
const FilterSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(changedValues, allValues)
})(FilterSetting);

export default FilterSettingForm;