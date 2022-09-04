/**
 * Created by caoshuaibiao on 2020/9/9.
 * 由schema及json数据动态生成表单项
 */
import React from 'react';
import { CodeOutlined, FormOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import {
    Spin,
    Button,
    Card,
    Modal,
    Tooltip,
    List,
    Row,
    Col,
    Divider,
    Collapse,
    Tabs,
} from 'antd';
import FormElementFactory from '../FormElementFactory'
import _ from 'lodash';
import JSONEditor from './JSONEditor';
import safeEval from '../../../utils/SafeEval';

const Panel = Collapse.Panel;
const { TabPane } = Tabs;
const FormItem = Form.Item;
const typeMapping = {
    "Input": 1,
    "Text": 2,
    "Select": 3,
    "MultiSelect": 4,
    "CheckBox": 11,
    "Radio": 10,
    "EditTable": 85,
    "JSON": 83,
    "COLOR_PICKER": 77,
    "TEXTAREA": 2,
    "Switch": 16,
    "MODAL_ACE": 78,
    "ImageUpload": 811,
    "JSONEditor": 86,
    'DISABLED_INPUT': 112,
    'DISABLED_TEXTAREA': 113,
    'IMAGE_UPLOAD_MULTI': 814,
    'HANDLE_TAG': 27,
    'ACEVIEW_JAVASCRIPT': 831,
    'INPUT_NUMBER':114
};
const selectType = ["Select", "MultiSelect", "CheckBox", "Radio"];
/*
let myschema={
        "type": "object",
        "properties": {
            "COMMON": {
                "type": "object",
                "title": "公共属性",
                "x-component-props": {
                    "labelCol": 7,
                    "wrapperCol": 12
                },
                "properties": {
                    "name":{
                        "type":"string",
                        "title":"项目名",
                        "pattern":"[a-z]",
                        "required":true,
                        "description":"用于存储 机器基本信息tsar、基础信息（changfree、armory）",
                        "x-component":"Input"
                    },
                    "type":{
                        "type":"string",
                        "title":"类型",
                        "description":"集群类型",
                        "x-component":"Select",
                        "x-component-props":{
                            "options":[{"value":"a","label":"计算集群"},{"value":"b","label":"存储集群"}]
                        }
                    },
                    "region":{
                        "type":"x-array",
                        "title":"地域列表",
                        "properties":{
                            "type": "object",
                            "properties":{
                                "network":{
                                    "type":"string",
                                    "title":"网络域",
                                    "x-component":"Input",
                                },
                                "area":{
                                    "type":"string",
                                    "title":"地域",
                                    "x-component":"Input",
                                }
                            }
                        }
                    },
                    "clusters":{
                        "type":"array",
                        "title":"集群列表",
                        "items":{
                            "type":"object",
                            "properties":{
                                "quota":{
                                    "type":"number",
                                    "title":"配额",
                                    "x-component":"Input",
                                },
                                "location":{
                                    "type":"string",
                                    "title":"路径",
                                    "x-component":"Input",
                                }
                            }
                        }
                    }
                }
            }
        }
    };
let json={
    "COMMON": {
        "name": "bigdata_sre",
        "type":"b",
        "region":{
            "AY36A": {
                "network": 'ipv4',
                "area": "cn"
            },
            "AY38A": {
                "network": 'ipv6',
                "area": "au"
            },
        },
        "clusters":[
            {
                quota:10,
                location:"a/b/v"
            },
            {
                quota:20,
                location:"aa/ab/av"
            }
        ]
    }
};
*/

const formItemLayout = {
    labelCol: {
        xs: { span: 8 },
        sm: { span: 6 },
        md: { span: 4 },
    },
    wrapperCol: {
        xs: { span: 16 },
        sm: { span: 12 },
        md: { span: 10 },
    },
};

function genItem(name, schema, json, path) {
    let { type, title, properties } = schema;
    if (type === 'object' && properties) {
        let genPath = (path + (path ? "/" : "") + name);
        let children = Object.keys(properties).map(propName => {
            return genItem(propName, properties[propName], json, genPath);
        });
        return { title, children }
    }
    if (type === 'array') {
        return getArrayItem(name, schema, path, json)
    }
    if (type === 'string' || type === 'number') {
        return getAtomItem(name, schema, path, json)
    }
    if (type === 'x-array') {
        return getClassItem(name, schema, path, json)
    }
}

function getAtomItem(name, props, path, json) {
    //json key存在含有"."的情况,"."在form的name中又有特殊的用意，因此需要进行替换,最后再替换回来
    let uiType = props["x-component"], itemName = `${path.replace(/\//g, ".")}.${name.replace(/\./g, "~")}`, visibleExp = props["x-visibleExp"], validateType = props["x-validateType"];
    let { description, title, required, pattern, enableScroll, initValue } = props;
    let initValueEdit = _.get(json, itemName) === undefined ? initValue : _.get(json, itemName);
    let commonProps = {
        name: itemName, required: required, initValue: initValueEdit, label: title, validateReg: pattern, visibleExp: visibleExp,
        tooltip: description,
        validateType: validateType,

    };
    if (uiType === 'Input') {
        return { type: 1, inputTip: description, ...commonProps }
    } else if (uiType === 'Text') {
        return { type: 2, inputTip: description, ...commonProps }
    } else if (selectType.includes(uiType)) {
        return { type: typeMapping[uiType], optionValues: props["x-component-props"].options, ...commonProps }
    } else if (uiType === 'EditTable') {
        if (typeof initValueEdit === 'string') {
            return { type: 1, inputTip: description, ...commonProps }
        }
        let { columns = [] } = props["x-component-props"];
        let tableDefModel = {
            "pagination": false,
            "enableEdit": true,
            "enableRemove": true,
            "enableAdd": true,
            "columns": columns,
            "enableScroll": enableScroll,
        };
        return { type: typeMapping[uiType], ...commonProps, defModel: tableDefModel }
    } else if (uiType === 'JSON') {
        let componentProps = Object.assign({ defModel: { mode: "json", height: 160, disableShowDiff: true } }, props["x-component-props"]);
        return { type: typeMapping[uiType], ...componentProps, ...commonProps }
    } else {
        let componentProps = props["x-component-props"] || {};
        return { type: typeMapping[uiType], ...componentProps, ...commonProps }
    }
}

function getArrayItem(name, props, path, json) {
    let itemName = `${path.replace(/\//g, ".")}.${name.replace(/\./g, "~")}`;
    let arrayData = _.get(json, itemName, []), items = props.items;
    let arrayItems = arrayData.map((item, index) => {
        return genItem(`${index}`, items, json, (path + "/" + name))
    });
    return { arrayTitle: props.title, children: arrayItems }


}

function getClassItem(name, props, path, json) {
    let itemName = `${path.replace(/\//g, ".")}.${name.replace(/\./g, "~")}`;
    let instanceData = _.get(json, itemName, {});
    let children = Object.keys(instanceData).map((key, index) => {
        return genItem(`${key}`, Object.assign({}, props.properties, { title: key }), json, (path + "/" + name))
    });
    return { title: props.title, children }
}



export default class JSONSchemaItem extends React.Component {

    constructor(props) {
        super(props);
        let { model, onChange } = this.props;
        let initJson = {}, { schema } = model.defModel;
        if (model.initValue) {
            if (typeof model.initValue === 'string') {
                initJson = JSON.parse(model.initValue)
            } else {
                initJson = model.initValue;
            }
        }
        this.initJson = initJson;
        this.newJson = initJson;
        onChange && onChange(initJson);
        this.formItemTree = genItem('', schema, this.initJson, '');
        //this.formChildrens=this.genFormItem(this.formItemTree);
        this.state = {
            editMode: 'base',
        };
    }

    genFormItem = (itemNode) => {
        if (!itemNode) return null;
        let { title, children, arrayTitle } = itemNode, { form, model, layout } = this.props;
        //console.log("`${model.name}.${itemNode.name}`----->",`${model.name}.${itemNode.name}`,itemNode);
        let { visibleExp } = itemNode;
        if (visibleExp) {
            let valuesObject = form.getFieldsValue()[model.name];
            let isVisable = safeEval(visibleExp, valuesObject);
            if (!isVisable) return null;
        }
        //存在分类嵌套
        if (title && children && children.length) {
            return (
                <Collapse key={title} style={{ marginBottom: 8 }} defaultActiveKey={[title]}>
                    <Panel header={title} key={title}>
                        {
                            children.map(child => {
                                return this.genFormItem(child)
                            })
                        }
                    </Panel>
                </Collapse>
            )
        }
        if (arrayTitle && children) {
            //数组用ListCard展示
            if (Array.isArray(children) && children.length) {
                return (
                    <List
                        header={<div>{arrayTitle}</div>}
                        key={children.length}
                        size="small"
                        bordered
                        dataSource={children}
                        style={{ marginBottom: 8 }}
                        renderItem={(item, index) => (
                            <List.Item key={index}>
                                {this.genFormItem(item)}
                            </List.Item>
                        )}
                    />
                )
            } else {
                return FormElementFactory.createFormItem(Object.assign({}, itemNode, { name: `${model.name}.${itemNode.name}` }), form, layout || formItemLayout);
            }
        }
        if (children && itemNode.hasOwnProperty("title")) {
            return children.map(child => {
                return this.genFormItem(child);
            })
        }
        return FormElementFactory.createFormItem(Object.assign({}, itemNode, { name: `${model.name}.${itemNode.name}` }), form, layout || formItemLayout);
    };

    handleModeChanged = (editMode) => {
        this.setState({ editMode });
    };

    render() {
        let { editMode } = this.state;
        let { model, form, layout } = this.props;
        const { getFieldDecorator } = form;
        //form模式只返回表单不返回编辑json模式
        if (model.mode === "form") {
            return this.genFormItem(this.formItemTree)
        }
        return (
            <div>
                <FormItem
                    {...layout}
                    label={model.label}
                    key={model.name}
                >
                    <Tabs activeKey={editMode} size="small" onChange={this.handleModeChanged}>
                        <TabPane tab={<span><FormOutlined />普通模式</span>} key="base">
                            {editMode === 'base' && this.genFormItem(this.formItemTree)}
                        </TabPane>
                        <TabPane tab={<span><CodeOutlined />高级模式</span>} key="advanced">
                            {editMode === 'advanced' &&
                                <FormItem
                                    label={""}
                                    key={model.name}
                                >
                                    {getFieldDecorator(model.name, {
                                        initialValue: model.initValue,
                                        rules: [],
                                    })(
                                        <JSONEditor key={model.name} model={model} />
                                    )}
                                </FormItem>
                            }
                        </TabPane>
                    </Tabs>
                </FormItem>
            </div>
        );
    }
}