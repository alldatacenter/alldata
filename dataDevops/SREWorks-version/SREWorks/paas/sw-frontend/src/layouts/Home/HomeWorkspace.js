/**
 * Created by caoshuaibiao on 2020/12/8.
 * 桌面
 */
import React from "react";
import { LeftCircleOutlined, RightCircleOutlined } from '@ant-design/icons';
import { Layout, Alert, Carousel, Row, Col, Button } from 'antd';
import DesktopLayout from './DesktopLayout';
import localeHelper from '../../utils/localeHelper';
import { connect } from 'dva';
import SRESearch from "../../components/SRESearch";
import Bus from '../../utils/eventBus';
import properties from "../../properties";
import { debounce } from "lodash";

// const CarouselCompSec = window.CarouselCompSec
@connect(({ home, global }) => ({
    home: home,
    global: global
}))
export default class HomeWorkspace extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            showLayout: false
        }
    }

    componentDidMount() {
        Bus.emit('refreshMenu', true);
        this.setState({
            showLayout: true
        });
    }

    handleNext = (from, to) => {
        const { dispatch } = this.props;
        dispatch({ type: "home/setDesktopIndex", desktopIndex: to });
    };

    onLayoutChange = layout => {
        const { dispatch, home } = this.props;
        const { workspaces, desktopIndex } = home;
        workspaces[desktopIndex].layout = layout;
        // dispatch({type: "home/setWorkspaces", workspaces});
    };

    renderWorkspace = () => {
        let { home } = this.props;
        let { widgetConfig } = this.props;
        let { searchConfig } = widgetConfig;
        const { workspaces, desktopIndex } = home;
        const { currentUser } = this.props.global;
        workspaces.forEach((item, index) => {
            if (index === 0) {
                item.hasSearch = true
            }
        })
        return workspaces.map(workspace => {
            return (
                <div key={workspace.type} style={{ width: '100%', height: 'calc(95vh)' }}>
                    {
                        workspace.hasSearch && searchConfig &&
                        <div className="search-content">
                            <SRESearch sreworksSearchPath={"gateway/v2/foundation/kg"}
                                userEmpId={currentUser.empId}
                                category={`sreworks-search`}
                                className="header-search"
                                includeSearchBtn={true}
                                placeholder={localeHelper.get("common.search.input.tip", "团队、集群、应用、事件、指标...")}
                                customSize={"large"}
                            />
                        </div>
                    }
                    <div className="mian-content">
                        <DesktopLayout onLayoutChange={this.onLayoutChange} layout={workspace.layout} items={workspace.items} type={workspace.hasSearch ? 'main' : workspace.type} />
                    </div>
                </div>
            )
        })
    };

    render() {
        const settings = {
            infinite: true,
            speed: 500,
            appendDots: dots => (
                <div style={{ display: "flex", justifyContent: "center", color: "#fff" }}>
                    <div className="switch-handle"><LeftCircleOutlined className="icon" onClick={() => this.slider.prev()} /></div>
                    <div><ul className="switch-handle-content"> {dots} </ul></div>
                    <div className="switch-handle"><RightCircleOutlined className="icon" onClick={() => this.slider.next()} /></div>
                </div>
            )
        };
        let { home } = this.props;
        const { workspaces } = home;
        let cachedComp = this.renderWorkspace();
        if (workspaces.length === 1) {
            return <div>{this.renderWorkspace()}</div>;
        }
        //length为0会无法默认定位到第一张
        return (
            <div>
                {
                    (workspaces.length !== 0) &&
                    <Carousel beforeChange={this.handleNext} {...settings} ref={slider => (this.slider = slider)}>
                        {cachedComp}
                    </Carousel>
                }
            </div>
        );
    }
}
