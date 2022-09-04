import React, { Component } from "react";
import { EditOutlined, MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Tabs, Select, Button, Row, Col, Collapse, Input } from "antd";
import AceEditor from "react-ace";
import _ from "lodash";
import uuid from "uuid/v4";
import brace from 'brace';
import "brace/mode/json";
import 'brace/mode/javascript';
import 'brace/theme/monokai';
import 'brace/theme/xcode';

const Panel = Collapse.Panel;
const { TabPane } = Tabs;
const { Option } = Select;
const formItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 4 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 16 },
        md: { span: 16 },
    },
};
let themeType = localStorage.getItem('sreworks-theme');
export default class SearchParamsEditor extends Component {
    constructor(props) {
        super(props);
        // this.value = "[\n  {\n    \"id\": \"xxxxxxx\",\n    \"type\": \"api\",\n    \"names\": [\n      \"appId\",\n      \"appName\"\n    ],\n    \"dependencies\": [],\n    \"config\": {\n      \"url\": \"http://prod-app-app.sreworks.svc.cluster.local:80/appdev/app/listAll\",\n      \"method\": \"GET\",\n      \"contentType\": \"\",\n      \"headers\": {},\n      \"params\": {},\n      \"body\": \"\",\n      \"fileds\": [\n        {\n          \"field\": \"$.data[*].appId\",\n          \"type\": \"String\",\n          \"alias\": \"appId\"\n        },\n        {\n          \"field\": \"$.data[*].appName\",\n          \"type\": \"Auto\",\n          \"alias\": \"appName\"\n        }\n      ]\n    }\n  }\n]"
        this.value = '[]';
        if (this.props.value && this.props.value.length > 20) {
            this.value = this.props.value
        }
    }
    editorChange = (finalValue) => {
        let formateValue = [];
        this.value = finalValue;
        if (finalValue) {
            try {
                formateValue = finalValue
                this.props.onValuesChange && this.props.onValuesChange(formateValue)
            } catch (error) {
            }
        }
    }
    render() {
        // let value = '[]';
        // if (this.props.value && this.props.value.length > 20) {
        //     this.value = this.props.value
        // }
        return <div className="card-tab-panel">
            <AceEditor
                mode="json"
                style={{ minHeight: 400, width: 700 }}
                fontSize={12}
                theme={themeType === 'light' ? "xcode" : "monokai"}
                showPrintMargin={true}
                showGutter={true}
                defaultValue={this.value}
                value={this.value}
                onChange={this.editorChange}
                highlightActiveLine={true}
                setOptions={{
                    enableBasicAutocompletion: false,
                    enableLiveAutocompletion: false,
                    enableSnippets: false,
                    showLineNumbers: true,
                    tabSize: 2,
                }}
            />
        </div>;
    }
}