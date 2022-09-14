/**
 * @author caoshuaibiao
 * @date 2021/6/29 19:47
 * @Description:流式布局中一行的容器
 */
import React from 'react';
import { Row, Col, Tooltip, Button, Drawer, Modal, Space, message } from 'antd';
import Constants from '../model/Constants';
import PopoverConfirm from "../../../components/PopoverConfirm";
import { PlusOutlined, CloseOutlined, BarChartOutlined, DeleteOutlined, EditOutlined, SnippetsOutlined } from '@ant-design/icons';
import WidgetHandleCard from './WidgetHandleCard';
import WidgetSelector from "./WidgetSelector";
import RowSetting from "./RowSetting";
import WidgetModel from "../model/WidgetModel";
import service from '../../services/appMenuTreeService';
import uuidv4 from "uuid/v4";

import './index.less';

class RowContainerHandler extends React.Component {

    constructor(props) {
        super(props);
        let { row, nodeModel } = props;
        this.state = {
            visible: false,
            currentIndex: -1,
            row: row,
            reload: 0
        }
    }

    onDelete = () => {
        let { mode, row, onRemove } = this.props;
        onRemove && onRemove();
    }

    onWidgetSelected = (widgetMeta) => {
        let { currentIndex, row } = this.state, { nodeModel, onUpdate } = this.props;
        let { elements } = row;
        let widgetModel = WidgetModel.CREATE_DEFAULT_INSTANCE();
        widgetModel.initFromMeta(widgetMeta);
        widgetModel.setNodeModel(nodeModel);
        elements[currentIndex] = widgetModel;
        onUpdate && onUpdate(row);
        this.setState({
            visible: false,
            currentIndex: -1
        });
    };
    getClipboard = () => {
        return localStorage.getItem("copyedModel");
    };
    initCopyedModel = (model, currentIndex) => {
        let { row } = this.state, { nodeModel, onUpdate } = this.props;
        let { elements } = row;
        let copyedModel = JSON.parse(model);
        copyedModel.uniqueKey = uuidv4();
        service.getWidgetRepository().then(category => {
            let allCompMeta = []
            category.forEach(cate => {
                allCompMeta = allCompMeta.concat(cate.children);
            })
            let widgetMeta = allCompMeta.find(comp => comp.type === copyedModel.type) || {}
            let widgetModel = WidgetModel.CREATE_DEFAULT_INSTANCE();
            widgetModel.initFromMeta(widgetMeta);
            widgetModel.setNodeModel(nodeModel);
            widgetModel.config = copyedModel.config;
            Object.assign(widgetModel, { ...copyedModel.config })
            elements[currentIndex] = widgetModel;
            onUpdate && onUpdate(row);
            this.setState({
                currentIndex: -1
            });
        })
    }
    pasteComp = (currentIndex) => {
        try {
            if (!navigator.clipboard) {
                let strModel = this.getClipboard()
                strModel && this.initCopyedModel(strModel, currentIndex)
            } else {
                navigator.clipboard.readText().then(clText => this.initCopyedModel(clText, currentIndex)
                ).catch(error => {
                    message.warn("请先复制组件源码")
                })
            }
        } catch (e) {
            message.warn("请先复制组件源码")
        }
    }
    openSelector = (currentIndex) => {
        this.setState({
            visible: true,
            currentIndex: currentIndex
        })
    }

    onCloseSelector = () => {
        this.setState({
            visible: false,
            currentIndex: -1
        });
    };

    onRemoveWidget = (widget) => {
        let { row, reload } = this.state, { onUpdate } = this.props;
        let { elements } = row;
        elements.forEach((element, index) => {
            if (widget.config && element.config && widget.config.uniqueKey === element.config.uniqueKey) {
                elements[index] = {};
            }
        })
        //let cloneRow={...row};
        onUpdate && onUpdate(row);
        this.setState({
            //row:cloneRow
            reload: reload++
        });
    };


    rowConfigEdit = () => {
        let { row, reload } = this.state, { onUpdate } = this.props;
        Modal.confirm({
            title: '行属性',
            icon: '',
            width: 640,
            content: <div><RowSetting row={row} onValuesChange={(changedValues, allValues) => Object.assign(row, allValues)} /></div>,
            onOk: () => {
                //let cloneRow={...row};
                onUpdate && onUpdate(row);
                this.setState({
                    // row:cloneRow,
                    reload: reload++
                });
            },
            okText: '修改',
            cancelText: '取消',
        });
    }
    creatComp = () => {
        window.open('#/system/plugin/opsdev?tab=frontend', "_blank")
    }
    render() {
        let { exclude, include, filterType } = this.props, { visible, row } = this.state;
        let { spans = "12,12", elements = [] } = row, defaultRowHeight = 220, rowHeight = "auto", handleCardHeight = "auto", rowMinHeight = 202;
        if (row.height && isFinite(row.height)) {
            rowHeight = parseInt(row.height);
            defaultRowHeight = rowHeight;
            handleCardHeight = rowHeight - 18;
            rowMinHeight = defaultRowHeight - 18;
        }
        return (
            <div style={{ display: "flex", width: "100%" }}>
                <div className="row_edit_handler">
                    <span>
                        <a onClick={this.rowConfigEdit} className="action-icon">
                            <Tooltip title="编辑">
                                <EditOutlined />
                            </Tooltip>
                        </a>
                    </span>
                    <span>
                        <PopoverConfirm onOK={this.onDelete}>
                            <a className="action-icon">
                                <Tooltip title="删除">
                                    <DeleteOutlined className="delete" />
                                </Tooltip>
                            </a>
                        </PopoverConfirm>
                    </span>
                </div>
                <div className="row_container_wrapper" style={{ height: rowHeight, minHeight: rowMinHeight, paddingTop: 20 }}>
                    <Row gutter={Constants.WIDGET_DEFAULT_MARGIN}>
                        {
                            spans.split(",").map((span, index) => {
                                let widgetModel = elements[index];
                                if (widgetModel && widgetModel.type) {
                                    return (
                                        <Col span={parseInt(span)} key={index}>
                                            <WidgetHandleCard {...this.props}
                                                handleKey={widgetModel.uniqueKey}
                                                cardHeight={handleCardHeight}
                                                widgetModel={widgetModel}
                                                onDelete={() => this.onRemoveWidget(widgetModel)}
                                                openAction={this.handlePreviewOpenAction}
                                            />
                                        </Col>
                                    )
                                } else {
                                    return (
                                        <Col span={parseInt(span)} key={index}>
                                            <div className="blank_widget_placeholder" style={{ height: handleCardHeight, minHeight: rowMinHeight }}>
                                                {
                                                    parseInt(span) >= 6 && <Button type="dashed" icon={<BarChartOutlined />} onClick={() => this.openSelector(index)}>
                                                        选择组件
                                                    </Button>
                                                }
                                                {
                                                    parseInt(span) < 6 && <Tooltip placement="topLeft" title={'选择组件'}>
                                                        <Button type="dashed" icon={<BarChartOutlined />} onClick={() => this.openSelector(index)}>
                                                        </Button>
                                                    </Tooltip>
                                                }
                                                {
                                                    parseInt(span) >= 6 && <Button type="dashed" icon={<SnippetsOutlined />} onClick={() => this.pasteComp(index)}>
                                                        粘贴组件
                                                    </Button>
                                                }
                                                {
                                                    parseInt(span) < 6 && <Tooltip placement="topLeft" title={'粘贴组件'}>
                                                        <Button type="dashed" icon={<SnippetsOutlined />} onClick={() => this.pasteComp(index)}>
                                                        </Button>
                                                    </Tooltip>
                                                }
                                            </div>
                                        </Col>
                                    )
                                }
                            })
                        }
                    </Row>
                </div>
                <Drawer
                    title={"选择组件"}
                    placement="right"
                    maskClosable={true}
                    destroyOnClose={true}
                    mask={true}
                    extra={
                        <Space>
                            <Button type="primary" onClick={this.creatComp}>
                                新增自定义组件
                            </Button>
                        </Space>
                    }

                    width={'60%'}
                    closable={true}
                    onClose={this.onCloseSelector}
                    visible={visible}
                >
                    <WidgetSelector filterType={filterType} include={include} exclude={exclude} onWidgetSelected={this.onWidgetSelected} />
                </Drawer>
            </div>
        )

    }
}

export default RowContainerHandler