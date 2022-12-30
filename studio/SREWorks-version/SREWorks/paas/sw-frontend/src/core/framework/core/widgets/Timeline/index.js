/**
 * Created by wangkaihua on 2021/05/07.
 * 时间线
 */
import React, { Component } from "react";
import { Timeline, Tag } from "antd";
import moment from "moment";
import JSXRender from "../../JSXRender";
import _ from "lodash";
import * as util from "../../../../../utils/utils";
import DefaultItemToolbar from "../../DefaultItemToolbar";

export default class TimelineRender extends Component {
  render() {
    let { widgetData = [], widgetConfig = {}, actionParams, ...otherProps } = this.props;
    let {
      mode, itemObject = {},
      itemToolbar,
      innerPosition = 'left'
    } = widgetConfig;
    let { date, dateFormat = "YYYY-MM-DD", titleTime, tagColorMap = {}, circleMap = {}, tag, color, description } = itemObject;
    if (widgetData && widgetData.length === 0) {
      return <div style={{ textAlign: 'center', padding: 10 }}>暂无数据</div>
    }
    return <div style={{ padding: 10, textAlign: innerPosition, marginLeft: innerPosition === 'left' ? -40 : '' }}>
      <Timeline mode={mode}>
        {
          (widgetData || []).map(item => {
            return <Timeline.Item label={moment(String(_.get(item, date)).length > 12 ? _.get(item, date) : _.get(item, date) * 1000).format(dateFormat)} color={circleMap[_.get(item, color)]}>
              {tag && <Tag color={tagColorMap[_.get(item, tag)]}>{_.get(item, tag)}</Tag>}
              {titleTime && <JSXRender {...this.props} jsx={util.renderTemplateString(titleTime, { row: item })} />}
              <div style={{ fontSize: 13 }}>
                {description && <JSXRender {...this.props} jsx={util.renderTemplateString(description, { row: item })} />}
              </div>
              {itemToolbar && <div style={{ marginTop: 5 }}>
                <DefaultItemToolbar {...otherProps} itemToolbar={itemToolbar} itemData={item}
                  actionParams={Object.assign({}, actionParams, item)} />
              </div>}
            </Timeline.Item>;
          })
        }
      </Timeline>
    </div>;
  }
}