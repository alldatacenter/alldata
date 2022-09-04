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





class ToolbarSetting extends React.Component {

    constructor(props) {
        super(props);
        let { widgetModel, config, noFilter, noHelp, configIndex = "toolbar" } = props;
        let paramsDef = [], toolbar = config[configIndex];
        let { filter, type = "button",label="操作", actionList = [], docList = [], customRender } = toolbar || {}, filterBlocks = [], actionBlocks = [];
        widgetModel.nodeModel.getBlocks().forEach(block => {
            let { elementId, label, category } = block;
            let option = { value: elementId, label: label, category };
            if (category === Constants.BLOCK_CATEGORY_FILTER) {
                filterBlocks.push(option);
            } else {
                actionBlocks.push(option);
            }
        });
        let toolbarGroup = [];
        if (!noFilter) {
            toolbarGroup.push(
                {
                    name: "过滤器",
                    items: [
                        {
                            type: FormElementType.SELECT, name: 'filter', initValue: filter, required: false, label: "选择过滤区块", inputTip: "来源于页面区块中定义的过滤器",
                            optionValues: filterBlocks,
                            layout: {
                                span: 24
                            }
                        }
                    ]
                }
            )
        }
        toolbarGroup.push(
            {
                name: "操作",
                items: [
                    {
                        type: FormElementType.RADIO, name: 'type', initValue: type, required: false, label: "展示样式",
                        optionValues: [{ value: 'button', label: '按钮' }, { value: "link", label: '超链' }],
                        layout: {
                            span: 24
                        }
                    },
                    {
                        type: FormElementType.INPUT, name: 'label', initValue: label, required: false, label: "展示标签",
                        layout: {
                            span: 24
                        }
                    },
                    {
                        type: 85, name: 'actionList', required: false, label: '操作定义', initValue: actionList,
                        defModel: {
                            "pagination": false,
                            "enableEdit": true,
                            "enableRemove": true,
                            "columns": [
                                {
                                    "dataIndex": "block",
                                    "width": 170,
                                    "title": "操作区块",
                                    "editProps": {
                                        "inputTip": "选择定义的操作区块",
                                        "required": true,
                                        "type": FormElementType.SELECT,
                                        "optionValues": actionBlocks
                                    },
                                },

                                {
                                    "editProps": {
                                        "required": false,
                                        "type": 1,
                                        "inputTip": "操作的名称",
                                    },
                                    "dataIndex": "label",
                                    "title": "操作名称"
                                },
                                {
                                    "editProps": {
                                        "initValue": "default",
                                        "optionValues": [
                                            {
                                                "value": "default",
                                                "label": "默认"
                                            },
                                            {
                                                "value": "primary",
                                                "label": "重要"
                                            },
                                            {
                                                "value": "danger",
                                                "label": "高危"
                                            }
                                        ],
                                        "required": false,
                                        "type": FormElementType.SELECT
                                    },
                                    "dataIndex": "type",
                                    "title": "操作类型"
                                },
                                {
                                    "editProps": {
                                        "initValue": "horizontal",
                                        "optionValues": [
                                            {
                                                "value": "horizontal",
                                                "label": "水平"
                                            },
                                            {
                                                "value": "vertical",
                                                "label": "垂直"
                                            }
                                        ],
                                        "required": false,
                                        "type": FormElementType.SELECT
                                    },
                                    "dataIndex": "layout",
                                    "title": "布局方式"
                                },
                                {
                                    "editProps": {
                                        "initValue": "drawer",
                                        "optionValues": [
                                            {
                                                "value": "drawer",
                                                "label": "右侧划出"
                                            },
                                            {
                                                "value": "modal",
                                                "label": "对话框"
                                            }
                                        ],
                                        "required": false,
                                        "type": FormElementType.SELECT
                                    },
                                    "dataIndex": "displayType",
                                    "title": "展示方式"
                                },
                                {
                                    "editProps": {
                                        "initValue": "default",
                                        "optionValues": [
                                            {
                                                "value": "small",
                                                "label": "小"
                                            },
                                            {
                                                "value": "default",
                                                "label": "默认"
                                            },
                                            {
                                                "value": "large",
                                                "label": "大"
                                            }
                                        ],
                                        "required": false,
                                        "type": FormElementType.SELECT
                                    },
                                    "dataIndex": "size",
                                    "title": "宽度"
                                },
                                {
                                    "editProps": {
                                        "required": false,
                                        "type": 1,
                                        "inputTip": "antd3中的icon名",
                                    },
                                    "dataIndex": "icon",
                                    "title": "图标"
                                }
                            ],
                            "enableAdd": true
                        },
                        layout: {
                            span: 24
                        }
                    }
                ]
            }
        );
        toolbarGroup.push(
            {
                name: "自定义JSX渲染",
                items: [
                    {
                        type: FormElementType.TEXTAREA, name: 'customRender', initValue: customRender,
                        required: false, label: "JSX模板", tooltip: "填写标准的JSX,支持变量占位;支持antd组件、内置render组件,引用方式<antd.xxx>,<common.xxx>",
                        layout: {
                            span: 24
                        }
                    }
                ]
            }
        );
        if (!noHelp) {
            toolbarGroup.push(
                {
                    name: "帮助栏",
                    items: [
                        {
                            type: 85, name: 'docList', required: false, label: '文档链接', initValue: docList,
                            layout: {
                                span: 24
                            },
                            defModel: {
                                "pagination": false,
                                "enableEdit": true,
                                "enableRemove": true,
                                "columns": [

                                    {
                                        "editProps": {
                                            "required": false,
                                            "type": 1,
                                            "inputTip": "帮助文档的标题",
                                        },
                                        "dataIndex": "label",
                                        "title": "文档标题"
                                    },
                                    {
                                        "editProps": {
                                            "required": false,
                                            "inputTip": "帮助文档的链接路径",
                                            "type": 1
                                        },
                                        "dataIndex": "path",
                                        "title": "链接路径"
                                    },
                                    {
                                        "editProps": {
                                            "required": false,
                                            "type": 1,
                                            "inputTip": "antd3中的icon名",
                                        },
                                        "dataIndex": "icon",
                                        "title": "图标"
                                    },

                                ],
                                "enableAdd": true
                            }
                        }
                    ]
                }
            )
        }
        paramsDef.push(
            { type: 89, name: 'toolbarSetting', required: false, label: ' ', groups: toolbarGroup }
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
const ToolbarSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(changedValues, allValues)
})(ToolbarSetting);

export default ToolbarSettingForm;