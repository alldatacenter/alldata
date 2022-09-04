import React, { Component } from "react";
import { RightOutlined, EllipsisOutlined, QuestionCircleOutlined } from "@ant-design/icons";
import { StatisticCard } from "@ant-design/pro-card";
import { Space, Tooltip } from "antd";
import { TinyLineChart, TinyAreaChart, ProgressChart, LiquidChart, ColumnChart } from "bizcharts";
import _ from 'lodash';
let themeType = localStorage.getItem("sreworks-theme");
const { Statistic } = StatisticCard;

export default class StatusList extends Component {
  render() {
    let { widgetData = {}, widgetConfig = {}, actionParams, ...otherProps } = this.props;
    let { chartPlacement = "bottom", chartTitle = "", tooltip = "", footerList = [], mainList = [], cStyle = { width: 268 }, isOnlyChart = false, total = "", chartConfig = {}, chartData = [] } = widgetConfig;
    let { height = 60, width = 220, autoFit = false, extraConfig, xField, yField, percent, min, max, chartValue } = chartConfig;
    let config = {
      height,
      width,
      autoFit,
      xField,
      yField,
      data: chartData,
      smooth: true,
      percent,
      ...extraConfig,
    };
    let chartRender = <TinyLineChart {...config} />;
    if (chartConfig.type === "TinyLineChart") {
      //折线图
      chartRender = <TinyLineChart {...config} />;
    } else if (chartConfig.type === "TinyAreaChart") {
      //面积图
      chartRender = <TinyAreaChart {...config} />;
    } else if (chartConfig.type === "ProgressChart") {
      //进度条
      chartRender = <ProgressChart {...config} />;
    } else if (chartConfig.type === "ColumnChart") {
      //柱状图
      chartRender = <ColumnChart xAxis={{ label: { visible: false } }} {...config} />;
    } else if (chartConfig.type === "LiquidChart") {
      //水波图
      chartRender = <LiquidChart
        min={min}
        max={max}
        value={chartValue}
        statistic={{
          title: {
            customHtml(container, view, item) {
              return { chartTitle };
            },
          },
          content: {
            style: {
              fill: themeType === "dark" ? "#fff" : "#000",
            },
            customHtml(container, view, item) {
              return `${(item.percent * 100).toFixed(2)}%`;
            },
          },
        }
        }
        {...extraConfig}
      />;
    }
    return <StatisticCard
      chartPlacement={chartPlacement}
      title={
        chartTitle && <Space>
          <span>{chartTitle}</span>
          {tooltip && <em className="optional">
            <Tooltip title={tooltip}>
              <QuestionCircleOutlined style={{ marginRight: 4 }} />
            </Tooltip>
          </em>}
        </Space>
      }
      statistic={!isOnlyChart && {
        value: total,
        prefix: "¥",
        description: (
          <Space>
            {
              mainList.map(item => {
                return <Statistic title={item.label} suffix={item.unit} value={_.get(item.dataIndex, widgetData)} />;
              })
            }
          </Space>
        ),
      }}
      chart={
        chartRender
      }
      footer={
        <div>
          {
            footerList.map(item => {
              return <Statistic title={item.label} suffix={item.unit} value={_.get(item.dataIndex, widgetData)} />;
            })
          }
        </div>
      }
      style={cStyle}
    />;
  }
}