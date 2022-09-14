/**
 * Created by caoshuaibiao on 2020/12/9.
 */
import React from "react";
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { AppstoreOutlined } from '@ant-design/icons';
import { Drawer, Menu, Avatar } from "antd";
import NavSelectPanel from './NavSelectPanel';
import './index.less';
import { connect } from "dva";
import * as util from '../../utils/utils';
import httpClient from '../../utils/httpClient';
import Bus from '../../utils/eventBus';
import AppService from "../../core/services/appService";
import properties from "../../properties";

const colors = ['#90ee90', '#2191ee', '#9a69ee', '#41ee1a', '#484aee', '#6B8E23', '#48D1CC', '#3CB371', '#388E8E', '#1874CD'];

const { SubMenu } = Menu;
@connect(({ home }) => ({
  home: home
}))
export default class Desktop extends React.Component {

  state = { visible: false, placement: 'left' };

  switchNavVisible = () => {
    if(!this.state.visible) {
      this.freshNavMenu()
    }
    this.setState({
      visible: !this.state.visible,
    });
  };
  freshNavMenu=()=> {
    let params = {
      namespaceId: util.getNewBizApp().split(",")[1],
      stageId: util.getNewBizApp().split(",")[2],
      visit: true,
      pagination: false,
    };
    const { dispatch, home } = this.props;
    const { collectList, appStore, workspaces } = home;
    AppService.getAllProfile(params)
      .then(res => {
        let { collectList = [], customQuickList = [], workspaces } = res.profile;
        dispatch({ type: "home/setAppStore", appStore: res.appStore || [] });
        dispatch({ type: "home/setDesktopData", workspaces, collectList, customQuickList, isEqual: res.isEqual });
      });
  }
  onClose = () => {
    this.setState({
      visible: false,
    });
  };

  UNSAFE_componentWillMount() {
    Bus.on('refreshMenu', (msg) => {
      this.freshNavMenu()
  });
  }
  handleMouseEnter = (item) => {
    document.getElementById("close-icon-" + item.appId).style.display = "inline-block";
  };

  handleMouseLeave = item => {
    document.getElementById("close-icon-" + item.appId).style.display = "none";
  };

  deleteCollect = (e, index) => {
    const { dispatch, home } = this.props;
    const { collectList, workspaces, customQuickList } = home;
    e.stopPropagation();
    collectList.splice(index, 1);
    dispatch({ type: "home/setDesktopData", collectList, workspaces, customQuickList });
  };

  handleClick = (e, item) => {
    if (item.appId) {
      window.open("#/" + item.appId + `?namespaceId=${item.namespaceId || properties.defaultNamespace}&stageId=${item.stageId || properties.defaultStageId}`);
    } else if (item.href) {
      window.open(item.href)
    }
  };

  render() {

    const { home, top, theme } = this.props;
    const { collectList } = home;
    return (
      <div className="abm-toggle-menu-bar" style={theme === 'light' ? { background: "#fffffd" } : { background: "#2c2f40" }}>
        <div onClick={this.switchNavVisible}>
          <LegacyIcon type={this.props.icon || "menu"} style={this.props.cStyle || {
            height: 48,
            lineHeight: "25px",
          }} className="menu-icon" />
        </div>
        <Drawer
          placement={this.state.placement}
          closable={false}
          onClose={this.onClose}
          visible={this.state.visible}
          width={220}
          style={{ marginTop: top || 48, height: "calc(100% - 50px)" }}
          bodyStyle={
            this.props.theme !== "dark" ? { top: 0, padding: 0 } : {background: "#001529", color: "#fff",  top: 0, padding: 0 }
          }
        >
          <div className="abm-toggle-menu-sider">
            <Menu style={{ width: 210 }}
              mode="vertical"
              theme={this.props.theme || 'dark'}
              selectedKeys={["apps"]}
              triggerSubMenuAction="hover"
            >
              <SubMenu key="apps"
                title={
                  <span><AppstoreOutlined /><span style={{ color: this.props.theme === "dark" ? "#fff" : "", paddingLeft: 5, fontSize: 12 }}>全部</span></span>
                }
                popupOffset={[0, -4]}

              >
                <div style={{ height: '80vh' }} className={this.props.theme === "dark" ? "abm-toggle-menu-bar-product-nav-content abm-toggle-menu-bar-product-nav-content-dark" : "abm-toggle-menu-bar-product-nav-content abm-toggle-menu-bar-product-nav-content-light"}>
                  <NavSelectPanel />
                </div>

              </SubMenu>
              {
                collectList.map((item, i) => {
                  let IconRender = null;
                  if (item.logoUrl) {
                    IconRender = <img style={{
                      position: "absolute",
                      top: "50%",
                      transform: "translateY(-50%)",
                      width: 28,
                      height: 28
                    }} src={item.logoUrl} />
                  } else {
                    IconRender = <Avatar size={18} style={{
                      backgroundColor: colors[i % 10],
                      position: "absolute",
                      top: "50%",
                      transform: "translateY(-50%)",
                      fontSize: '14px'
                    }}>
                      {item.appName && (item.appName).substring(0, 1)}
                    </Avatar>
                  }
                  return <Menu.Item key={item.appId || item.id}>
                    <div id={"menu_" + (item.appId || item.id)}
                      onClick={e => this.handleClick(e, item)}>
                      {IconRender}
                      <span style={{ paddingLeft: 30, paddingRight: 30, fontSize: 12, color: this.props.theme === "dark" ? "#fff" : "" }}>{item.appName}</span>
                    </div>
                  </Menu.Item>;
                })
              }
            </Menu>
          </div>
        </Drawer>
      </div>
    );
  }
}