/**
 * Created by wangkaihua on 2021/05/17.
 * 排名列表
 */
import React, { Component } from "react";
import { List, Avatar, Tooltip, Statistic, Timeline } from "antd";
import _ from "lodash";
import * as util from "../../../../../utils/utils";
import DefaultItemToolbar from "../../DefaultItemToolbar";
import "./index.less";

const colors = ["#90ee90", "#2191ee", "#9a69ee", "#41ee1a", "#484aee", "#6B8E23", "#48D1CC", "#3CB371", "#388E8E", "#1874CD"];

export default class RankingList extends Component {

  render() {
    let { widgetData = [], widgetConfig = {}, actionParams, ...otherProps } = this.props;
    let { label, value, sort, largest, href, importCount, importColor, backupValue, customTooltip, itemToolbar,minHeight } = widgetConfig;
    return <div className="ranking-list" style={{minHeight:minHeight}}>
      {
        (widgetData || []).sort((a, b) => a[sort] - b[sort]).slice(0, largest || 5).map((item, index) => {
          let href = href && util.renderTemplateString(href, { row: item });
          return <div className="ranking-item">
            <div className="rank">
              <Avatar size="small" style={index < importCount && { backgroundColor: importColor || "#ffc53d" }}>
                {index + 1}
              </Avatar>
            </div>
            <div className="label">
              {href ? <a href={href}><Tooltip title={_.get(item, customTooltip ? customTooltip : label)}>{_.get(item, label)}</Tooltip></a> :
                <Tooltip title={_.get(item, customTooltip ? customTooltip : label)}>{_.get(item, label)}</Tooltip>}
            </div>
            <div className="rank-value">
              {_.get(item, value)}
            </div>
            {
              backupValue && <div className="rank-value">
                {_.get(item, backupValue)}
              </div>
            }
            {itemToolbar && !!itemToolbar.actionList.length && <div className="action">
              <DefaultItemToolbar {...otherProps} itemToolbar={itemToolbar} itemData={item}
                actionParams={Object.assign({}, actionParams, item)} />
            </div>}
          </div>;
        })
      }
    </div>;
  }
};