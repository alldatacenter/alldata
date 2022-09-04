/**
 * Created by wangkaihua on 2021/05/06.
 * 内嵌型卡片
 */
import React, { Component } from "react";
import { Avatar, Card, Tooltip } from "antd";
import "./index.less";
import _ from "lodash";

const colors = ["#90ee90", "#2191ee", "#9a69ee", "#41ee1a", "#484aee", "#6B8E23", "#48D1CC", "#3CB371", "#388E8E", "#1874CD"];
const { Meta } = Card;
export default class CardRender extends Component {

  render() {
    let { widgetData = [], widgetConfig = {}, actionParams, ...otherProps } = this.props;
    let { listItem, flex = 6 } = widgetConfig;
    let { title, description, avatar, auth, date } = listItem;
    const gridStyle = {
      width: 100 / flex + "%",
      padding: 20,
      textAlign: "left",
      height: 156,
    };
    return <Card className="abm-widgets-card">
      {
        (widgetData || []).map((item, index) => {
          let avatarRender = _.get(item, avatar) ? <Avatar src={_.get(item, avatar)} /> : <Avatar style={{
            backgroundColor: colors[index % 10],
            verticalAlign: "middle",
            fontSize: "18px",
          }}>
            {_.get(item, title) && (_.get(item, title) || item.title).substring(0, 1)}
          </Avatar>;
          return <Card.Grid style={gridStyle}>
            <div className="card-title">
              {avatarRender}
              <div className="card-item-text">
                <span>{_.get(item, title)}</span>
              </div>
            </div>
            <Meta
              description={
                <div className="card-description">
                  <div className="card-description-text">
                    <Tooltip title={_.get(item, description)}>{_.get(item, description)}</Tooltip>
                  </div>
                  <div className="card-extra-mess">
                    <div className="auth">
                      {_.get(item, auth)}
                    </div>
                    <div className="date">
                      {_.get(item, date)}
                    </div>
                  </div>
                </div>
              }
            />
          </Card.Grid>;
        })
      }
    </Card>;
  }
}