/**
 * Created by wangkaihua on 2021/05/06.
 * 复杂列表
 */
import React, { Component } from "react";
import { List, Avatar, Popover, Card, Pagination } from "antd";
import JSXRender from "../../JSXRender";
import './index.less'
import DefaultItemToolbar from "../../DefaultItemToolbar";
import _ from "lodash"
import * as util from "../../../../../utils/utils"
const colors = ['#90ee90', '#2191ee', '#9a69ee', '#41ee1a', '#484aee', '#6B8E23', '#48D1CC', '#3CB371', '#388E8E', '#1874CD'];

export default class ListRender extends Component {
  constructor(props) {
    super(props);
    let { widgetData = [] } = props;
    if (props.widgetData && props.widgetData.items) {
      widgetData = widgetData.items;
    }
    this.state = {
      pageSize: 20,
      total: (widgetData && widgetData.length) || 0,
      currentPage: 1,
      splitData: widgetData
    }
  }
  componentWillMount() {
    let { widgetConfig = {} } = this.props;
    let { widgetData = [] } = this.props;
    if (this.props.widgetData && this.props.widgetData.items) {
      widgetData = widgetData.items;
    }
    if (widgetConfig.dataSourceMeta && widgetConfig.dataSourceMeta.paging) {
      this.setState({
        splitData: widgetData.slice(0, this.state.pageSize)
      })
    } else {
      this.setState({
        splitData: widgetData
      })
    }
  }
  componentWillReceiveProps(nextProps) {
    let { widgetConfig = {} } = this.props;
    let { widgetData = [] } = nextProps
    if (this.props.widgetData && this.props.widgetData.items) {
      widgetData = widgetData.items;
    }
    if (!_.isEqual(nextProps.widgetData, this.props.widgetData)) {
      if (widgetConfig.dataSourceMeta && widgetConfig.dataSourceMeta.paging) {
        this.setState({
          splitData: widgetData.slice(0, this.state.pageSize),
          currentPage: 1,
          total: (widgetData && widgetData.length) || 0
        })
      } else {
        this.setState({
          splitData: widgetData,
          total: (widgetData && widgetData.length) || 0
        })
      }
    }
  }
  onChange = page => {
    this.setState({ currentPage: page }, () => {
      this.formatData()
    })
  }
  formatData = () => {
    let { widgetData = [] } = this.props;
    if (this.props.widgetData && this.props.widgetData.items) {
      widgetData = widgetData.items;
    }
    let { currentPage, pageSize } = this.state;
    this.setState({
      splitData: widgetData.slice((currentPage - 1) * pageSize, currentPage * pageSize)
    })
  }
  onShowSizeChange = (current, pageSize) => {
    this.setState({ currentPage: 1, pageSize }, () => {
      this.formatData()
    })
  }
  render() {
    let { widgetData = [], widgetConfig = {}, actionParams, ...otherProps } = this.props;
    let { bordered, header, footer, listItem, itemLayout, itemToolbar, columns = [], emptyText = '', minHeight } = widgetConfig;
    let { title, description, avatar, extra, href, content } = listItem;
    let { total, pageSize, currentPage, splitData } = this.state;
    return <div style={{ width: '100%', minHeight }}>
      <List
        bordered={bordered}
        locale={{
          emptyText: emptyText ? <JSXRender  {...this.props} jsx={emptyText} /> : null
        }}
        itemLayout={itemLayout}
        className="card-list-ant"
        dataSource={splitData || []}
        footer={footer ? <JSXRender  {...this.props} jsx={footer} /> : null}
        header={header ? <JSXRender  {...this.props} jsx={header} /> : null}
        renderItem={(item, index) => {
          let title = item[widgetConfig.title] || item.title;
          let icon = item[widgetConfig.icon] || item.icon;
          let description = item[widgetConfig.description] || item.description;
          if (widgetConfig && widgetConfig['listItem']) {
            title = item[widgetConfig.listItem.title] || item.title;
            icon = item[widgetConfig.listItem.icon] || item.icon;
            description = item[widgetConfig.listItem.description] || item.description;
          }
          if (widgetConfig && widgetConfig['listItem']) {
            icon = item[widgetConfig.listItem.icon] || item.icon;
          }
          let avatar = icon ? <Avatar src={icon} /> : <Avatar style={{
            backgroundColor: colors[index % 10],
            verticalAlign: 'middle',
            fontSize: '18px',
            marginLeft: '8px'
          }}>
            {title && (title).substring(0, 1)}
          </Avatar>;
          return <List.Item
            key={index}
            actions={[itemToolbar && <DefaultItemToolbar {...otherProps} itemToolbar={itemToolbar} itemData={item} actionParams={Object.assign({}, actionParams, item)} />]}
            extra={extra}
          >
            <List.Item.Meta
              avatar={avatar}
              title={title}
              description={description}
            />
            {/* {item[content] ? <JSXRender  {...this.props} jsx={item[content]}/> : null} */}
            <div className="columns-list" style={{ width: `calc(100% - ${widgetConfig.width || '300px'})` }}>
              {columns && !!columns.length && columns.map((column) => {
                if (column.render && typeof column.render === 'string') {
                  return <JSXRender key={column.dataIndex} jsx={util.renderTemplateString(column.render)} />
                } else {
                  return <div key={column.dataIndex} className={column.width ? "description-width" : "description-wrap"}
                    style={{ width: column.width }}>
                    <div className="description-item">
                      <div className="description-item-title">{column.label}</div>
                      <div className="description-item-value">
                        {/* <Popover content={item[column.dataIndex] || '-'}>
                          {item[column.dataIndex] || '-'}
                        </Popover> */}
                        {
                          column.isLink ? <a href={'#/' + item[column.dataIndex]} target='_blank'>{item[column.dataIndex]}</a> :
                            <Popover content={item[column.dataIndex] || '-'}>
                              {item[column.dataIndex] || '-'}
                            </Popover>
                        }
                      </div>
                    </div>
                  </div>
                }
              })}
            </div>
          </List.Item>;
        }}
      />
      {widgetConfig.dataSourceMeta && widgetConfig.dataSourceMeta.paging && <Pagination style={{ textAlign: 'right', marginTop: 10 }}
        showSizeChanger
        showQuickJumper
        showTotal={total => `共 ${total} 条`}
        defaultPageSize={pageSize}
        onShowSizeChange={this.onShowSizeChange}
        defaultCurrent={currentPage}
        total={this.state.total}
        onChange={this.onChange} />}
    </div>
      ;
  }
}