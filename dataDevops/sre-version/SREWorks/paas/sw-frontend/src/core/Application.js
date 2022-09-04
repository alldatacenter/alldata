/**
 * Created by caoshuaibiao on 18-8-14.
 * 应用主文件
 **/
import React from 'react';
import { connect } from 'dva';
import { withRouter } from 'dva/router';
import { Spin, Alert, Popover, message, Button, Modal, Space } from "antd";
import DefaultLayout from '../layouts/DefaultLayout';
import properties from 'appRoot/properties';
import ABMLoading from '../components/ABMLoading';
import Login from '../components/Login';
import Home from '../layouts/Home';
import * as util from './../utils/utils';
import cacheRepository from './../utils/cacheRepository';
import JsonEditor from '../components/JsonEditor';
import MenuTreeService from './services/appMenuTreeService'

@connect(({ node, global }) => ({
  nodeParams: node.nodeParams,
  actionParams: node.actionParams,
  remoteComp: global.remoteComp
}))
class Application extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      reloadKey: (new Date()).getTime(),
      isModalVisible: false
    }
  }
  componentWillMount() {
    this.loadRemoteComp();
  }
  componentDidMount() {
    const { dispatch, routes, app, global } = this.props;
    global.app = app;
    dispatch({ type: 'global/hookRoutes', payload: { routes: routes } });
    // 初始化全局配置平台名称和logo
    this.setHeader()
  }
  setHeader() {
    const { platformLogo, platformName } = properties;
    var link = document.querySelector("link[rel*='icon']") || document.createElement('link');
    link.type = 'image/x-icon';
    link.rel = 'shortcut icon';
    link.href = platformLogo;
    document.getElementsByTagName('head')[0].appendChild(link);
    document.title = platformName;
  }
  loadRemoteComp = async () => {
    try {
      let compList = await MenuTreeService.getCustomList();
      let remoteComList = compList.filter(item => item.configObject.componentType === 'UMD');
      let pros = [], recievedList = [];
      remoteComList.forEach(item => {
        if (item.configObject && item.configObject.umdUrl) {
          pros.push(Promise.resolve(window.System.import(item.configObject.umdUrl)))
          recievedList.push(item.name)
        }
      })
      let remoteComp = await Promise.all(pros);
      window['REMOTE_COMP_LIST'] = recievedList
    } catch (error) {
      message.info('获取远程组件列表失败')
    }
  }
  getEnvLabel = () => {
    let bizApp = util.getNewBizApp();
    if (bizApp) {
      let env = bizApp.split(",");
      let envMap = {
        "dev": "开发环境",
        "pre": "预发环境",
        "prod": "正式环境",
      };
      if (env[2] === "prod") {
        return null;
      } else {
        return envMap[env[2]];
      }
    } else {
      return null;
    }
  };
  render() {
    const { global, nodeParams, actionParams } = this.props;
    const { fakeGlobal, currentUser, logined, loading, currentProduct, } = global;
    let { isModalVisible, reloadKey } = this.state;
    if (!logined) {
      return <Login />;
    }
    if (!currentUser || !currentUser.empId || !currentProduct || loading) {
      return <ABMLoading />
    }

    let path = window.location.hash;
    if (properties.envFlag === properties.ENV.PaaS
      && (path.endsWith("#/") || path.endsWith("#"))
      && !properties.defaultProduct
    ) {
      return <Home />;
    }
    let content = (
      <Space>
        <Popover
          placement="topLeft"
          trigger="click"
          title="节点参数集合"
          content={
            <JsonEditor key={reloadKey} json={Object.assign({}, nodeParams, { __currentUser__: undefined })} readOnly={true} style={{ height: '80vh', width: '50vw' }} />
          }
        >
          <Button style={{ position: 'relative', bottom: 6 }} size="small" type="primary" >
            节点参数集合
          </Button>
        </Popover>
        <Popover
          placement="topLeft"
          trigger="click"
          title="Action参数集合"
          content={
            <JsonEditor key={reloadKey} json={Object.assign({}, actionParams, { __currentUser__: undefined })} readOnly={true} style={{ height: '80vh', width: '50vw' }} />
          }
        >
          <Button style={{ position: 'relative', bottom: 6 }} size="small" type="primary" >
            Action参数集合
          </Button>
        </Popover>
      </Space>
    )
    return (
      <div>
        {this.getEnvLabel() && <div style={{
          position: "fixed",
          zIndex: 10000,
          bottom: 10,
          left: 10,
        }}>
          <Alert
            message={<div>{this.getEnvLabel()}</div>}
            description={<div>
              非生产Prod环境，仅供测试开发使用！请前往<a onClick={(e) => {
                let bizApp = util.getNewBizApp(), app = window.location.hash.split("/")[1], namespaceId = properties.defaultNamespace, stageId = "prod";
                if (bizApp) {
                  let env = bizApp.split(",");
                  app = env[0], namespaceId = env[1], stageId = "prod";
                }
                cacheRepository.setAppBizId(app, namespaceId, stageId);
                window.location.reload()
              }}> 正式环境</a>
            </div>}
            type="warning"
            showIcon
            action={
              content
            }
            closable
          />
        </div>}
        <Spin tip="Loading..." size="large" spinning={fakeGlobal}>
          <DefaultLayout {...this.props} />
        </Spin>
      </div>
    );
  }
}

export default withRouter(connect(({ global }) => ({ global }))(Application));
