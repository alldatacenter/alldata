/**
 * Created by wangkaihua on 2021/05/06.
 * 内嵌型卡片
 */
import React, {Component} from "react";
import {Avatar, Card, Tooltip, Icon} from "antd";
import "./index.less";
import _ from "lodash";

const colors = ["#90ee90", "#2191ee", "#9a69ee", "#41ee1a", "#484aee", "#6B8E23", "#48D1CC", "#3CB371", "#388E8E", "#1874CD"];
const {Meta} = Card;
export default class WelcomeCard extends Component {

  render() {
    let {widgetConfig = {}, actionParams, ...otherProps} = this.props;
    let {flex = 4, steps = []} = widgetConfig;
    return <div className="welcome-card">
      {steps.map((item, index) => {
        return <div className="welcome-card-item" style={{flex: `0 0 ${100 / flex}%`}}>
          <div className="welcome-card-item-icon">
            <div className="icon-wrapper">
              {index + 1}
            </div>
          </div>
          <div className="welcome-card-item-content">
            <h3 style={{margin: "0 0 8px"}}>
              {item.title}
            </h3>
            <p className="description">
              {item.description}
            </p>
            {item.href && item.buttonText &&
            <a className="extra-content" onClick={() => window.open(item.href, "_blank")}>
              {item.buttonText}
            </a>}
          </div>
        </div>;
      })}
    </div>;
  }
}