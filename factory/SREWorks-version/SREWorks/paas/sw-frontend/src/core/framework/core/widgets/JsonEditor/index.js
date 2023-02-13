import React, { Component } from 'react';

import brace from 'brace';
import AceEditor from 'react-ace';

import 'brace/mode/yaml';
import 'brace/mode/javascript';
import 'brace/mode/python';
import "brace/mode/jsx";
import "brace/mode/json";
import "brace/mode/java";
import "brace/mode/text"

import 'brace/theme/monokai';
import 'brace/theme/xcode';

import { Modal, Message } from 'antd';
import jsYaml from "js-yaml";
import _ from "lodash";

class JsonEditor extends Component {
    constructor(props) {
        super(props);
        this.themeType = localStorage.getItem('sreworks-theme');
        let { widgetData = "", onChange, model, mode, target } = props;
        this.mode = model && (mode || (model.defModel && model.defModel.mode));
        try {
            if (typeof widgetData === 'object') {
                if (this.mode === 'json') {
                    this.initContent = JSON.stringify(widgetData, null, 2);
                } else if (this.mode === 'yaml') {
                    this.initContent = jsYaml.dump(widgetData, { lineWidth: 100000 });
                } else {
                    this.initContent = JSON.stringify(widgetData, null, 2);
                }
            } else if (typeof widgetData === 'string') {
                if (this.mode === 'yaml') {
                    this.initContent = jsYaml.load(widgetData);
                    this.initContent = jsYaml.dump(this.initContent, { lineWidth: 100000 });
                } else if (this.mode === 'json') {
                    this.initContent = widgetData;
                } else {
                    this.initContent = widgetData;
                }
            }
        } catch (error) {
            Message.error(error);
        }
        this.oldContent = this.initContent;
        this.state = {
            editorValue: this.initContent
        }
    }

    render() {
        let { model, widgetConfig } = this.props, { editorValue } = this.state;
        let aceProps = { mode: widgetConfig.langType || "python", theme: this.themeType === 'light' ? "xcode" : "monokai" };
        let { readOnly = true, heightNum = undefined } = this.props.widgetConfig;
        if (model && model.defModel && !_.isEmpty(model.defModel)) {
            Object.assign(aceProps, model.defModel);
        }
        if (typeof (Number(heightNum)) === 'number') {
            heightNum = heightNum + 'px'
        } else {
            heightNum = undefined;
        }
        return (
            <div>
                <AceEditor
                    name={model && model.name}
                    readOnly={readOnly}
                    value={editorValue}
                    editorProps={{ $blockScrolling: true }}
                    wrapEnabled={true}
                    width={'100%'}
                    height={heightNum}
                    showPrintMargin={true}
                    showGutter={true}
                    highlightActiveLine={true}
                    debounceChangePeriod={500}
                    setOptions={{
                        enableBasicAutocompletion: true,
                        enableLiveAutocompletion: false,
                        enableSnippets: true,
                        showLineNumbers: true,
                        tabSize: 2
                    }}
                    {...aceProps}
                />
            </div>
        )
    }
}

export default JsonEditor;