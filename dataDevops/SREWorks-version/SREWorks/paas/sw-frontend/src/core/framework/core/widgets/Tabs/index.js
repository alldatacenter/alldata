/**
 * Created by wangkaihua on 2021/6/8.
 * Tab页
 */
import React, { Component } from "react";
import { Tabs, Icon } from "antd";
import ToolBar from "../../toolbar";
import Block from "../../block";
import _ from "lodash";
import safeEval from "../../../../../utils/SafeEval";
import "./index.less"
const queryString = require('query-string');
const { TabPane } = Tabs;
let UNLISTEN;

export default class TabsRender extends Component {
  constructor(props) {
    super(props);
    this.state = {
      activeKey: '',
      num:1,
    }
  }
  componentDidMount() {
    UNLISTEN = this.props.history && this.props.history.listen(router => {
      this.dealLocationWithInitTab()
    })
    const { widgetConfig = {} } = this.props;
    const { tabPanes } = widgetConfig;
    let tabPnaeKeys = []
    if (tabPanes && tabPanes.length) {
      tabPnaeKeys = tabPanes.map((item) => {
        return item.key
      })
    }
    this.setState({
      activeKey: tabPnaeKeys[0] || '',
    })
  }
  componentWillUnmount() {
    UNLISTEN && UNLISTEN(); // 执行解绑
  }
  dealLocationWithInitTab = () => {
    const { widgetConfig = {} } = this.props;
    const { tabPanes } = widgetConfig;
    let tabPnaeKeys = []
    if (tabPanes && tabPanes.length) {
      tabPnaeKeys = tabPanes.map((item) => {
        return item.key
      })
    }
    let hashString = window.location.hash, searchObj = {};
    if (hashString && hashString.indexOf('?') !== -1) {
      searchObj = queryString.parse(hashString.split("?")[1]);
    }
    if (searchObj.tab && tabPnaeKeys.includes(searchObj.tab)) {
      this.setState({
        activeKey: searchObj.tab
      })
    }
    // else {
    //   this.setState({
    //     activeKey: tabPnaeKeys[0] || '',
    //   })
    // }
  }
  changeActiveKey = (activeKey) => {
    this.setState({ activeKey })
  }
  render() {
    const { widgetConfig } = this.props;
    let toolbarItem = null;
    let { centered, tabPosition, size, title, tabType = "default" } = widgetConfig;
    if (widgetConfig.toolbar && Object.keys(widgetConfig.toolbar).length > 0 && widgetConfig.hasWrapper && widgetConfig.hasWrapper ==='none') {
      toolbarItem = <ToolBar {...this.props} widgetConfig={widgetConfig} />;
    }
    return <div className="tab-panel-widgets">
      <Tabs renderTabBar={(props, DefaultTabBar) => {
        return (
          <div className={tabType === "capsule" ? "tab-capsule" : ""}>
            {tabType === "capsule" &&
              <span style={{ float: "left", marginRight: 5, marginTop: 5, fontSize: 14 }}>{title}</span>}
            <DefaultTabBar {...props} />
          </div>
        )
      }} tabBarExtraContent={toolbarItem} centered={centered} tabPosition={tabPosition} size={size} onChange={this.changeActiveKey} activeKey={this.state.activeKey}>
        {
          (widgetConfig.tabPanes || []).map(item => {
            let disabled = null;
            if (item.disabledExp) {
              let valuesObject = this.props.nodeParams;
              disabled = safeEval(item.disabledExp, valuesObject);
            }
            return <TabPane disabled={disabled} tab={<span>{item.icon && <Icon type={item.icon} />}{item.tab}</span>}
              key={item.key}>
              <div className="tab-content" style={{ marginTop: tabType === "capsule" ? 10 : ""}}>
                <Block {...this.props} widgetConfig={{ block: item.block }} />
              </div>
            </TabPane>;
          })
        }
      </Tabs>
    </div>;
  }
}