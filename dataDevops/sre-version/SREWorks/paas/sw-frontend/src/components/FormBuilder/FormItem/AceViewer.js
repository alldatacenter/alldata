import React, { Component } from 'react';

import brace from 'brace';
import AceEditor from 'react-ace';

import 'brace/mode/yaml';
import 'brace/mode/javascript';
import 'brace/mode/python';
import "brace/mode/jsx";
import "brace/mode/json";

import 'brace/theme/monokai';
import 'brace/theme/xcode';

import { Modal, Message } from 'antd';
import jsYaml from "js-yaml";
import localeHelper from '../../../utils/localeHelper';
import _ from "lodash";

class AceViewer extends Component {
    constructor(props) {
        super(props);
        this.themeType = localStorage.getItem('sreworks-theme');
        let { value, onChange, model, mode, target } = props;
        this.mode = model && (mode || (model.defModel && model.defModel.mode));
        this.target = model && (target || (model.defModel && model.defModel.target));
        try {
            if (typeof value === 'object') {
                if (this.mode === 'json') {
                    this.initContent = JSON.stringify(value, null, 2);
                    onChange && onChange(value);
                } else if (this.mode === 'yaml') {
                    this.initContent = jsYaml.dump(value, { lineWidth: 100000 });
                    if (this.target === 'json') {
                        onChange && onChange(value);
                    } else {
                        onChange && onChange(this.initContent);
                    }
                } else {
                    this.initContent = JSON.stringify(value, null, 2);
                    onChange && onChange(this.initContent);
                }
            } else if (typeof value === 'string') {
                if (this.mode === 'yaml') {
                    this.initContent = jsYaml.load(value);
                    if (this.target === 'json') {
                        onChange && onChange(this.initContent);
                    } else {
                        onChange && onChange(value);
                    }
                    this.initContent = jsYaml.dump(this.initContent, { lineWidth: 100000 });
                } else if (this.mode === 'json') {
                    this.initContent = value;
                    onChange && onChange(JSON.parse(value));
                } else {
                    this.initContent = value;
                    onChange && onChange(this.initContent);
                }
            }
        } catch (error) {
            Message.error(error);
        }
        this.state = {
            editorValue: this.initContent
        }
    }

    editorChange = (content) => {
        this.newContent = content;
        let { onChange, readOnly } = this.props;
        if (readOnly) {
            return;
        }
        this.setState({
            editorValue: content
        });
        if (this.mode === 'json') {
            try {
                let json = JSON.parse(content);
                if (json && onChange && (content.indexOf('{') !== -1) && (content.indexOf('}') !== -1)) {
                    onChange && onChange(json);
                }
            } catch (e) {

            }
        } else if (this.target === 'json' && this.mode === 'yaml') {
            try {
                if (content.length > 6) {
                    onChange && onChange(jsYaml.load(content));
                }
            } catch (e) {

            }
        } else {
            onChange && onChange(content);
        }
    };


    showDiff = () => {
        Modal.info({
            content: (
                <div>
                    需要增加diff组件
                </div>
            ),
            width: '80%',
            onOk() { },
        });
    };

    render() {
        let { model, onChange, value, mode, readOnly } = this.props, { editorValue } = this.state;
        let aceProps = { mode: mode || "python", theme: this.themeType === 'light' ? "xcode" : "monokai" };
        if (model && model.defModel && !_.isEmpty(model.defModel)) {
            Object.assign(aceProps, model.defModel);
        }
        return (
            <div>
                <AceEditor
                    name={model.name}
                    readOnly={readOnly}
                    value={editorValue}
                    editorProps={{ $blockScrolling: true }}
                    wrapEnabled={true}
                    width={'100%'}
                    showPrintMargin={true}
                    showGutter={true}
                    highlightActiveLine={true}
                    onChange={this.editorChange}
                    debounceChangePeriod={2000}
                    setOptions={{
                        enableBasicAutocompletion: true,
                        enableLiveAutocompletion: false,
                        autoScrollEditorIntoView: true,
                        enableSnippets: true,
                        showLineNumbers: true,
                        tabSize: 2
                    }}
                    {...aceProps}
                />
                {/*增加配置新旧版本入口*/}
                {/* {!aceProps.disableShowDiff &&
                <a onClick={this.showDiff}>{localeHelper.get("formItem.versionComparison", "新旧版本对比")}</a>} */}
            </div>
        )
    }
}

export default AceViewer;