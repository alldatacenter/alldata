/**
 * Created by caoshuaibiao on 2021/2/1.
 * 预览widget Card
 */
import React from 'react';
import { DownOutlined } from '@ant-design/icons';
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
} from 'antd';
import WidgetSelector from './WidgetSelector';

import './index.less';


export default class WidgetPreviewCard extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            visible: false,
            drawerTitle: "选择卡片",
            selectKey: "new",
            currentMeta: null,
            current: 0,
        };
    }

    onClose = () => {
        this.setState({
            visible: false,
            currentMeta: null,
            current: 0
        });
    };

    show = () => {
        this.setState({
            visible: true,
        });
    };

    onWidgetSelected = (widgetMeta) => {
        this.setState({
            currentMeta: widgetMeta,
            drawerTitle: "添加卡片",
            current: 1
        });

    };

    onChange = (current) => {
        this.setState({ current });
    };

    onTabChange = (key) => {
        this.setState({ selectKey: key });
    };


    handleComplete = (e) => {
        e && e.stopPropagation && e.stopPropagation();
        let saved = this.widgetEditor.saveEditor();
        if (saved) {
            let { onWidgetCreated } = this.props;
            let model = this.widgetEditor.getModel();
            onWidgetCreated && onWidgetCreated(model);
            this.onClose();
        }
    };

    render() {
        let { drawerTitle, visible, currentMeta, current, selectKey } = this.state;
        const menu = (
            <Menu>
                <Menu.Item>
                    <a>
                        编辑
                    </a>
                </Menu.Item>
                <Menu.Item>
                    <a>
                        复制
                    </a>
                </Menu.Item>
                <Menu.Item>
                    <a>
                        删除
                    </a>
                </Menu.Item>
            </Menu>
        );
        return (
            <Card size="small" style={{ width: '100%', height: "100%" }}
                className="abm_frontend_widget_component_wrapper"
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
                onTabChange={key => {
                    this.onTabChange(key);
                }}
                tabBarExtraContent={
                    <Dropdown overlay={menu}>
                        <a className="ant-dropdown-link" onClick={e => e.preventDefault()}>
                            配置 <DownOutlined />
                        </a>
                    </Dropdown>
                }
            >
                {selectKey === "new" && <WidgetSelector onWidgetSelected={this.onWidgetSelected} />}
                <Drawer
                    title={
                        <div style={{ display: 'flex', justifyContent: "space-between" }}>
                            <div>{drawerTitle}</div>
                            <div>
                                <Button onClick={this.onClose} style={{ marginRight: 8 }}>
                                    取消
                                </Button>
                                <Button disabled={!currentMeta} onClick={this.handleComplete} type="primary">
                                    保存
                                </Button>
                            </div>
                        </div>
                    }
                    placement="right"
                    maskClosable={false}
                    destroyOnClose={true}
                    width={'80%'}
                    closable={false}
                    onClose={this.onClose}
                    visible={visible}
                >

                </Drawer>
            </Card>
        );
    }
}