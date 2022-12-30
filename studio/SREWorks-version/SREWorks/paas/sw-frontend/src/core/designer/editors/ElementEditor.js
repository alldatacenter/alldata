/**
 * Created by caoshuaibiao on 2021/2/1.
 */
import React from 'react';
import { CloseOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import {
    Card,
    Tabs,
    Tooltip,
    Collapse,
    List,
    Avatar,
    Select,
    Row,
    Col,
    Input,
    InputNumber,
    Button,
    Switch,
    Radio
} from 'antd';
import FormEditor from './FormEditor';
import FormElementType from '../../../components/FormBuilder/FormElementType';
import FormElementFactory from '../../../components/FormBuilder/FormElementFactory';
import Constants from '../../framework/model/Constants';
import DataSourceEditor from "./DataSourceEditor";
import widgetLoader from '../../framework/core/WidgetLoader';
import ReactMarkdown from "react-markdown";
import { getLegacyWidgetMeta } from '../../framework/components/WidgetRepository';
import AceViewer from '../../../components/FormBuilder/FormItem/AceViewer';
import debounce from 'lodash.debounce';
import { throttle } from 'lodash'
import * as util from '../../../utils/utils';
import ActionSetting from './settings/ActionSetting';
import BlockSetting from './settings/BlockSetting';
import CustomSetting from './settings/CustomSetting';
import FilterSetting from './settings/FilterSetting';
import ToolbarSetting from './settings/ToolbarSetting';
import WidgetSetting from './settings/WidgetSetting';
import TabFilterSetting from './settings/TabFilterSetting';
import ScreenSetting from './settings/ScreenSetting';
import service from '../../services/appMenuTreeService';

const { Panel } = Collapse;
const { TabPane } = Tabs;

export default class ElementEditor extends React.Component {

    constructor(props) {
        super(props);
        let { widgetModel } = props;
        this.commonConfig = JSON.parse(JSON.stringify(widgetModel.config));
        this.state = {
            widgetModel: widgetModel,
            widgetMeta: null,
            isJsonEdit: false,
            configWrapper: JSON.parse(JSON.stringify(widgetModel.config)),
            hasWrapper: JSON.parse(JSON.stringify(widgetModel.config)),
            customList: []
        };
        this.isFilter = widgetModel.isFilter();
        this.isTabFilter = widgetModel.isTabFilter();
        this.isAction = widgetModel.isAction();
        this.isBlock = widgetModel.isBlock();
        this.onPreviewThrottle = throttle(this.onPreview, 1000);
    }

    componentWillMount() {
        let { widgetModel } = this.props;
        widgetLoader.getWidgetMeta(widgetModel).then(widgetMeta => {
            //旧组件适配,带逐步替换
            if (!widgetMeta) {
                widgetMeta = getLegacyWidgetMeta(widgetModel);
            }
            this.setState({
                widgetMeta: widgetMeta,
            });
        });
    }
    getCustomComList = () => {
        service.getCustomList().then(list => {
            if (list) {
                this.setState({
                    customList: list
                })
            }
        })
    }
    setCommonConfig = (cfg) => {
        this.commonConfig = Object.assign(this.commonConfig, cfg);
        if (cfg.hasWrapper) {
            this.setState({
                configWrapper: cfg.wrapper || cfg,
                hasWrapper: cfg.hasWrapper || cfg
            })
        }
        // dispatch({ type: 'global/switchBtnLoading',payload:{btnLoading: true}});
        this.onPreviewThrottle();
    };

    saveEditor = () => {
        let { widgetMeta, widgetModel } = this.state, values = this.saveHandleHook.validate();
        if (values) {
            let { userConfig, ...other } = values;
            let config = Object.assign({}, widgetMeta.configSchema.defaults, userConfig.config, other);
            widgetModel.updateConfig(config);
            return true;
        }
        return false;
    };

    saveHandleHook = (values) => {
        return Object.assign({}, this.commonConfig, values);
    };

    getModel = () => {
        const { widgetModel } = this.state;
        return widgetModel;
    };

    onPreview = () => {
        let { widgetModel } = this.state, { onSave } = this.props;
        let config = Object.assign({}, this.commonConfig);
        widgetModel.updateConfig(config);
        onSave && onSave(widgetModel);
    };

    onPreviewThrottle = () => {
        throttle(this.onPreview, 1000);
    }
    onClose = () => {
        const { onClose } = this.props;
        onClose && onClose();
    };

    onConfigChanged = (conf) => {
        if (typeof conf !== 'object') return;
        this.commonConfig = conf;
        this.onPreviewThrottle();
    };
    switchUpdateRule = (isJsonEdit) => {
        this.setState({
            isJsonEdit
        })
    }
    componentWillReceiveProps() {

    }
    render() {
        let { widgetModel, widgetMeta, isJsonEdit } = this.state;
        let { configWrapper, customList, hasWrapper } = this.state;
        let CommonTabContent = null, tabContentStyle = { height: 'calc(45vh - 10px)', overflowY: "auto", overflowX: "none" };
        // if(this.isFilter){
        //      CommonTabContent=(props)=><FilterSetting {...props}/>;
        // }else if(this.isAction){
        //      CommonTabContent=(props)=><ActionSetting {...props}/>;
        // }else if(this.isBlock){
        //     CommonTabContent=(props)=><BlockSetting {...props}/>;
        // }else{
        //     CommonTabContent=(props)=><WidgetSetting {...props}/>;
        // }
        // 兼容自定义共模板组件
        let editTitle = ""
        if (widgetModel.type === 'CustomComp') {
            editTitle = widgetModel.compName || widgetModel.name;
            if (widgetMeta && widgetModel.name) {
                widgetMeta['configSchema']['schema']['properties']['compName']['initValue'] = widgetModel.name;
                widgetMeta['configSchema']['schema']['properties']['compDescribtion']['initValue'] = (widgetModel.info && widgetModel.info.description) || ''
                let obj = {
                    compDescribtion: (widgetModel.info && widgetModel.info.description) || '',
                    compName: widgetModel.name
                }
                this.commonConfig = Object.assign(this.commonConfig, obj);
                let config = Object.assign({}, this.commonConfig);
                widgetModel.updateConfig(config);
            }
        } else {
            editTitle = (widgetMeta && widgetMeta.title) || ''
        }
        return (
            <div>
                <Tabs defaultActiveKey="bars" size="small"
                    renderTabBar={(props, DefaultTabBar) => {
                        return (
                            <div>
                                <h4 style={{ float: "left", marginRight: 36, marginTop: 9, fontSize: 18 }}>{editTitle}</h4>
                                <DefaultTabBar {...props} />
                            </div>
                        )
                    }}
                    tabBarExtraContent={
                        <div style={{ display: 'flex', justifyContent: "space-between" }}>
                            <div>
                                {/* <Button size="small" onClick={this.onPreview} style={{ marginRight: 8 }}>
                                      预览
                                  </Button>
                                  <Button size="small" type="primary"  onClick={this.onSave} style={{ marginRight: 8 }}>
                                      保存
                                  </Button>*/}
                                <div style={{ marginRight: '10px', height: '40px', lineHeight: '40px', display: 'inline-block' }}>{isJsonEdit ? '源码编辑' : '可视化编辑'}</div>
                                <Switch style={{ marginRight: '30px', display: 'inline-block' }} defaultChecked checked={isJsonEdit} onChange={this.switchUpdateRule} />
                                <a onClick={this.onClose}>
                                    <CloseOutlined />
                                </a>
                            </div>
                        </div>
                    }
                >
                    {
                        !isJsonEdit &&
                        <TabPane key="common" tab={<span>通用属性</span>}>
                            <div style={tabContentStyle}>
                                {/* <CommonTabContent /> */}
                                {
                                    this.isFilter &&
                                    <FilterSetting widgetModel={widgetModel} config={Object.assign({}, this.commonConfig)} onValuesChange={(changedField, allValue) => this.setCommonConfig(allValue)} />
                                }
                                {
                                    this.isAction &&
                                    <ActionSetting widgetModel={widgetModel} config={Object.assign({}, this.commonConfig)} onValuesChange={(changedField, allValue) => this.setCommonConfig(allValue)} />
                                }
                                {
                                    this.isBlock &&
                                    <BlockSetting widgetModel={widgetModel} config={Object.assign({}, this.commonConfig)} onValuesChange={(changedField, allValue) => this.setCommonConfig(allValue)} />
                                }
                                {
                                    !this.isBlock && !this.isFilter && !this.isAction &&
                                    <WidgetSetting widgetModel={widgetModel} config={Object.assign({}, this.commonConfig)} onValuesChange={(changedField, allValue) => this.setCommonConfig(allValue)} />
                                }
                            </div>
                        </TabPane>
                    }
                    {
                        this.isFilter && !isJsonEdit &&
                        <TabPane key="filter" tab={<span>过滤项</span>}>
                            <div style={tabContentStyle}>
                                {
                                    this.isTabFilter && <TabFilterSetting onChange={(parameters) => this.setCommonConfig({ parameters: parameters })} parameters={this.commonConfig.parameters || []} />
                                }
                                {
                                    !this.isTabFilter && <FormEditor tabPosition="left" parameters={this.commonConfig.parameters || []} onChange={(parameters) => this.setCommonConfig({ parameters: parameters })} />
                                }
                            </div>
                        </TabPane>

                    }
                    {
                        this.isAction && !widgetModel.isStepAction() && !isJsonEdit &&
                        <TabPane key="action" tab={<span>操作表单</span>}>
                            <div style={tabContentStyle}>
                                <FormEditor tabPosition="left" parameters={this.commonConfig.parameters || []} onChange={(parameters) => this.setCommonConfig({ parameters: parameters })} />
                            </div>
                        </TabPane>

                    }
                    {
                        !this.isAction && !this.isFilter && !this.isBlock && widgetMeta && !isJsonEdit &&
                        (
                            (widgetMeta.configSchema.schema && Object.keys(widgetMeta.configSchema.schema).length) || widgetMeta.configSchema.supportItemToolbar
                        ) &&
                        <TabPane key="custom" tab={<span>组件属性</span>}>
                            <Row style={tabContentStyle}>
                                <Col span={16}>
                                    <div>
                                        {
                                            widgetMeta.configSchema.schema && Object.keys(widgetMeta.configSchema.schema).length > 0 &&
                                            <CustomSetting widgetModel={widgetModel}
                                                config={Object.assign({}, this.commonConfig)}
                                                widgetMeta={widgetMeta}
                                                onValuesChange={(changedField, allValue) => this.setCommonConfig(changedField)} />
                                        }
                                        {
                                            widgetMeta.configSchema.supportItemToolbar &&
                                            <Collapse>
                                                <Panel header="数据项工具栏" key="row_action">
                                                    <ToolbarSetting noFilter={true} noHelp={true} configIndex="itemToolbar" widgetModel={widgetModel} config={Object.assign({}, this.commonConfig)} onValuesChange={(changedField, allValue) => this.setCommonConfig({ itemToolbar: allValue })} />
                                                </Panel>
                                            </Collapse>
                                        }
                                        {/* 大屏图表组件位置标配 */}
                                        {
                                            widgetMeta.type === 'DisplayScreens' &&
                                            <Collapse>
                                                <Panel header="大屏图表配置" key="row_action">
                                                    <ScreenSetting widgetModel={widgetModel} config={Object.assign({}, this.commonConfig)} onValuesChange={(changedField, allValue) => this.setCommonConfig({ chartDisplayConfig: allValue })} />
                                                </Panel>
                                            </Collapse>
                                        }
                                    </div>
                                </Col>
                                <Col span={8}>
                                    <div>
                                        {
                                            widgetMeta && widgetMeta.info.docs &&
                                            <Card title="配置文档" size="small">
                                                <div>
                                                    <ReactMarkdown children={widgetMeta.info.docs} escapeHtml={false} />
                                                </div>
                                            </Card>
                                        }
                                    </div>
                                </Col>
                            </Row>
                        </TabPane>
                    }
                    {
                        !this.isAction && !this.isFilter && !(hasWrapper && hasWrapper === Constants.CARD_WRAPPER_NONE) && (widgetMeta && widgetMeta.configSchema.supportToolbar) && !isJsonEdit &&
                        <TabPane key="toolbar" tab={<span>工具栏</span>}>
                            <div style={tabContentStyle}>
                                <ToolbarSetting widgetModel={widgetModel} config={Object.assign({}, this.commonConfig)} onValuesChange={(changedField, allValue) => this.setCommonConfig({ toolbar: allValue })} />
                            </div>
                        </TabPane>
                    }
                    {
                        isJsonEdit &&
                        <TabPane key="widget" tab={<span>源码</span>}>
                            <Row gutter={8}>
                                <Col span={18}>
                                    {/*<JsonEditor json={Object.assign({},widgetModel.config,this.commonConfig)} mode="code" readOnly={false} onChange={this.onConfigChanged} changeInterval={500} style={{height:"calc(50vh - 100px)"}}/>*/}
                                    <AceViewer model={{ showDiff: false, defModel: { height: "calc(50vh - 64px)", disableShowDiff: true, mode: "json" } }}
                                        mode="json"
                                        value={Object.assign({}, this.commonConfig)}
                                        onChange={this.onConfigChanged}
                                    />
                                </Col>
                            </Row>
                        </TabPane>
                    }
                    {
                        !isJsonEdit &&
                        <TabPane tab="组件数据源" key="datasource">
                            <div style={tabContentStyle}>
                                <DataSourceEditor value={this.commonConfig.dataSourceMeta || {}} onValuesChange={(changedValues, allValues) => {
                                    this.setCommonConfig({ dataSourceMeta: allValues });
                                    widgetModel.setDataSourceMeta(allValues);
                                }} />
                            </div>
                        </TabPane>
                    }
                </Tabs>
            </div>
        );
    }
}
