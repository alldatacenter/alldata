/**
 * Created by wangkaihua on 2021/05/17.
 * 排名列表
 */
import React, { Component } from "react";
import { List, Avatar, Tooltip, Statistic, Timeline } from "antd";
import "./index.less";
import _ from "lodash";

export default class StatusList extends Component {

  render() {
    let { widgetData = [], widgetConfig = {}, actionParams, ...otherProps } = this.props;
    let { warningExp, successExp, processExp, defaultExp, toolTip, href } = widgetConfig;
    return <div className="status-list">
      {(widgetData || []).map(item => {
        let href = href && util.renderTemplateString(href, { row: item });
        let row = item;
        let style = null;
        if (eval(warningExp)) {
          style = { backgroundColor: "#a8071a" };
        } else if (eval(successExp)) {
          style = { backgroundColor: "#5b8c00" };
        } else if (eval(processExp)) {
          style = { backgroundColor: "#096dd9" };
        } else if (eval(defaultExp)) {
          style = null;
        }
        return <Tooltip title={_.get(item, toolTip)}>{href ? <a href={href}>
          <Avatar style={style} className="status-item" />
        </a> : <Avatar style={style} className="status-item" />}</Tooltip>;
      })}
    </div>;
  }
};