/**
 * Created by caoshuaibiao on 2019/9/6.
 * 挂件工具栏
 */
import React from 'react';
import { Dropdown, Menu, Card, Modal, Tooltip, List, Divider, Drawer, Button } from 'antd';
import { connect } from 'dva';
import OamWidget from './OamWidget';
import ActionsRender from './ActionsRender';
import ActionForm from './legacy/widgets/ActionForm';
import moment from 'moment';
import JSXRender from "../../components/JSXRender";
import * as util from "../../utils/utils";

@connect(({ node }) => ({
    nodeParams: node.nodeParams
}))
export default class ToolBar extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            initTime: false
        };
    }


    getNodeParams = () => {
        let { parameters, nodeParams, rowData = {}, widgetDefaultParams = {}, formInitParams = {}, widgetParams = {} } = this.props;
        let params = Object.assign({}, formInitParams, widgetDefaultParams, parameters, nodeParams, rowData);
        if (this.props.widgetParams) {
            params = { ...params, ...widgetParams };
        }
        return params;
    };

    componentWillMount() {
        let { timeSelector = false, widgetParams } = this.props;
        if (timeSelector) {
            let { type, defaultTime = {}, output } = timeSelector, initTime, urlParams = util.getUrlParams();
            let { stimeOffsetFromNow, etimeOffsetFromNow, stimeNow, etimeNow } = defaultTime,
                stime = moment().subtract(1, 'hours'), etime = moment();
            //url中的时间优先级高于配置
            let urlStime = urlParams.stime, urlEtime = urlParams.etime;
            //存在输出定义取输出定义的值
            if (output) {
                if (Array.isArray(output)) {
                    urlStime = urlParams[output[0]];
                    urlEtime = urlParams[output[1]];
                } else {
                    urlStime = urlParams[output];
                }
            }
            if (urlStime && isFinite(urlStime)) {
                let intStime = parseInt(urlStime);
                //兼容毫秒和秒格式
                if (urlStime.length < 12) {
                    intStime = intStime * 1000
                }
                stime = moment(intStime);
            } else {
                if (stimeOffsetFromNow) {
                    stime = moment().subtract(stimeOffsetFromNow[0], stimeOffsetFromNow[1]);
                }
            }
            if (urlEtime && isFinite(urlEtime)) {
                let intEtime = parseInt(urlEtime);
                //兼容毫秒和秒格式
                if (urlEtime.length < 12) {
                    intEtime = intEtime * 1000
                }
                etime = moment(intEtime);
            } else {
                if (etimeOffsetFromNow) {
                    etime = moment().subtract(etimeOffsetFromNow[0], etimeOffsetFromNow[1]);
                }
            }
            //如果是actionTab模式 且配置了stimeNow或etimeNow  defaultTime中的优先级最高
            if (widgetParams) {
                if (stimeNow) {
                    stimeNow = this.getNodeParams()[stimeNow];
                    stimeNow = parseInt(stimeNow);
                    if (String(stimeNow).length < 12) {
                        stimeNow = stimeNow * 1000;
                    }
                    stime = moment(stimeNow);
                    if (stimeOffsetFromNow) {
                        stime = moment(stimeNow).subtract(stimeOffsetFromNow[0], stimeOffsetFromNow[1]);
                    }
                }

                if (etimeNow) {
                    etimeNow = this.getNodeParams()[etimeNow];
                    etimeNow = parseInt(etimeNow);
                    if (String(etimeNow).length < 12) {
                        etimeNow = etimeNow * 1000;
                    }
                    etime = moment(etimeNow);
                    if (etimeOffsetFromNow) {
                        etime = moment(etimeNow).subtract(etimeOffsetFromNow[0], etimeOffsetFromNow[1]);
                    }
                }
            }

            if (type === 'dataTime') {
                //this.handTimeChange([etime.valueOf(),etime.valueOf()]);
                if (urlStime) {
                    initTime = stime;
                } else {
                    initTime = stimeOffsetFromNow ? stime : etime;
                }
            } else {
                initTime = [stime, etime];
                //this.handTimeChange([stime.valueOf(),etime.valueOf()]);

            }
            this.setState({ initTime: initTime });
        }
    }

    handTimeChange = (timeRange) => {
        let { handleParamsChanged, timeSelector = {}, history } = this.props, outputParams = { ___refresh_timestamp: (new Date()).getTime() }, output = timeSelector.output;
        let { type } = timeSelector;
        //允许用户自定义输出的参数名,为数组时默认第一个是开始时间第二个是结束时间
        if (output) {
            if (Array.isArray(output)) {
                Object.assign(outputParams, { [output[0]]: timeRange[0], [output[1]]: timeRange[1] });
            } else {
                Object.assign(outputParams, { [output]: timeRange[0] });
            }
        } else {
            outputParams.startTime = timeRange[0]; outputParams.endTime = timeRange[1];
            Object.assign(outputParams, {
                startTime: timeRange[0], endTime: timeRange[1],
                stime_ms: timeRange[0], etime_ms: timeRange[1],
                stime: parseInt(timeRange[0] * 0.001), etime: parseInt(timeRange[1] * 0.001)
            });
        }
        //增加url重入支持
        if (history) {
            let urlParams = util.getUrlParams();
            if (output) {
                if (Array.isArray(output)) {
                    urlParams[output[0]] = parseInt(timeRange[0] * 0.001);
                    urlParams[output[1]] = parseInt(timeRange[1] * 0.001);
                } else {
                    urlParams[output] = parseInt(timeRange[0] * 0.001);
                }
            } else {
                urlParams.stime = parseInt(timeRange[0] * 0.001);
                if (type !== 'dataTime') {
                    urlParams.etime = parseInt(timeRange[1] * 0.001);
                }
            }
            history.push({
                pathname: history.location.pathname,
                search: util.objectToUrlParams(urlParams)
            });
        }
        handleParamsChanged && handleParamsChanged(outputParams);
    };

    handleRefresh = (type) => {
        let { handleParamsChanged } = this.props;
        handleParamsChanged && handleParamsChanged({ ___refresh_timestamp: (new Date()).getTime() });
    };


    render() {
        let { actionBar, nodeId, widgets = [], timeSelector = false, filters = [], nodeParams, userParams, openAction, handleParamsChanged, filterWidget, customRender, ...contentProps } = this.props, { initTime } = this.state,
            barItems = [];
        //添加自定义Render
        if (customRender) {
            barItems.push(
                <div style={{ display: 'flex', alignItems: 'center', marginLeft: 12 }} key="__widget_filters">
                    <JSXRender jsx={customRender} />
                </div>
            )
        }
        if (filterWidget) {
            barItems.push(
                <div style={{ display: 'flex', alignItems: 'center', marginLeft: 3 }} key="__widget_filters">
                    {filterWidget}
                </div>
            )
        }
        if (filters.length > 0) {
            barItems.push(
                <div style={{ height: 48, display: 'flex', alignItems: 'center', marginLeft: 12 }} key="__widget_filters">
                    {
                        filters.map((filter, index) => {
                            return <ActionForm key={index} {...contentProps} nodeParams={Object.assign({}, nodeParams, userParams)}
                                mode={filter} nodeId={nodeId}
                                handleParamsChanged={handleParamsChanged}
                                displayType="filter"
                            />
                        })
                    }
                </div>
            )
        }
        // 暂时删除工具栏添加widget区块的能力
        // if (widgets.length>0) {
        //     barItems.push(
        //         <div style={{height: 48, display: 'flex',alignItems: 'center',marginLeft:12,fontSize:12,fontWeight: 'normal'}} key="__widget_bar">
        //             {
        //                 widgets.map((widget,index) => {
        //                     return <OamWidget key={index} {...contentProps} nodeParams={Object.assign({}, nodeParams, userParams)}
        //                                       widget={widget} nodeId={nodeId}
        //                                       handleParamsChanged={handleParamsChanged}/>
        //                 })
        //             }
        //         </div>
        //     )
        // }
        if (timeSelector) {
            let { limitRang, pickConfig, type, showAutoRefresh, showRefresh, defaultTime, ...otherProps } = timeSelector;
            barItems.push(
                <div key="__time_bar" style={{ marginLeft: 12 }}>
                    {/*<TimeSelectBar limitRang={limitRang} initTime={initTime} pickConfig={pickConfig} pickType={type} autoRefresh={false}
                                   showAutoRefresh={showAutoRefresh!==false} showRefresh={showRefresh!==false} onChange={this.handTimeChange}
                                   {...otherProps}
                    />*/}
                </div>
            )
        }
        if (actionBar) {
            barItems.push(
                <div key="__actions_" style={{ marginLeft: 12 }}>
                    <ActionsRender  {...actionBar} nodeParams={nodeParams} openAction={openAction} handleRefresh={this.handleRefresh} />
                </div>
            );
        }
        return (
            <div style={{ display: 'flex', alignItems: 'center', height: "100%" }}>
                {barItems}
            </div>
        )
    }

}
