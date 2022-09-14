/**
 * Created by caoshuaibiao on 2020/12/10.
 * 工作桌面
 */
import React from "react";
import { MinusCircleFilled } from '@ant-design/icons';
import { Layout, Divider, Modal, Avatar } from "antd";
import GridLayout from "react-grid-layout";
import { connect } from 'dva';
import "./index.less"
import properties from "../../properties";

const colors = ['#90ee90', '#2191ee', '#9a69ee', '#41ee1a', '#484aee', '#6B8E23', '#48D1CC', '#3CB371', '#388E8E', '#1874CD'];

@connect(({ home }) => ({
    home: home
}))
export default class DesktopLayout extends React.Component {
    static defaultProps = {
        className: "layout",
        items: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15],
        rowHeight: 100,
        layout: [],
        onLayoutChange: function () { },
    };

    constructor(props) {
        super(props);
        let layout = this.generateLayout();
        this.state = { layout, visible: false, isMouseDown: false };

    }

    handleRemoveItem = (e, item) => {
        const { dispatch, home } = this.props;
        const { workspaces, desktopIndex } = home;
        let temIndex = 0;
        e.stopPropagation();
        (workspaces[desktopIndex].items || []).map((desktopItem, index) => {
            if (desktopItem.id === item.id) {
                temIndex = index;
            }
        });
        workspaces[desktopIndex].items.splice(temIndex, 1);
        dispatch({
            type: "home/setWorkspaces", workspaces,
        });
    };

    onMouseDown = () => {
        this.setState({
            isMouseDown: true,
        });
    };


    handleClick = (item) => {
        let { isMouseDown } = this.state;
        if (isMouseDown) {
            if (item.href) {
                window.open(item.href);
            } else {
                if (item.appId) {
                    window.open("#/" + item.appId + `?namespaceId=${item.namespaceId || properties.defaultNamespace}&stageId=${item.stageId || properties.defaultStageId}`);
                }
            }
        }
    };

    generateDOM = () => {
        let { items, home } = this.props, handleRemoveItem = this.handleRemoveItem;
        const { switchDeleteState } = home;
        return items.map((item, i) => {
            let IconRender = null;
            if (item.logoUrl) {
                IconRender = <img className="icon" src={item.logoUrl} />
            } else {
                IconRender = <Avatar size={48} style={{
                    backgroundColor: colors[i % 10],
                    verticalAlign: 'middle',
                    fontSize: '18px'
                }}>
                    {item.appName && (item.appName).substring(0, 1)}
                </Avatar>
            }
            //shaky
            return (
                <div key={i} className={switchDeleteState ? " item-block" : "item-block"}>
                    <div className="item-mask" />
                    <div className="icon-wrapper" style={{ position: "relative" }} onMouseDown={this.onMouseDown} onClick={() => this.handleClick(item)}>
                        {IconRender}
                        <span className="removeHandle"
                            style={{ display: switchDeleteState ? "block" : "none" }}
                            onClick={(e) => handleRemoveItem(e, item)}>
                            <MinusCircleFilled />
                        </span>
                    </div>
                    <div style={{ marginTop: 8 }} onClick={() => this.handleClick(item)}>{item.appName}</div>
                </div>
            );
        });
    };

    generateLayout() {
        let { items, layout } = this.props;
        return items.map((item, i) => {
            return {
                y: 6 - parseInt(i / 6),
                x: i % 6,
                w: 1,
                h: 1,
                i: i.toString(),
            };
        });
    }
    componentWillReceiveProps() {
        let layout = this.generateLayout();
        this.setState({ layout, visible: false, isMouseDown: false });
    }
    onDragStop = (layout) => {
        this.setState({
            isMouseDown: false,
        })
        this.props.onLayoutChange(layout);
    }

    render() {
        let { layout } = this.state, { type } = this.props;
        let layoutWidth = type === 'custom' ? 1400 : 800;
        return (
            <div style={{ width: layoutWidth, height: 420 }}>
                <GridLayout
                    {...this.props}
                    layout={layout}
                    rowHeight={100}
                    width={layoutWidth}
                    height={420}
                    cols={6}
                    margin={[16, 16]}
                    isResizable={false}
                    onDragStop={this.onDragStop}
                    resizeHandle={() => <div />}
                >
                    {this.generateDOM()}
                </GridLayout>
            </div>
        );
    }
}