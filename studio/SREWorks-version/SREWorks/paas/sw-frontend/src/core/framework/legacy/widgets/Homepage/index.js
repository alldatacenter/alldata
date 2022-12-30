/**
* @params
* config中可配置
*  @stageId
 * @namespaceId
* */

import HomeLayout from "../../../../../layouts/Home";
import React from 'react';
import { Component } from "react";
import { connect } from "dva";
import * as utils from "../../../../../utils/utils";
@connect(({ global }) => ({
  currentProduct: global.currentProduct,
  currentUser: global.currentUser,
  theme: global.theme
}))
export default class OPSDesktop extends Component {
  constructor(props) {
    super(props);
    this.state = {
      stageId: "",
      namespaceId: "",
      api: null,
    };
  }

  getNodeParams = () => {
    let { parameters, nodeParams, rowData = {}, widgetDefaultParams = {}, formInitParams = {} } = this.props;
    return Object.assign({}, formInitParams, widgetDefaultParams, parameters, nodeParams, rowData);
  };


  getConf = () => {
    let config = this.props.mode.config, nodeParams = this.getNodeParams();
    return utils.renderTemplateJsonObject(config, nodeParams);
  };

  componentDidMount() {
    let config = this.getConf();
    this.setState({
      stageId: config.stageId,
      namespaceId: config.namespaceId,
      api: config.api,
    });
  }

  render() {
    let { namespaceId, stageId, api } = this.state;
    return <HomeLayout {...this.props} stageId={stageId} namespaceId={namespaceId} api={api} />;
  }
}