/**
 * Created by caoshuaibiao on 2020/12/11.
 * 条目选择导航内容区
 */
import React from "react";
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { List, Avatar } from "antd";
import { connect } from "dva";
import _ from "lodash";
import properties from "../../properties";
const colors = ['#90ee90', '#2191ee', '#9a69ee', '#41ee1a', '#484aee', '#6B8E23', '#48D1CC', '#3CB371', '#388E8E', '#1874CD'];
const innerTypes = ["交付", "监测", "管理", "控制", "运营", "服务", "快捷方式"];
@connect(({ home }) => ({
    home: home
}))
export default class NavSelectPanel extends React.Component {


    constructor(props) {
        super(props);
        this.state = {
            data: [],
            categoryMap: {},
            isCollectMap: {},
            displayDataSource: [],
            enterMouseMap: {},
        };
    }

    componentDidMount() {
        this.updateCollect(this.props);
    }

    filterSort = (arr) => {
        arr.map(item => {
            if (innerTypes.indexOf(item.title.trim()) !== -1) {
                item.index = innerTypes.indexOf(item.title.trim());
            } else {
                item.index = 100;
            }
        });
        return arr.sort((a, b) => a.index - b.index);
    };

    componentWillReceiveProps(nextProps, nextContext) {
        if (!_.isEqual(this.props, nextProps)) {
            this.updateCollect(nextProps)
        }
    }

    updateCollect = (props) => {
        let { displayDataSource } = this.state;
        const { home } = props;
        const { appStore, collectList, customQuickList } = home;
        let dataSource = JSON.parse(JSON.stringify([...appStore, ...customQuickList]));
        let categoryMap = {};
        let data = [];
        let isCollectMap = {};
        dataSource.map(item => {
            if (item.category) {
                if (categoryMap[item.category]) {
                    categoryMap[item.category].children.push(item);
                } else {
                    categoryMap[item.category] = {
                        title: item.category,
                        children: [item],
                    };
                }
            } else {
                if (categoryMap["快捷方式"]) {
                    categoryMap["快捷方式"].children.push(item);
                } else {
                    categoryMap["快捷方式"] = {
                        title: "快捷方式",
                        children: [item],
                    };
                }
            }
        });
        collectList.map(item => {
            isCollectMap[item.appId || item.id] = true;
        });
        data = Object.values(categoryMap);
        this.setState({ data, isCollectMap });
    };

    onItemSelected = (item) => {
        if (item.appId) {
            window.open("#/" + item.appId + `?namespaceId=${item.namespaceId || properties.defaultNamespace}&stageId=${item.stageId || properties.defaultStageId}`);
        } else if (item.href) {
            window.open(item.href);
        }
        // window.open(item.link,"_blank");
    };

    handleCollect = (e, child) => {
        e.stopPropagation();
        const { dispatch, home } = this.props;
        let { collectList, appStore, customQuickList, workspaces } = home;
        let { isCollectMap } = this.state;
        if (child.appId) {
            isCollectMap[child.appId] = !isCollectMap[child.appId];
        } else {
            isCollectMap[child.id] = !isCollectMap[child.id];
        }
        collectList = [];
        [...appStore, ...customQuickList].map(item => {
            if (isCollectMap[item.appId]) {
                collectList.push(item);
            }
            if (isCollectMap[item.id]) {
                collectList.push(item);
            }
        });
        this.setState({ isCollectMap });
        dispatch({ type: "home/setDesktopData", collectList, workspaces, customQuickList });
    };

    handleMouseEnter = (item) => {
        let { enterMouseMap } = this.state;
        enterMouseMap[item.appId || item.id] = true;
        this.setState({
            enterMouseMap,
        });
        document.getElementById("collect-star-" + (item.appId || item.id)).style.display = "inline-block";
    };

    handleMouseLeave = (item) => {
        let { isCollectMap } = this.state;
        let { enterMouseMap } = this.state;
        enterMouseMap[item.appId || item.id] = false;
        this.setState({
            enterMouseMap,
        });
        document.getElementById("collect-star-" + (item.appId || item.id)).style.display = isCollectMap[item.appId || item.id] ? "inline-block" : "none";
    };

    render() {
        let { data, isCollectMap, enterMouseMap } = this.state;
        return (
            <List
                itemLayout="horizontal"
                dataSource={this.filterSort(data)}
                style={{ width: "100%" }}
                renderItem={item =>
                (<List.Item style={{ padding: '6px 0' }}>
                    <List.Item.Meta
                        title={
                            <div style={{ display: 'flex', width: '100%' }}>
                                <div className="list-meta-title-color" style={{ width: 4, height: 20}} />
                                <span style={{ marginLeft: 8, marginRight: 8, fontSize: 14 }}>{item.title}</span>
                            </div>
                        }
                    />
                    <div className="category-nav">
                        {
                            item.children && (item.children || []).map((child, i) => {
                                let IconRender = null;
                                if (child.logoUrl) {
                                    // 兼容处理本地开发环境logo展示
                                    let logoUrl = child.logoUrl
                                    if(process.env.NODE_ENV === 'local'){
                                        logoUrl = properties.baseUrl + logoUrl
                                    }
                                    IconRender = <img className="small-icon" style={{ width: 28, height: 28 }} src={logoUrl} />
                                } else {
                                    IconRender = <Avatar size={20} style={{
                                        backgroundColor: colors[i % 10],
                                        verticalAlign: 'middle',
                                        fontSize: '14px'
                                    }}>
                                        {child.appName && (child.appName).substring(0, 1)}
                                    </Avatar>
                                }
                                return (
                                    <div className="item" style={{ position: "relative" }}
                                        onMouseEnter={() => this.handleMouseEnter(child)}
                                        onMouseLeave={() => this.handleMouseLeave(child)}
                                        onClick={() => this.onItemSelected(child)}>
                                        <span>
                                            {IconRender}
                                        </span>
                                        <span style={{ marginLeft: 8 }}>
                                            {child.appName}
                                        </span>
                                        <LegacyIcon theme={isCollectMap[child.appId || child.id] ? "filled" : ""}
                                            style={{
                                                display: isCollectMap[child.appId || child.id] || enterMouseMap[child.appId || child.id] ? "inline-block" : "none",
                                                color: isCollectMap[child.appId || child.id] ? "#faad14" : "",
                                                position: "absolute",
                                                top: "50%",
                                                transform: "translateY(-50%)", right: 10,
                                            }}
                                            id={"collect-star-" + (child.appId || child.id)}
                                            onClick={e => this.handleCollect(e, child)} type="star" />
                                    </div>
                                );
                            })
                        }
                    </div>
                </List.Item>
                )}
            />
        );
    }
}