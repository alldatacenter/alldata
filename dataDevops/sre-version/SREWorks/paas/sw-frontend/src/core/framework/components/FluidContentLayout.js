/**
 * @author caoshuaibiao
 * @date 2021/7/6 11:43
 * @Description:流式内容布局展示
 */
import React, { Component } from "react";
import { ReactSortable } from "react-sortablejs";
import { Button, Col, Row } from 'antd';
import { PlusOutlined, CloseOutlined, CopyOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import WidgetModel from "../model/WidgetModel";
import Constants from "../model/Constants";
import WidgetCard from "../core/WidgetCard";

import './index.less';


class FluidContentLayout extends Component {

    constructor(props) {
        super(props);
        //流式布局定义为一个组件存在
        let { widgets, pageModel } = props;
        let fluidWidget = widgets[0];
        let rows = (fluidWidget && fluidWidget.config && fluidWidget.config.rows) || [];
        //bad small
        rows.forEach(row => {
            if (row.elements && row.elements.length) {
                row.elements.forEach((element, index) => {
                    if (!element.nodeModel) {
                        let widgetModel = new WidgetModel(element);
                        widgetModel.setNodeModel(pageModel.nodeModel);
                        row.elements[index] = widgetModel;
                    }
                })
            }
        })
        this.state = {
            rows: rows
        }
    }


    render() {
        let { rows } = this.state;
        return (
            <div className="PAGE_LAYOUT_TYPE_FLUID">
                {rows.map((row, i) => {
                    let { spans = "12,12", elements = [] } = row;
                    return (
                        // 删除 row gutter={Constants.WIDGET_DEFAULT_MARGIN}
                        <Row key={i} gutter={elements.length > 1 ? Constants.WIDGET_DEFAULT_MARGIN : ''} style={{ marginBottom: Constants.WIDGET_DEFAULT_MARGIN }}>
                            {
                                spans.split(",").map((span, index) => {
                                    let widgetModel = elements[index];
                                    if (widgetModel && widgetModel.type) {
                                        return (
                                            <Col span={parseInt(span)} key={index}>
                                                <WidgetCard {...this.props}
                                                    cardHeight={row.height ? parseInt(row.height) : "auto"}
                                                    widgetModel={widgetModel}
                                                />
                                            </Col>
                                        )
                                    } else {
                                        return (
                                            <Col span={parseInt(span)} key={index}>
                                                <div className="blank_widget_placeholder">

                                                </div>
                                            </Col>
                                        )
                                    }
                                })
                            }
                        </Row>
                    )
                })}
            </div>
        );
    }
}

export default FluidContentLayout