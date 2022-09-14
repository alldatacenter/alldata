/**
 * Created by caoshuaibiao on 2020/12/24.
 * 节点主页面
 */
import React from 'react';
import { Spin, Button, Card, Modal, Tooltip, List } from 'antd';
import { WidthProvider, Responsive as ResponsiveReactGridLayout } from "react-grid-layout";
import WidgetCard from './WidgetCard';
import { Route, Redirect, Link, Switch } from 'dva/router';
import Constants from '../model/Constants';
import { connect } from 'dva';
import ActionPanel from './ActionPanel';
import { SizeMe } from 'react-sizeme';
import FluidContentLayout from '../components/FluidContentLayout';

@connect(({ global, node }) => ({
    nodeParams: node.nodeParams,
    theme: global.theme,
    currentUser: global.currentUser,
    language: global.language
}))
export default class PageContent extends React.PureComponent {

    static defaultProps = {
        ...Constants.DESIGNER_DEFAULT_PROPS
    };

    constructor(props) {
        super(props);
        let { pageModel } = props;
        this.dataSource = pageModel.getDataSource();
        this.state = {
            pageParamsLoaded: this.dataSource,
            currentAction: null,
        };
    }

    componentWillMount() {
        let { pageModel, dispatch, nodeParams, actionParams } = this.props;
        if (this.dataSource) {
            this.dataSource.query(Object.assign({}, nodeParams, actionParams)).then(pageParams => {
                dispatch({ type: 'node/updateParams', paramData: pageParams });
                this.setState({
                    pageParamsLoaded: false
                });
            });
        }
    }


    handleOpenAction = (action, actionParams = {}, callBack) => {
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
        let { pageModel, pageLayoutType = Constants.PAGE_LAYOUT_TYPE_CUSTOM } = this.props, { pageParamsLoaded, currentAction } = this.state;
        if (pageParamsLoaded) {
            return <Spin className="globalSpin" />
        }
        let widgets = pageModel.getWidgets(), props = this.props;
        if (currentAction && currentAction.displayType === 'drawer') {
            currentAction = Object.assign({ isDrawer: true }, currentAction);
        }
        return (
            <div>
                {
                    pageLayoutType === Constants.PAGE_LAYOUT_TYPE_CUSTOM &&
                    <SizeMe>
                        {({ size }) => {
                            if (!size.width) {
                                return null;
                            }
                            return (
                                <div className="PAGE_LAYOUT_TYPE_CUSTOM">
                                    {
                                        <ResponsiveReactGridLayout
                                            {...props}
                                            onLayoutChange={this.onLayoutChange}
                                            onBreakpointChange={this.onBreakpointChange}
                                            width={size.width || 880}
                                            height={size.height || 680}
                                            isDraggable={false}
                                            isResizable={false}
                                            resizeHandle={() => <div />}
                                        >
                                            {
                                                widgets.map(widget => {
                                                    return (
                                                        <div key={widget.uniqueKey || widget.config.uniqueKey || 'unknown'} data-grid={widget.gridPos || widget.config.gridPos || Constants.UNKNOWN_GRID_POS} >
                                                            <WidgetCard {...props}
                                                                widgetModel={widget}
                                                                openAction={this.handleOpenAction}
                                                            />
                                                        </div>
                                                    )
                                                })
                                            }
                                        </ResponsiveReactGridLayout>
                                    }

                                </div>
                            )
                        }

                        }
                    </SizeMe>
                }
                {
                    pageLayoutType === Constants.PAGE_LAYOUT_TYPE_FLUID &&
                    <FluidContentLayout {...props} pageLayoutType={Constants.PAGE_LAYOUT_TYPE_FLUID} widgets={widgets} openAction={this.handleOpenAction} />
                }
                {currentAction && <ActionPanel {...this.props} currentAction={currentAction} nodeModel={pageModel.getNodeModel()} onClose={this.handleCloseAction} />}
            </div>
        );
    }

}