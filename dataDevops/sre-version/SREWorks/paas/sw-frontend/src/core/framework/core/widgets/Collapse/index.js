/**
 * Created by wangkaihua on 2021/06/28.
 * Tab页
 */
import React, { Component } from "react";
import { Collapse } from "antd";
import Block from "../../block";
import _ from "lodash";
import safeEval from "../../../../../utils/SafeEval";

const { Panel } = Collapse;

export default class TabsRender extends Component {
  render() {
    const { widgetConfig } = this.props;
    let { accordion, bordered, expandIconPosition } = widgetConfig;
    return <Collapse accordion={accordion} bordered={bordered} expandIconPosition={expandIconPosition}>
      {
        (widgetConfig.panes || []).map(item => {
          let disabled = null;
          if (item.disabledExp) {
            let valuesObject = this.props.nodeParams;
            disabled = safeEval(item.disabledExp, valuesObject);
          }
          let showArrow = true;
          if (item.showArrowExp) {
            let valuesObject = this.props.nodeParams;
            showArrow = safeEval(item.showArrowExp, valuesObject);
          }
          return <Panel showArrow={showArrow} header={item.title} key={item.key} disabled={disabled}>
            <Block {...this.props} widgetConfig={{ block: item.block }} />
          </Panel>;
        })
      }
    </Collapse>;
  }
}