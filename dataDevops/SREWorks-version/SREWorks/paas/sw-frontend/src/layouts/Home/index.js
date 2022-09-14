/**
 * Created by caoshuaibiao on 2020/12/8.
 * 主页
 */
import React from 'react';
import { Layout, Alert, Button, Spin } from 'antd';
import { connect } from 'dva';
import * as util from "../../utils/utils";
import HomeHeader from './HomeHeader';
import HomeWorkspace from './HomeWorkspace';
import WorkspaceSetting from './WorkspaceSetting';
import AppService from '../../core/services/appService'
import './index.less';
import _ from "lodash";
import properties from '../../properties';
import { urlToRequest } from 'loader-utils';
import { localImglist, requiredImglist } from "./localImglist";

const { Content } = Layout;

@connect(({ home }) => ({
  home: home
}))
class HomeLayout extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      loading: false,
    };
    const { global } = this.props;
  }

  componentWillMount() {
    // this.freshNavMenu()
  }

  componentWillReceiveProps(nextProps, nextContext) {
    if (!_.isEqual(this.props.params, nextProps.params)) {
      const { dispatch, api } = nextProps;
      let params = {
        namespaceId: util.getNewBizApp().split(",")[1],
        stageId: util.getNewBizApp().split(",")[2],
        visit: true,
        pagination: false,
      };
      this.getImageList();
      this.setState({ loading: true });
      AppService.getAllProfile(params)
        .then(res => {
          this.setState({ loading: false });
          let { collectList, customQuickList, workspaces } = res.profile;
          dispatch({ type: "home/setAppStore", appStore: res.appStore || [] });
          dispatch({ type: "home/setDesktopData", workspaces, collectList, customQuickList, isEqual: res.isEqual });
        });
    }
  }
  freshNavMenu=()=> {
    const { dispatch, api } = this.props;
    let params = {
      namespaceId: util.getNewBizApp().split(",")[1],
      stageId: util.getNewBizApp().split(",")[2],
      visit: true,
      pagination: false,
    };
    this.getImageList();
    this.setState({ loading: true });
    AppService.getAllProfile(params)
      .then(res => {
        this.setState({ loading: false });
        let { collectList, customQuickList, workspaces } = res.profile;
        dispatch({ type: "home/setAppStore", appStore: res.appStore || [] });
        dispatch({ type: "home/setDesktopData", workspaces, collectList, customQuickList, isEqual: res.isEqual });
      });
  }
  getImageList = () => {
    const { dispatch } = this.props;
    AppService.getDesktopBackgroundList().then(res => {
      let imgList = res.imgList || []
      this.setState({ imgList });
      dispatch({
        type: 'home/setImgList',
        imgList
      })
    })
  };
  onLogout = () => {
    const { dispatch } = this.props;
    dispatch({ type: 'global/logout' });
  };

  render() {
    const { global, children, location, app, home,widgetConfig={} } = this.props;
    const { workspaces, desktopIndex, imgList } = home;
    let backgroundImgeUrl = ''
    if (workspaces[desktopIndex] && workspaces[desktopIndex].background) {
      if (workspaces[desktopIndex].background.indexOf('//') > -1) {
        backgroundImgeUrl = workspaces[desktopIndex].background
      } else {
        backgroundImgeUrl = localImglist.includes(workspaces[desktopIndex].background) ? requiredImglist[localImglist.indexOf(workspaces[desktopIndex].background)] : (properties.baseUrl + workspaces[desktopIndex].background)
      }
    } else {
      backgroundImgeUrl = requiredImglist[0];
    }
    if(!backgroundImgeUrl) {
      backgroundImgeUrl = requiredImglist[0]
    }
    let { loading } = this.state;
    return (
      <div className="abm-home-page" style={{ backgroundImage: `url(${backgroundImgeUrl})`, backgroundPosition: 'center bottom', backgroundRepeat: 'no-repeat', backgroundColor: 'rgb(51, 51, 51)',backgroundSize:"cover" }} >
        <Layout style={{ background: "transparent" }}>
          <HomeHeader />
          <Layout style={{ background: "transparent" }}>
            <Content className="workspace">
              <Spin spinning={loading}>{!loading && <HomeWorkspace widgetConfig={widgetConfig}/>}</Spin>
              <WorkspaceSetting />
            </Content>
          </Layout>
        </Layout>
      </div>
    );
  }
}

export default HomeLayout;