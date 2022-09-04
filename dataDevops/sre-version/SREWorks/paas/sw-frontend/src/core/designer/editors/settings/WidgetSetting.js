

/**
 * @author caoshuaibiao
 * @date 2021/6/23 16:44
 * @Description:通用属性定义
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
        md: { span: 8 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 16 },
    },
};
class WidgetSetting extends React.Component {

    constructor(props) {
        super(props);
        let { widgetModel, config } = props;
        let paramsDef = [], { title, visibleExp = '', dependParams, backgroundColor = "#ffffff", headerColor = "#ffffff", cardBorder = true,
            refreshInterval, style = {}, foldEnable, hiddenEnable, hasWrapper = Constants.CARD_WRAPPER_DEFAULT, wrapper = Constants.CARD_WRAPPER_DEFAULT } = config;
        paramsDef.push({ type: FormElementType.INPUT, name: 'title', initValue: title, required: false, label: "组件标题", tooltip: "组件显示的卡片标题" });
        paramsDef.push({
            type: FormElementType.SELECT_TAG, name: 'dependParams', initValue: dependParams, required: false, label: "组件渲染",
            inputTip: "指定的参数不存在时将不会创建,值改变时会进行刷新",
            tooltip: "指定的参数不存在时将不会创建,值改变时会进行刷新",
            optionValues: widgetModel.nodeModel.getVariables().map(variable => {
                let { name, label } = variable;
                return {
                    value: name,
                    label: label
                }
            })
        });
        paramsDef.push({
            type: FormElementType.INPUT, name: 'visibleExp', initValue: visibleExp, required: false, label: "组件可见",
            tooltip: "为空时默认显示。nodeParams代表节点参数集合,示例如nodeParams.source=='action',表示节点中的source参数为action时展示"
        });
        paramsDef.push({
            type: FormElementType.INPUT, name: 'refreshInterval', initValue: refreshInterval, required: false, label: "组件刷新",
            tooltip: "每隔多久刷新一次卡片,单位秒"
        });
        paramsDef.push({
            type: FormElementType.MODAL_ACE, name: 'style', initValue: style, required: false, label: "卡片样式Style",
            height: "calc(50vh - 150px)",
            showDiff: false,
            tooltip: "填写 React中的css style定义样式对象或标准的css style对象",
            defModel: {
                mode: 'json'
            },
        }
        );
        paramsDef.push({
            type: FormElementType.RADIO, name: 'hasWrapper', initValue: hasWrapper, required: false, label: "组件卡片",
            tooltip: "是否具有包装器卡片",
            optionValues: [{ value: Constants.CARD_WRAPPER_DEFAULT, label: '有卡片封装' },
            { value: Constants.CARD_WRAPPER_NONE, label: '无卡片封装' }]
        });
        paramsDef.push({
            type: FormElementType.RADIO, name: 'wrapper', initValue: wrapper, required: false, label: "卡片风格",
            tooltip: "包装器卡片的整体风格式样",
            optionValues: [{ value: Constants.CARD_WRAPPER_DEFAULT, label: '基本' },
            { value: Constants.CARD_WRAPPER_ADVANCED, label: '高级' },
            { value: Constants.CARD_WRAPPER_TITLE_TRANSPARENT, label: '标题透明' },
            { value: Constants.CARD_WRAPPER_TRANSPARENT, label: '透明' },
            ]
        });

        paramsDef.push({
            type: FormElementType.SWITCH, name: 'foldEnable', initValue: foldEnable, required: false, label: "卡片可收起",
            defModel: {
                defaultChecked: foldEnable
            },
            tooltip: "是否卡片内容区可收起"
        }
        );
        paramsDef.push({
            type: FormElementType.SWITCH, name: 'hiddenEnable', initValue: hiddenEnable, required: false, label: "卡片可关闭",
            defModel: {
                defaultChecked: hiddenEnable
            },
            tooltip: "可以在页面隐藏卡片"
        }
        );
        paramsDef.push({
            type: FormElementType.SWITCH, name: 'cardBorder', initValue: cardBorder, required: false, label: "卡片带边框",
            defModel: {
                defaultChecked: cardBorder
            },
        }
        );
        paramsDef.push({ type: FormElementType.COLOR_PICKER, name: 'backgroundColorObj', defaultColor: backgroundColor, required: false, label: "卡片背景色", tooltip: "整个卡片的背景色", enableAlpha: false });
        paramsDef.push({ type: FormElementType.INPUT, name: 'headerColorObj', initValue: headerColor, required: false, label: "卡片标题色", tooltip: "卡片头的背景色，直接输入颜色色号即可，输入‘theme’，则保持和主题色同步", enableAlpha: false });
        this.itemDef = paramsDef;

    }


    buildingFormElements = () => {
        let { form } = this.props;
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, formItemLayout));
        let values = form.getFieldsValue();
        return (
            <Row className="target-form-setting">
                <Col span={8}>
                    {
                        values.hasWrapper !== Constants.CARD_WRAPPER_NONE &&
                        [
                            formChildrens[0],
                            formChildrens[1],
                            formChildrens[2],
                            formChildrens[3],
                            formChildrens[5],
                            formChildrens[6],
                        ]
                    }
                    {
                        values.hasWrapper === Constants.CARD_WRAPPER_NONE &&
                        [
                            formChildrens[0],
                            formChildrens[1],
                            formChildrens[2],
                            formChildrens[3],
                            formChildrens[5],
                        ]
                    }
                </Col>
                <Col span={6} offset={1}>
                    {
                        values.hasWrapper !== Constants.CARD_WRAPPER_NONE &&
                        [
                            formChildrens[7],
                            formChildrens[8],
                            formChildrens[9],
                            formChildrens[10],
                            formChildrens[11],
                            formChildrens[4],
                        ]
                    }
                </Col>
                {/* <Col span={8}>
                    {
                        values.wrapper!==Constants.CARD_WRAPPER_NONE &&
                        [
                            formChildrens[4],
                        ]
                    }
                </Col> */}
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
const WidgetSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => {
        let { backgroundColorObj, headerColorObj, ...otherConfig } = allValues;
        if (backgroundColorObj) {
            otherConfig.backgroundColor = backgroundColorObj.color;
        }
        if (headerColorObj) {
            // otherConfig.headerColor = headerColorObj.color;
            //改造卡片头部，允许配置主题色变量
            otherConfig.headerColor = headerColorObj
        }
        props.onValuesChange && props.onValuesChange(changedValues, otherConfig)
    }
})(WidgetSetting);

export default WidgetSettingForm;
