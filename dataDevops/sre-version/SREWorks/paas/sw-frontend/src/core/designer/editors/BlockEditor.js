/**
 * Created by caoshuaibiao on 2021/2/5.
 */
import React from 'react';

import {
    BuildOutlined,
    CodeOutlined,
    DatabaseOutlined,
    DownOutlined,
    ProfileOutlined,
    SettingOutlined,
} from '@ant-design/icons';

import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';

import {
    Layout,
    Menu,
    Dropdown,
    Tree,
    Steps,
    Button,
    Card,
    Radio,
    List,
    Tabs,
    Drawer,
    Input,
    Tooltip,
    Col,
    Row,
} from 'antd';
import PageModel from '../../framework/model/PageModel';
import DataSourceEditor from "./DataSourceEditor";
import Constants from '../../framework/model/Constants';
import PageContent from "../../framework/core/PageContent";
import JsonEditor from '../../../components/JsonEditor';
import FormElementType from '../../../components/FormBuilder/FormElementType';
import FormElementFactory from '../../../components/FormBuilder/FormElementFactory';
import '../workbench/index.less';
import './index.less';
import ContentLayout from "../../framework/components/ContentLayout";
import PopoverConfirm from '../../../components/PopoverConfirm';
import AceViewer from '../../../components/FormBuilder/FormItem/AceViewer';
import FluidContentLayoutDesigner from "../../framework/components/FluidContentLayoutDesigner";

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
const ACTION = Constants.BLOCK_CATEGORY_ACTION;
const PAGE = Constants.BLOCK_CATEGORY_PAGE;
const FILTER = Constants.BLOCK_CATEGORY_FILTER;
const FORM = Constants.BLOCK_CATEGORY_FORM;
const CHART = Constants.BLOCK_CATEGORY_CHART;

class BlockSetting extends React.Component {

    constructor(props) {
        super(props);
        let { config } = props;
        let paramsDef = [], { label, name, category, tags = "private" } = config;
        paramsDef.push({
            type: FormElementType.RADIO, name: 'category', initValue: category, required: true, label: "区块类型",
            optionValues: [
                { value: ACTION, label: '操作' },
                { value: FILTER, label: '过滤器' },
                { value: PAGE, label: '页面' },
                { value: FORM, label: '步骤表单' },
                { value: CHART, label: '图表' }
            ]
        });
        paramsDef.push({
            type: FormElementType.RADIO, name: 'tags', initValue: tags, required: true, label: "区块范围",
            optionValues: [
                { value: "private", label: '私有' },
                { value: "public", label: '公开' },
            ]
        });
        paramsDef.push({ type: FormElementType.INPUT, name: 'name', initValue: name, required: true, label: "区块标识", disabled: true, inputTip: "区块的业务标识,用于其他地方引用" },);
        paramsDef.push({ type: FormElementType.INPUT, name: 'label', initValue: label, required: true, label: "区块标题", inputTip: "显示的名称" });
        this.itemDef = paramsDef;
    }


    buildingFormElements = () => {
        let { form } = this.props;
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, formItemLayout));
        return (
            <div>
                {
                    formChildrens
                }
            </div>

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
const BlockSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(changedValues, allValues)
})(BlockSetting);

export const BlockPropsSettingForm = BlockSettingForm;

export default class BlockEditor extends React.Component {

    constructor(props) {
        super(props);
        let { editorData = { config: {} }, nodeModel } = props, pageModel;
        if (editorData) {
            pageModel = new PageModel(editorData);
        } else {
            pageModel = PageModel.CREATE_DEFAULT_INSTANCE();
        }
        pageModel.setNodeModel(nodeModel);
        this.state = {
            pageModel: pageModel,
            showPreview: false,
            openDrawer: false,
            current: 0,
            activeKey: "dev",
            mode: pageModel.mode,
        };
        let { category } = pageModel;
        this.blockConfig = { label: pageModel.label, name: pageModel.name || pageModel.id, category: pageModel.category, tags: pageModel.tags };
        this.include = [], this.exclude = [], this.filterType = [];
        if (category === Constants.BLOCK_CATEGORY_ACTION) {
            this.include.push("action");
        } else if (category === Constants.BLOCK_CATEGORY_FILTER) {
            this.include.push("filter");
        } else if (category === Constants.BLOCK_CATEGORY_PAGE) {
            this.exclude = ["block"];
        } else if (category === Constants.BLOCK_CATEGORY_FORM) {
            this.include.push("action");
            this.filterType.push("STEP_FORM");
        } else if (category === Constants.BLOCK_CATEGORY_CHART) {
            this.include.push("charts");
        }
    }

    addElement = (eType) => {

    };

    removeElement = (eModel) => {

    };

    handleChange = res => {
        console.log(res);
    };



    handleSave = () => {
        let { pageModel } = this.state;
        let { onSave, editorData } = this.props, pageJson = pageModel.toJSON();
        //console.log("block save to json---->",pageModel.toJSON());
        let blockItem = {
            ...editorData,
            ...pageJson,
            ...this.blockConfig

        };
        onSave && onSave(blockItem);
    };

    handlePreview = () => {
        this.setState({
            showPreview: true,
            openDrawer: true,
            showJson: false
        });
    };

    showJson = () => {
        this.setState({
            showJson: true,
            showPreview: false,
            openDrawer: true
        });
    };

    onClose = () => {
        this.setState({
            openDrawer: false
        });
    };

    onDelete = () => {
        let { onDelete, editorData } = this.props;
        onDelete && onDelete(editorData)
    };

    handleToolClick = (toolType, payload) => {
        let { pageModel } = this.state;
        pageModel.getRootWidgetModel().events.emit(toolType, payload);
    };

    handleChangeTab = res => {
        this.setState({ activeKey: res })
    }

    render() {
        let { pageModel, showPreview, openDrawer, showJson, activeKey } = this.state, { editorData, height = 620, nodeData, ...otherProps } = this.props;
        let tabEditorContentStyle = { height: height - 124, overflowY: "auto", overflowX: "none" }, { config } = nodeData;
        let { pageLayoutType = Constants.PAGE_LAYOUT_TYPE_CUSTOM } = config, containerModel = pageModel.getRootWidgetModel();
        return (
            <div className={"globalBackground page_small_tabs abm-frontend-designer-page-editor"}>
                <Tabs onChange={this.handleChangeTab} activeKey={activeKey} size="small" tabBarExtraContent={
                    <div className="feature-bar">
                        {
                            activeKey === "dev" && pageLayoutType === Constants.PAGE_LAYOUT_TYPE_CUSTOM &&
                            <Button className="feature-button" size="small" style={{ marginLeft: 6 }} onClick={() => this.handleToolClick(Constants.TOOL_TYPE_ADD)} >
                                <div className="wrapper">
                                    <svg t="1619012520900" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="36244" width="16" height="16">
                                        <path className="path" d="M138.24 158.72v706.56c0 11.264 9.216 20.48 20.48 20.48h299.52v-0.512c10.24-1.024 17.92-9.728 17.92-19.968s-7.68-18.944-17.92-19.968v-0.512H199.68c-11.264 0-20.48-9.216-20.48-20.48V199.68c0-11.264 9.216-20.48 20.48-20.48h624.64c11.264 0 20.48 9.216 20.48 20.48v258.56h0.512c1.024 10.24 9.728 17.92 19.968 17.92s18.944-7.68 19.968-17.92h0.512V158.72c0-11.264-9.216-20.48-20.48-20.48H158.72c-11.264 0-20.48 9.216-20.48 20.48z" p-id="36245"></path>
                                        <path className="path" d="M701.44 307.2H322.56c-11.264 0-20.48-9.216-20.48-20.48s9.216-20.48 20.48-20.48h378.88c11.264 0 20.48 9.216 20.48 20.48s-9.216 20.48-20.48 20.48zM865.792 706.048H506.368c-10.752 0-19.968-8.704-19.968-19.968 0-10.752 8.704-19.968 19.968-19.968h359.936c10.752 0 19.968 8.704 19.968 19.968-0.512 10.752-9.216 19.968-20.48 19.968z" p-id="36246"></path>
                                        <path className="path" d="M666.112 865.792V506.368c0-10.752 8.704-19.968 19.968-19.968 10.752 0 19.968 8.704 19.968 19.968v359.936c0 10.752-8.704 19.968-19.968 19.968-10.752-0.512-19.968-9.216-19.968-20.48z" p-id="36247">
                                        </path>
                                    </svg>
                                    <span className="text">添加</span>
                                </div>
                            </Button>
                        }
                        {/*<Button size="small"  className="feature-button" onClick={this.showJson}>源JSON</Button>*/}
                        <Button size="small" className="feature-button" onClick={this.handlePreview}>
                            <div className="wrapper">
                                <svg viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="31824" width="16" height="16">
                                    <path className="path" d="M886.26 138.29H169.54A60.62 60.62 0 0 0 109 198.84V669.6a60.62 60.62 0 0 0 60.55 60.55h333.62V837.6H136.28v27.29h783.24V837.6H557.75V730.15h328.51a60.62 60.62 0 0 0 60.55-60.55V198.84a60.62 60.62 0 0 0-60.55-60.55z m6 531.31a5.91 5.91 0 0 1-6 6H169.54a5.91 5.91 0 0 1-6-6V198.84a5.91 5.91 0 0 1 6-6h716.72a5.91 5.91 0 0 1 6 6z" p-id="31825"></path>
                                    <path className="path" d="M659.75 416.26L481.62 313.38a21 21 0 0 0-20.92 0.1 20.68 20.68 0 0 0-10.41 18.06v205.53a20.68 20.68 0 0 0 10.41 18.06 21 21 0 0 0 21 0.06l178-102.84a20.85 20.85 0 0 0 0-36.1zM477.58 526V342.56l158.84 91.74z" p-id="31826" >
                                    </path>
                                </svg>
                                <span className="text"> 预览</span>
                            </div>
                        </Button>
                        <Button size="small" type="primary" className="feature-button" onClick={this.handleSave}>
                            <div className="wrapper">
                                <svg className="save-icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="37884" width="16" height="16">
                                    <path d="M954.083556 267.434667L702.549333 63.331556A28.444444 28.444444 0 0 0 684.515556 56.888889h-483.555556C87.921778 56.888889 56.888889 91.648 56.888889 199.964444v615.096889C56.888889 925.895111 92.103111 967.111111 200.96 967.111111h625.777778C941.624889 967.111111 967.111111 918.087111 967.111111 815.061333V289.436444c0-8.519111-6.456889-16.597333-13.027555-22.001777zM725.333333 154.652444v196.053334C725.333333 384 720.071111 384 695.921778 384H331.818667C298.183111 384 298.666667 376.903111 298.666667 350.705778V113.777778h375.68L725.333333 154.652444z m184.888889 660.408889C910.222222 902.229333 898.375111 910.222222 826.737778 910.222222h-625.777778C123.192889 910.222222 113.777778 895.118222 113.777778 815.089778V199.964444C113.777778 124.245333 119.751111 113.777778 200.96 113.777778H270.222222v236.928C270.222222 404.977778 297.102222 412.444444 331.818667 412.444444h364.103111C746.069333 412.444444 753.777778 390.144 753.777778 350.705778V177.464889l156.444444 125.425778v512.170666z" p-id="37885"></path><path d="M611.555556 298.666667a28.444444 28.444444 0 0 0 28.444444-28.444445v-85.333333a28.444444 28.444444 0 0 0-56.888889 0v85.333333a28.444444 28.444444 0 0 0 28.444445 28.444445zM293.404444 611.555556h113.777778a14.222222 14.222222 0 1 0 0-28.444445h-113.777778a14.222222 14.222222 0 1 0 0 28.444445zM464.071111 611.555556h142.222222c7.879111 0 14.222222-6.357333 14.222223-14.222223s-6.343111-14.222222-14.222223-14.222222h-142.222222a14.222222 14.222222 0 1 0 0 28.444445zM720.071111 583.111111h-56.888889a14.222222 14.222222 0 1 0 0 28.444445h56.888889c7.879111 0 14.222222-6.357333 14.222222-14.222223s-6.343111-14.222222-14.222222-14.222222zM293.404444 682.666667h56.888889a14.222222 14.222222 0 1 0 0-28.444445h-56.888889a14.222222 14.222222 0 1 0 0 28.444445zM407.182222 654.222222a14.222222 14.222222 0 1 0 0 28.444445h113.777778c7.879111 0 14.222222-6.357333 14.222222-14.222223s-6.343111-14.222222-14.222222-14.222222h-113.777778zM720.071111 654.222222h-142.222222a14.222222 14.222222 0 1 0 0 28.444445h142.222222c7.879111 0 14.222222-6.357333 14.222222-14.222223s-6.343111-14.222222-14.222222-14.222222zM435.626667 739.555556h-142.222223a14.222222 14.222222 0 1 0 0 28.444444h142.222223a14.222222 14.222222 0 1 0 0-28.444444zM535.182222 739.555556h-42.666666a14.222222 14.222222 0 1 0 0 28.444444h42.666666c7.879111 0 14.222222-6.357333 14.222222-14.222222s-6.343111-14.222222-14.222222-14.222222z" p-id="37886"></path>
                                </svg>
                                <span className="text">保存</span>
                            </div>
                        </Button>
                        <Dropdown overlay={
                            <Menu>
                                <Menu.Item>
                                    <PopoverConfirm onOK={this.onDelete}>
                                        <a>删除</a>
                                    </PopoverConfirm>
                                </Menu.Item>
                            </Menu>
                        }>
                            <Button size="small" className="feature-button">
                                <div className="wrapper" style={{ marginTop: 1 }}>
                                    <span className="text">更多 <DownOutlined /></span>
                                </div>
                            </Button>
                        </Dropdown>
                        {/*<Button size="small" type="danger" style={{marginLeft:8,marginRight:8}}>删除</Button>*/}
                    </div>
                }>
                    <TabPane tab={<span><BuildOutlined style={{ marginRight: 8 }} />画布</span>} key="dev">
                        <Card size="small" style={{ margin: "2vh 10vw" }} className="abm_frontend_widget_component_wrapper">
                            <div style={tabEditorContentStyle}>
                                {
                                    pageLayoutType === Constants.PAGE_LAYOUT_TYPE_FLUID &&
                                    <FluidContentLayoutDesigner editType={'block'} {...this.props} include={this.include} exclude={this.exclude} filterType={this.filterType} containerModel={containerModel} />
                                }
                                {
                                    pageLayoutType === Constants.PAGE_LAYOUT_TYPE_CUSTOM &&
                                    <ContentLayout {...otherProps} include={this.include} exclude={this.exclude} filterType={this.filterType} containerModel={containerModel} />
                                }
                            </div>
                        </Card>
                    </TabPane>
                    <TabPane tab={<span><SettingOutlined style={{ marginRight: 8 }} />设置</span>} key="setting">
                        <Tabs tabPosition={"left"} animated tabBarGutter={2}>
                            <TabPane tab={<span><ProfileOutlined style={{ marginRight: 8 }} />属性设置</span>} key="props">
                                <div style={{ width: "60%", marginTop: 12 }}>
                                    <BlockSettingForm config={this.blockConfig} onValuesChange={(changedField, allValue) => this.blockConfig = allValue} />
                                </div>
                            </TabPane>
                            <TabPane tab={<span><CodeOutlined style={{ marginRight: 8 }} />&nbsp;源码</span>} key="json">
                                <AceViewer model={{
                                    showDiff: false,
                                    defModel: { height: height - 48, disableShowDiff: true, mode: "json" },
                                }} mode="json" value={pageModel.toJSON()} readOnly={true} />
                            </TabPane>
                        </Tabs>
                    </TabPane>
                    <TabPane tab={<span><DatabaseOutlined style={{ marginRight: 8 }} />区块数据源</span>} key="datasource">
                        <DataSourceEditor value={pageModel.config && pageModel.config.dataSourceMeta || {}} onValuesChange={(changedValues, allValues) => {
                            pageModel.setDataSourceMeta(allValues);
                        }} onChange={this.handleChange} />
                    </TabPane>
                </Tabs>
                <Drawer
                    title={showJson ? "源JSON" : "区块预览"}
                    placement="right"
                    destroyOnClose={true}
                    width={'50vw'}
                    closable={true}
                    onClose={this.onClose}
                    visible={openDrawer}
                >
                    <div>
                        {showPreview && <PageContent pageLayoutType={pageLayoutType} pageModel={pageModel} />}
                        {
                            showJson &&
                            <JsonEditor json={pageModel.toJSON()} mode="code" readOnly={true} style={{ height: '82vh' }} />
                        }
                    </div>
                </Drawer>
            </div>
        );
    }
}