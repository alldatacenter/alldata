/**
 * 描述列表
 */
import React, {Component} from "react";
import {QuestionCircleOutlined} from "@ant-design/icons";
import {Descriptions, Tooltip, Row, Col} from "antd";
import JSXRender from "../../JSXRender";
import _ from "lodash";
import DefaultItemToolbar from "../../DefaultItemToolbar";

const colors = ["#90ee90", "#2191ee", "#9a69ee", "#41ee1a", "#484aee", "#6B8E23", "#48D1CC", "#3CB371", "#388E8E", "#1874CD"];

export default class StatusSummary extends Component {

  render() {
    let {widgetData = {}, widgetConfig = {}, actionParams, ...otherProps} = this.props;
    let {itemToolbar, formatList, statusList, segmentTitle,minHeight} = widgetConfig;
    let dataList = [];
    statusList && statusList.map(item => {
      dataList.push({
        value: _.get(widgetData, item.dataIndex),
        label: item.label,
        color: item.color,
      });
    });
    dataList = dataList.sort((a, b) => b.value - a.value);
    return (
      <div style={{padding: 10,minHeight:minHeight}}>
        <Row gutter={8}>
          {formatList && formatList.map((item) => {
            let itemRender = _.get(widgetData, item.dataIndex) + (item.unit || '');
            if (item.href) {
              itemRender = <a href={item.href}>{_.get(widgetData, item.dataIndex)}{item.unit || ''}</a>;
            } else if (item.render) {
              itemRender = <JSXRender  {...this.props} jsx={item.render}/>;
            }
            return <Col span={24 / formatList.length}>
              <p style={{color: "#ccc", marginBottom: 8, fontSize: 10}}>{item.label}</p>
              <p style={{fontSize: 20}}>{itemRender || '-'}</p>
            </Col>;
          })}

        </Row>
        <div style={{display: "flex"}}>
          <div style={{height: 8, marginRight: 10}}>{segmentTitle || "图表标题"}</div>
          {dataList.map((t, i) => <div style={{
            background: t.color || colors[i % colors.length],
            display: "inline-block",
            flex: t.value,
            height: 8,
            marginTop: 7,
          }} key={i}/>)}
        </div>
        <div style={{
          display: "grid",
          marginTop: 10,
          gridTemplateColumns: dataList.map(() => 100 / dataList.length + "%").join(" "),
        }}>
          {dataList.map((t, i) => <div key={i}
                                       style={{display: "flex", justifyContent: "center", alignItems: "center"}}>
                        <span style={{
                          width: 6,
                          height: 6,
                          display: "inline-block",
                          borderRadius: "50%",
                          background: t.color || colors[i % colors.length],
                        }}/>
            <span style={{display: "inline-blick", marginLeft: 4}}>
                            {t.label}: {t.value || 0}%
                        </span>
          </div>)}
        </div>
      </div>
    );
  }
}