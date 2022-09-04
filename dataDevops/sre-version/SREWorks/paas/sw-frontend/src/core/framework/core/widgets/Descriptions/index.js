/**
 * 描述列表
 */
import React, { Component } from "react";
import { QuestionCircleOutlined } from "@ant-design/icons";
import { Descriptions, Tooltip } from "antd";
import JSXRender from "../../JSXRender";
import _ from "lodash";
import DefaultItemToolbar from "../../DefaultItemToolbar";

export default class DescriptionList extends Component {

  render() {
    let { widgetData = {}, widgetConfig = {}, actionParams, ...otherProps } = this.props;
    let { itemToolbar, formatList, describeTitle, bordered, size, column, colon, layout, labelStyle = {}, descriptionStyle = {},minHeight } = widgetConfig;
    return (
      <Descriptions colon={colon} title={describeTitle||''} layout={layout} bordered={bordered} size={size || "small"} style={{minHeight:minHeight,margin:22}}
      >
        {
          formatList.map((item, index) => {
            let labelRender = item.label;
            if (item.description) {
              labelRender =
                <span>{item.label}
                  <em className="optional">
                    <Tooltip title={item.description}><QuestionCircleOutlined />
                    </Tooltip>
                  </em>
                </span>;
            }
            let itemRender = '';
            if (item.href) {
              itemRender = <a href={item.href}>{_.get(widgetData, item['dataIndex'])}</a>;
            } else if (item.render) {
              itemRender = <JSXRender  {...this.props} jsx={item.render} />;
            } else {
              itemRender = _.get(widgetData, item['dataIndex'])
              if (typeof itemRender === 'boolean') {
                itemRender = itemRender === true ? '是' : '否'
              }
            }
            return <Descriptions.Item span={Number(item.span) || 1}
              key={item.dataIndex}
              label={<span style={labelStyle}>{labelRender}</span>}><span
                style={descriptionStyle}>{itemRender}</span></Descriptions.Item>;
          })
        }
        {itemToolbar && itemToolbar.actionList && !!itemToolbar.actionList.length && <div style={{ marginTop: 5 }}>
          <DefaultItemToolbar {...otherProps} itemToolbar={itemToolbar} itemData={widgetData}
            actionParams={Object.assign({}, actionParams, widgetData)} />
        </div>}
      </Descriptions>
    );
  }
}