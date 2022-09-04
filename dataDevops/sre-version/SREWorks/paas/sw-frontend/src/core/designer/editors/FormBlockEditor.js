/**
 * Created by caoshuaibiao on 2021/2/3.
 * 表单编辑器
 */

import React, { PureComponent } from 'react';
import { CloseOutlined, DownOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import {
    message,
    Button,
    Card,
    Tooltip,
    Popover,
    Input,
    Row,
    Col,
    Menu,
    Spin,
    Collapse,
    Select,
    Divider,
    Tabs,
    Drawer,
    Dropdown,
} from 'antd';
import FormEditor from '../../../components/FormBuilder/FormEditor';
import SuperForm from '../../../components/FormBuilder/SuperForm';
import JsonEditor from '../../../components/JsonEditor';
import FormElementType from '../../../components/FormBuilder/FormElementType';
import FormElementFactory from '../../../components/FormBuilder/FormElementFactory';

const { TabPane } = Tabs;

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

class FormSetting extends React.Component {

    constructor(props) {
        super(props);
        let { config } = props;
        let paramsDef = [], { label, name } = config;
        paramsDef.push({ type: FormElementType.INPUT, name: 'name', initValue: name, required: true, label: "表单标识", inputTip: "表单的业务标识,用于其他地方引用" });
        paramsDef.push({ type: FormElementType.INPUT, name: 'label', initValue: label, required: true, label: "表单名称", inputTip: "表单显示的名称" });
        this.itemDef = paramsDef;
    }


    buildingFormElements = () => {
        let { form } = this.props;
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, formItemLayout));
        return (
            <Row style={{ marginTop: 12 }}>
                <Col span={12}>
                    {
                        [

                            formChildrens[0],
                            formChildrens[1],
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
const FormSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(changedValues, allValues)
})(FormSetting);

export default class FormBlockEditor extends PureComponent {

    constructor(props) {
        super(props);
        let { editorData } = props;
        let parameterDefiner = editorData.parameterDefiner || { bindingParamTree: [], paramMapping: [] };
        parameterDefiner.extConfig = {
            tabTitle: "表单设计",
            renderTabExt: this.renderTabExt(),
            showCard: false,
            colCount: 3,

            layout: {
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
            }
        };
        this.state = {
            parameterDefiner: parameterDefiner,
            reloadCount: 0,
            openDrawer: false
        };
        this.formConfig = { label: editorData.label, name: editorData.name || editorData.id };
    }

    componentDidMount() {

    }

    onPreview = () => {
        if (!this.editor) return;
        this.setState({
            parameterDefiner: this.editor.getParameterDefiner(),
            reloadCount: ++this.state.reloadCount
        });
    };

    showJson = () => {
        this.setState({
            showJson: this.getJsonData(),
        });
    };

    renderTabExt = () => {
        return (
            <div style={{ display: 'flex', justifyContent: "space-between" }}>
                <div>
                    <a onClick={this.onClose}>
                        <CloseOutlined />
                    </a>
                </div>
            </div>
        );
    };


    getJsonData = () => {
        let { editorData } = this.props;
        if (!this.editor) {
            return {
                ...editorData,
                ...this.formConfig
            };
        }
        let parameterDefiner = this.editor.getParameterDefiner();
        let { extConfig, ...parameterDefinerJson } = parameterDefiner;
        return {
            ...editorData,
            ...this.formConfig,
            parameterDefiner: parameterDefinerJson
        };
    };


    saveForm = () => {
        let { onSave } = this.props;
        let formJson = this.getJsonData();
        onSave && onSave(formJson);
    };

    onClose = () => {
        this.setState({
            openDrawer: false,
        });
    };


    onDelete = () => {
        let { onDelete, editorData } = this.props;
        onDelete && onDelete(editorData)
    };

    openDrawer = () => {
        this.setState({
            openDrawer: true,
            showJson: false
        });
    };

    render() {
        let { parameterDefiner, reloadCount, openDrawer, showJson } = this.state, { editorData } = this.props;

        return (
            <div className={"globalBackground page_small_tabs abm-frontend-designer-page-editor"}>
                <Tabs defaultActiveKey="base" size="small"
                    tabBarExtraContent={
                        <div className="feature-bar">
                            <Button size="small" className="feature-button" onClick={this.showJson}>源码</Button>
                            <Button size="small" onClick={this.onPreview} className="feature-button">
                                预览
                            </Button>
                            <Button size="small" type="primary" className="feature-button" onClick={this.saveForm}>保存</Button>
                            <Dropdown overlay={
                                <Menu>
                                    <Menu.Item><a onClick={this.onDelete}>删除</a></Menu.Item>
                                </Menu>
                            }>
                                <Button size="small" className="feature-button">
                                    更多 <DownOutlined />
                                </Button>
                            </Dropdown>
                        </div>
                    }
                >
                    <TabPane key="base" tab={<span>表单定义</span>}>
                        <Card size="small" style={{ margin: "2vh 10vw" }}
                            className="abm_frontend_widget_component_wrapper"
                            bodyStyle={{ overflow: "auto", height: "85vh" }}
                            title={
                                <div className="card-title-wrapper">
                                    {/*<div className="card-wrapper-title-prefix" />*/}
                                    <div>
                                        <b style={{ marginLeft: 12, fontSize: 14 }}>{this.label || editorData.label}</b>
                                    </div>
                                </div>
                            }
                            extra={<a onClick={this.openDrawer}>配置</a>
                            }
                        >
                            {
                                parameterDefiner &&
                                <SuperForm parameterDefiner={parameterDefiner}
                                    key={reloadCount}
                                    ref={r => this.superForm = r}
                                    formItemLayout={
                                        {
                                            labelCol: {
                                                xs: { span: 24 },
                                                sm: { span: 24 },
                                                md: { span: 6 },
                                            },
                                            wrapperCol: {
                                                xs: { span: 24 },
                                                sm: { span: 24 },
                                                md: { span: 12 },
                                            },
                                        }
                                    }
                                />
                            }
                        </Card>
                    </TabPane>
                    <TabPane key="props" tab={<span>表单属性</span>}>
                        <FormSettingForm config={this.formConfig} onValuesChange={(changedField, allValue) => this.formConfig = allValue} />
                    </TabPane>
                </Tabs>
                <Drawer
                    placement={"bottom"}
                    height={'50vh'}
                    onClose={this.onClose}
                    closable={false}
                    maskClosable={false}
                    destroyOnClose={false}
                    mask={false}
                    bodyStyle={{ top: 0, bottom: 0, padding: "0px" }}
                    visible={openDrawer}
                >
                    <div>
                        <FormEditor parameterDefiner={parameterDefiner} onReady={(editor) => this.editor = editor} />
                    </div>
                </Drawer>
                <Drawer
                    placement={"right"}
                    width={'50vw'}
                    onClose={() => this.setState({ showJson: false })}
                    closable={true}
                    maskClosable={true}
                    destroyOnClose={true}
                    title={"源JSON"}
                    mask={true}
                    visible={showJson}
                >
                    <div>
                        <JsonEditor json={showJson} mode="code" readOnly={true} style={{ height: '82vh' }} />
                    </div>
                </Drawer>
            </div>
        );
    }
}