/**
 * Created by wangkaihua on 2021/05/07.
 * 时间线
 */
import React, { Component } from "react";
import { Steps } from "antd";
import moment from "moment";
import JSXRender from "../../JSXRender";
import _ from "lodash";
import * as util from "../../../../../utils/utils";
const { Step } = Steps;
export default class StepBar extends Component {
  render() {
    let { widgetData = [], widgetConfig = {}, actionParams, ...otherProps } = this.props;
    let {} = widgetConfig;
    let Qdom = (
      <div>
        <div>曲丽丽</div>
        <div>2016-12-12 12:32</div>
      </div>
    )
    return <div></div>
    // return <Steps size="small" current={1}>
    //   <Step title="创建项目" description="曲丽丽" />
    //   <Step title="部门初审" subTitle="" description={<JSXRender jsx={Qdom}/>} />
    //   <Step title="财务复核" description="" />
    //   <Step title="完成" description="" />
    // </Steps>
  }
}