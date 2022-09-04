/**
 * Created by caoshuaibiao on 2021/2/1.
 * 挂件定义card
 */
import React from 'react';
import { CloseOutlined, CopyOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import {
    Spin,
    Button,
    Card,
    Tabs,
    Tooltip,
    Skeleton,
    Drawer,
    Collapse,
    List,
    Avatar,
    Steps,
    Menu,
    Dropdown,
    message,
} from 'antd';
import copy from 'copy-to-clipboard';
import WidgetSelector from './WidgetSelector';
import JsonEditor from '../../../components/JsonEditor';
import WidgetModel from '../model/WidgetModel';
import ElementEditor from '../../designer/editors/ElementEditor';
import WidgetCard from '../core/WidgetCard';
import PopoverConfirm from '../../../components/PopoverConfirm';
import widgetLoader from '../core/WidgetLoader';
import { getLegacyWidgetMeta } from './WidgetRepository';
import Constants from '../model/Constants';
import uuidv4 from 'uuid/v4';
import Bus from '../../../utils/eventBus';

import './index.less';



export default class WidgetHandleCard extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            visible: false,
            selectKey: "new",
            widgetModel: props.widgetModel,
            prevModel: props.widgetModel,
            widgetMeta: null,
            prevKey: 0,
            current: 0,
        };
        this.copyWidgetJson = {};
    }

    componentWillMount() {
        let { widgetModel } = this.props;
        this.loadWidgetMeta(widgetModel);
    }
    componentDidMount() {
        Bus.on('closeOtherDrawer',(flag)=> {
           if(!flag) {
               this.setState({
                   visible: flag
               });
           } 
        })
    }
    loadWidgetMeta(widgetModel) {
        if (widgetModel) {
            return widgetLoader.getWidgetMeta(widgetModel).then(widgetMeta => {
                console.log(widgetMeta,widgetModel,'loadedMeta')
                //旧组件适配,带逐步替换
                if (!widgetMeta) {
                    widgetMeta = getLegacyWidgetMeta(widgetModel);
                }
                this.setState({ widgetMeta })
            });
        }
    }

    onClose = () => {
        let { prevModel } = this.state;
        this.setState({
            visible: false,
            current: 0,
            widgetModel: prevModel
        });
    };

    show = () => {
        Bus.emit('closeOtherDrawer',false);
        this.setState({
            visible: !this.state.visible,
        });
    };

    onWidgetSelected = (widgetMeta) => {
        let widgetModel = WidgetModel.CREATE_DEFAULT_INSTANCE();
        widgetModel.initFromMeta(widgetMeta);
        widgetModel.setNodeModel(this.props.nodeModel);
        this.setState({
            widgetMeta: widgetMeta,
        });
        this.onSave(widgetModel);
    };

    onChange = (current) => {
        this.setState({ current });
    };

    onTabChange = (key) => {
        this.setState({ selectKey: key });
    };


    editorChange = (json) => {
        this.copyWidgetJson = json;
    };

    onDelete = () => {
        const { onDelete } = this.props;
        onDelete && onDelete();
    };

    onPreview = (previewConf) => {
        let { prevKey = 0 } = this.state, { widgetModel } = this.props;
        let preModel = new WidgetModel({ type: widgetModel.type, config: previewConf });
        preModel.setNodeModel(widgetModel.nodeModel);
        this.setState({
            prevKey: prevKey + 1,
            prevModel: preModel
        });
    };


    onSave = (widgetModel) => {
        let { prevKey = 0 } = this.state;
        if (!this.props.widgetModel) {
            let { onWidgetCreated } = this.props;
            onWidgetCreated && onWidgetCreated(widgetModel);
        }
        this.setState({
            prevKey: prevKey + 1,
            widgetModel: widgetModel,
            prevModel: widgetModel
        });
        // dispatch({ type: 'global/switchBtnLoading',payload:{btnLoading: false}});
    };
    fallbackCopyTextToClipboard = (text) => {
        localStorage.setItem("copyedModel",text)
        message.success("已成功复制!");
    }
    onCopy = () => {
        let { widgetModel } = this.props;
        let replaceUniquekeyWidgetModel = _.cloneDeep(widgetModel);
        if (replaceUniquekeyWidgetModel && replaceUniquekeyWidgetModel.uniqueKey) {
            replaceUniquekeyWidgetModel.uniqueKey = uuidv4()
        }
        let textWidgetModel = typeof replaceUniquekeyWidgetModel === "string" ? replaceUniquekeyWidgetModel : JSON.stringify(replaceUniquekeyWidgetModel)
        try {
            if (!navigator.clipboard) {
                this.fallbackCopyTextToClipboard(textWidgetModel)
            } else {
                let isCoped = navigator.clipboard.writeText(textWidgetModel);
                if (isCoped) {
                    message.success("已成功复制!");
                } else {
                    message.warning("复制失败!");
                }
            }
        } catch (e) {
            message.warning("复制失败!");
        }
        // let isCoped = copy(JSON.stringify(replaceUniquekeyWidgetModel, null, 2));
    };

    handleCopyCreate = () => {
        let { handleKey, nodeModel } = this.props, { type, config } = this.copyWidgetJson;
        if (!type || !config) {
            message.warning("复制的组件JSON定义不合法！");
            return;
        }
        this.copyWidgetJson.config.uniqueKey = handleKey;
        let widgetModel = new WidgetModel(this.copyWidgetJson);
        widgetModel.setNodeModel(nodeModel);
        this.loadWidgetMeta(widgetModel);
        this.onSave(widgetModel);
    };
    render() {
        let { visible, selectKey, prevKey, widgetModel, prevModel, widgetMeta } = this.state, { cardHeight, exclude, include, filterType } = this.props;
        let editTitle = "";
        if (widgetModel && widgetModel.type === 'CustomComp') {
            editTitle = widgetModel.name || widgetModel.compName;
        } else {
            editTitle = (widgetMeta && widgetMeta.title) || ''
        }
        if (widgetModel) {
            return (
                <div style={{ width: '100%', overflow: "auto", padding: "8px", border: "1px dashed #d9d9d9", borderRadius: 5, height: cardHeight,paddingTop:12 }}>
                    <div className="code-box-title">
                        <span>{editTitle}</span>
                        <a onClick={this.show} className="action-icon">
                            <Tooltip title="编辑">
                                <EditOutlined />
                            </Tooltip>
                        </a>
                        <a className="action-icon" onClick={this.onCopy}>
                            <Tooltip title="复制组件源码">
                                <CopyOutlined />
                            </Tooltip>
                        </a>
                        <PopoverConfirm onOK={this.onDelete}>
                            <a className="action-icon">
                                <Tooltip title="删除">
                                    <DeleteOutlined className="delete" />
                                </Tooltip>
                            </a>
                        </PopoverConfirm>
                    </div>
                    <WidgetCard {...this.props} widgetModel={prevModel} key={prevKey} cardHeight={cardHeight} mode={Constants.WIDGET_MODE_EDIT} />
                    <Drawer
                        placement="bottom"
                        maskClosable={false}
                        destroyOnClose={true}
                        bodyStyle={{ top: 0, bottom: 0, padding: "0px 12px" }}
                        width={'80%'}
                        height={"52vh"}
                        mask={false}
                        closable={false}
                        onClose={this.onClose}
                        visible={visible}
                        onClick={(e) => e.stopPropagation()}
                        onMouseDown={(e) => e.stopPropagation()}
                    >
                        <ElementEditor widgetModel={widgetModel} ref={r => this.widgetEditor = r} onClose={this.onClose} onPreview={this.onPreview} onSave={this.onSave} />
                    </Drawer>
                </div>
            );
        }
        return (
            <Card size="small" style={{ width: '100%', height: "100%" }}
                className="abm_frontend_widget_component_wrapper handle-card-title"
                bodyStyle={{ overflow: "auto", height: cardHeight - 48, padding: selectKey === "paste" ? '3px 0px' : undefined }}
                tabList={[
                    {
                        "key": "new",
                        "tab": "新建"
                    },
                    {
                        "key": "paste",
                        "tab": "粘贴"
                    }
                ]}
                tabProps={{
                    size: "small"
                }}
                onTabChange={key => {
                    this.onTabChange(key);
                }}
                tabBarExtraContent={<div style={{ alignItems: 'center', display: "flex" }}>
                    <div className="card-wrapper-title-prefix" />
                    <div>
                        <b style={{ marginLeft: 12, fontSize: 14 }}>新建元素</b>
                        <a style={{
                            position: "absolute",
                            right: 0,
                            cursor: "pointer",
                            zIndex: 1,
                        }} className="tab-delete-btn" onClick={this.onDelete}>
                            <CloseOutlined />
                        </a>
                    </div></div>}
            >
                {selectKey === "new" && <WidgetSelector filterType={filterType} include={include} exclude={exclude} onWidgetSelected={this.onWidgetSelected} />}
                {selectKey === "paste" &&
                    <div onMouseDown={(e) => e.stopPropagation()}>
                        <JsonEditor json={{}} readOnly={false} onChange={this.editorChange} style={{ height: '230px' }} />
                        <Button type="primary" style={{ marginTop: 3, marginLeft: 40 }} size="small" onClick={this.handleCopyCreate}>确定</Button>
                    </div>
                }

            </Card>
        );
    }
}
