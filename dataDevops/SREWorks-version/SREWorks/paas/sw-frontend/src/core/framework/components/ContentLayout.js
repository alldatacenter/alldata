/**
 * Created by caoshuaibiao on 2020/12/2.
 * 内容区布局
 */
import React from 'react';
import { Spin, Button, Card, Modal, Tooltip, List } from 'antd';
import { WidthProvider, Responsive as ResponsiveReactGridLayout } from "react-grid-layout";
import Constants from '../model/Constants';
import WidgetModel from '../model/WidgetModel';
import { SizeMe } from 'react-sizeme';
import ActionPanel from '../core/ActionPanel';
import WidgetHandleCard from './WidgetHandleCard';
const unknownPos = { x: 0, y: 0, h: 6, w: 6, i: 'unknown' };


export default class ContentLayout extends React.PureComponent {

    static defaultProps = {
        ...Constants.DESIGNER_DEFAULT_PROPS
    };

    constructor(props) {
        super(props);
        const { containerModel } = props;
        this.state = {
            widgets: containerModel.getWidgets(),
        };
        containerModel.events.on(Constants.TOOL_TYPE_ADD, this.onAddWidget);
        containerModel.events.on(Constants.CONTAINER_MODEL_TYPE_WIDGETS_CHANGED, this.onWidgetChanged);
    }

    createElement = (widget) => {
        let gridPos = widget.gridPos || unknownPos, { containerModel, isBlock } = this.props;
        //布局之间的间隙默认为10因此减去
        return (
            <div key={widget.uniqueKey} data-grid={gridPos}>
                <WidgetHandleCard {...this.props}
                    handleKey={widget.uniqueKey}
                    cardHeight={gridPos.h * (Constants.WIDGET_DEFAULT_ROW_HEIGHT + Constants.WIDGET_DEFAULT_MARGIN) - Constants.WIDGET_DEFAULT_MARGIN}
                    widgetModel={widget.type ? widget : null}
                    onWidgetCreated={(newWidget) => { newWidget.updatePos(widget.gridPos); containerModel.replaceWidget(widget, newWidget) }}
                    onDelete={() => this.onRemoveWidget(widget)}
                    openAction={this.handlePreviewOpenAction}
                    nodeModel={widget.getNodeModel()}
                />
            </div>
        );
    };

    onAddWidget = () => {
        const { containerModel } = this.props, widgets = containerModel.getWidgets();
        let existNew = false;
        widgets.forEach(w => {
            if (!w.type) {
                existNew = true;
            }
        });
        if (existNew) {
            return;
        }
        let widget = WidgetModel.CREATE_DEFAULT_INSTANCE(), wCount = widgets.length;
        widget.gridPos = {
            i: widget.uniqueKey,
            x: 0,
            y: parseInt(wCount) * Constants.WIDGET_DEFAULT_ROW_HEIGHT,
            w: Constants.WIDGET_DEFAULT_WIDTH,
            h: Constants.WIDGET_DEFAULT_HEIGHT
        };
        containerModel.addWidget(widget);
    };

    onEditWidget = (widget) => {
        console.log("编辑对象----------->", widget);
    };

    onRemoveWidget = (widget) => {
        const { containerModel } = this.props;
        containerModel.removeWidget(widget);
    };

    onWidgetChanged = () => {
        const { containerModel } = this.props;
        this.setState({
            widgets: containerModel.getWidgets(),
        });
    };

    onBreakpointChange = (breakpoint, cols) => {
        this.setState({
            breakpoint: breakpoint,
            cols: cols
        });
    };

    onLayoutChange = (layout) => {
        const { containerModel } = this.props, layoutMapping = {};
        if (layout.length) {
            layout.forEach(block => {
                layoutMapping[block.i] = block;
            });
            containerModel.getWidgets().forEach(widget => {
                let widgetBlock = layoutMapping[widget.uniqueKey];
                if (widgetBlock) {
                    widget.updatePos(widgetBlock);
                }
            })
        }
        this.props.onLayoutChange && this.props.onLayoutChange(layout);
        this.setState({ layout: layout });
    };

    handlePreviewOpenAction = (action, actionParams = {}, callBack) => {
        if (typeof action === 'string') {
            action = { name: action }
        }
        this.setState({
            currentAction: { actionParams: actionParams, ...action }
        });

    };

    handleCloseAction = () => {
        this.setState({
            currentAction: null
        });
    };



    render() {
        let { widgets, currentAction } = this.state;
        const { containerModel } = this.props;
        return (
            <SizeMe>
                {({ size }) => {
                    if (!size.width) {
                        return null;
                    }
                    return (
                        <div>
                            <ResponsiveReactGridLayout
                                onLayoutChange={this.onLayoutChange}
                                onBreakpointChange={this.onBreakpointChange}
                                {...this.props}
                                width={size.width}
                            >
                                {
                                    widgets.map(widget => {
                                        return this.createElement(widget)
                                    })
                                }
                            </ResponsiveReactGridLayout>
                            {currentAction && <ActionPanel {...this.props} currentAction={currentAction} onClose={this.handleCloseAction} />}
                        </div>
                    )
                }

                }
            </SizeMe>
        );
    }

    componentDidMount() {
        /* setTimeout(function () {
             let event = new Event('resize');
             window.dispatchEvent(event);
         },500);*/

    }

    componentWillUnmount() {
        const { containerModel } = this.props;
        containerModel.events.removeAllListeners();
    }

}