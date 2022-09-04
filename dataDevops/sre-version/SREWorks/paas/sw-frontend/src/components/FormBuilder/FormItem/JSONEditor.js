/**
 * Created by caoshuaibiao on 2019/3/26.
 * json编辑器Item 支持json编辑前后的diff
 */

import React from 'react';
import { Spin, Button, Card, Modal, Tooltip, List, Row, Col, Divider, message } from 'antd';
import localeHelper from '../../../utils/localeHelper';
import JsonEditor from '../../JsonEditor';
import _ from 'lodash';
import AceDiff from 'ace-diff';
import 'ace-diff/dist/ace-diff.min.css';
 
// optionally, include CSS, or use your own
// import 'ace-diff/dist/ace-diff.min.css';
// // Or use the dark mode
// import 'ace-diff/dist/ace-diff-dark.min.css';


export default class JSONEditor extends React.Component {

    constructor(props) {
        super(props);
        let { model, onChange } = this.props;
        let initJson = {};
        if (model.initValue) {
            if (typeof model.initValue === 'string') {
                initJson = JSON.parse(model.initValue)
            } else if (typeof model.initValue === 'object' || Array.isArray(model.initValue)) {
                initJson = model.initValue;
            }
        } else if (model.defModel && typeof model.defModel === 'string') {
            initJson = JSON.parse(model.defModel)
        }
        this.initJson = initJson;
        this.newJson = initJson;
        this.state={
            diffVisible: false
        }
        onChange && onChange(initJson);
    }

    editorChange = (json) => {
        this.newJson = json;
        let { onChange } = this.props;
        onChange && onChange(json);
    };

    jsonEditorInitialized = (jsonEditor) => {
        this.jsonEditor = jsonEditor;
    };

    showDiff = () => {
        let oldJson = _.isString(this.initJson) ? JSON.parse(this.initJson) : this.initJson;
        let newJson = _.isString(this.newJson) ? JSON.parse(this.newJson) : this.newJson;
        this.setState({
            diffVisible:true
        },()=> {
            const differ = new AceDiff({
                ace: window.ace,
                element: '.acediff',
                showDiffs: true,
                showConnectors: true,
                diffGranularity: 'broad',
                left: {
                  content: JSON.stringify(oldJson),
                },
                right: {
                  content: JSON.stringify(newJson),
                },
                classes: {
                    gutterID: 'acediff__gutter',
                    diff: 'acediff__diffLine',
                    connector: 'acediff__connector',
                    newCodeConnectorLink: 'acediff__newCodeConnector',
                    newCodeConnectorLinkContent: '&#8594;',
                    deletedCodeConnectorLink: 'acediff__deletedCodeConnector',
                    deletedCodeConnectorLinkContent: '&#8592;',
                    copyRightContainer: 'acediff__copy--right',
                    copyLeftContainer: 'acediff__copy--left',
                  },
              });
        })
    };

    render() {
        let {diffVisible} = this.state;
        let options = {
            modes: ['code', 'tree']
        };
        let { model } = this.props;
        let { defModel = {} } = model;
        return (
            <div>
                <JsonEditor json={this.initJson} mode="code" readOnly={false} options={options} onChange={this.editorChange} changeInterval={500} initialized={this.jsonEditorInitialized} defaultExpand={true} style={{ height: model.height || '80vh' }} />
                {model.showDiff !== false && !defModel.disableShowDiff && <a onClick={this.showDiff}>{localeHelper.get('formItem.versionComparison', '新旧版本对比')}</a>}
                <Modal bodyStyle={{height:400,overflowY:'scroll',width:"100%"}} title="编辑对比" width="80%" onCancel={()=> this.setState({diffVisible: false})} onOk={()=> this.setState({diffVisible: false})} visible={diffVisible}>
                    <div style={{height:300,width:"100%"}} class="acediff"></div>
                </Modal>
            </div>
        )
    }
}



