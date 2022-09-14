/**
 * @author caoshuaibiao
 * @date 2021/6/23 16:47
 * @Description:
 */
import React from 'react';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import './formCommon.less'
import { Row, Col, } from 'antd';
import Constants from "../../../framework/model/Constants";
import FormElementType from "../../../../components/FormBuilder/FormElementType";
import FormElementFactory from "../../../../components/FormBuilder/FormElementFactory";
import ChartFactory from '../../../framework/core/widgets/DisplayScreens/ChartFactory';





class ScreenSetting extends React.Component {

    constructor(props) {
        super(props);
        let { widgetModel, config, configIndex = "toolbar" } = props;
        let paramsDef = [], chartBlocks = [];
        let { chartDisplayConfig = {} } = config;
        widgetModel.nodeModel.getBlocks().forEach(block => {
            let { elementId, elements, label, category } = block;
            if (elements && elements[0] && elements[0]['config'] && elements[0]['config']['rows'] && elements[0]['config']['rows'][0] && elements[0]['config']['rows'][0]['elements']) {
                let elementArray = elements[0]['config']['rows'][0]['elements'];
                elementArray = elementArray.filter(item => ChartFactory.chartTypeArray.includes(item.type))
                if (elementArray && elementArray.length) {
                    let option = { value: elementId, label: label, category };
                    chartBlocks.push(option)
                }
            }
        });
        let chartGroup = [
            { name: "图表一", onlyKey: 'chart_one' },
            { name: "图表二", onlyKey: 'chart_two' },
            { name: "图表三", onlyKey: 'chart_three' },
            { name: "图表四", onlyKey: 'chart_four' },
            { name: "图表五", onlyKey: 'chart_five' },
            { name: "图表六", onlyKey: 'chart_six' },
            { name: "图表七", onlyKey: 'chart_seven' },
            { name: "图表八", onlyKey: 'chart_eight' },
            { name: "图表九", onlyKey: 'chart_nine' },
            { name: "图表十", onlyKey: 'chart_ten' },
            { name: "图表十一", onlyKey: 'chart_eleven' },
            { name: "图表十二", onlyKey: 'chart_twelve' }];
        chartGroup.forEach(item => {
            item.items = [
                {
                    type: FormElementType.SELECT, name: item.onlyKey, initValue: chartDisplayConfig[item.onlyKey], required: false, label: "选择图表区块", inputTip: "来源于图表页面区块中定义的图表",
                    optionValues: chartBlocks,
                    layout: {
                        span: 24
                    }
                }
            ]
        })
        paramsDef.push(
            { type: 89, name: 'toolbarSetting', required: false, label: ' ', groups: chartGroup }
        );
        this.itemDef = paramsDef;

    }


    buildingFormElements = () => {
        let { form } = this.props;
        const toolbarLayout = {
            labelCol: {
                xs: { span: 24 },
                sm: { span: 24 },
                md: { span: 3 },
            },
            wrapperCol: {
                xs: { span: 24 },
                sm: { span: 24 },
                md: { span: 18 },
            },
        };
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, toolbarLayout));
        return (
            <Row>
                <Col span={22}>
                    {
                        [

                            formChildrens[0],
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
const ScreenSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(changedValues, allValues)
})(ScreenSetting);

export default ScreenSettingForm;