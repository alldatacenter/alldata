import React from 'react';
import { Card, Descriptions , Spin , Table} from 'antd';
import PropTypes from 'prop-types';
import EllipsisTip from "../../../../../components/EllipsisTip";
import _ from 'lodash';
import ActionsRender from '../../../ActionsRender';
import  httpClient from "../../../../../utils/httpClient";
import * as utils from "../../../../../utils/utils";
import JSXRender from "../../../../../components/JSXRender";

import './index.scss'

class TCommonKv extends React.Component {
    constructor(props) {
        super(props);
    }

    renderCustomValue(value){
        switch(value.type){
            case 'link': return <a href={value.href} target="_black"><EllipsisTip>{value.value}</EllipsisTip></a>
            default: return <span><EllipsisTip>{value.value}</EllipsisTip></span>
        }
    }


    render() {
        return (
            <div style={this.props.style} >
                <Descriptions size="large" col={this.props.colNum} layout={this.props.layout}>
                    {
                        Object.keys(this.props.kvData).map(item=> {
                            //值为空字符时Ellipsis会出现布局混乱,因此判断为空的时候用空白字符串代替
                            let displayValue = this.props.kvData[item];
                            return (
                                <Descriptions.Item label={item} key={item}>
                                <pre style={{marginBottom: '0px', whiteSpace: 'pre-wrap', wordBreak: 'break-word'}}>{
                                    displayValue !== null && typeof displayValue === "object" ?
                                        this.renderCustomValue(displayValue)
                                        :
                                        <EllipsisTip>{displayValue === ''||displayValue ===null ? ' ' : displayValue}</EllipsisTip>
                                }</pre>
                                </Descriptions.Item>)
                        })
                    }
                </Descriptions>
            </div>
        );
    }
}

TCommonKv.propTypes = {
    // 通用可传递的 className 与 style 属性
    className: PropTypes.any,
    style: PropTypes.object,

    // 自定义所需要的属性
    colNum: PropTypes.number,
    layout: PropTypes.string,
    kvData: PropTypes.object.isRequired
};

TCommonKv.defaultProps = {
    colNum: 3,
    layout: "horizontal"
};

class CommonKvList extends React.Component  {

    constructor(props) {
        super(props);
        this.state = {
            loading: true,
            kvData : {}
        };
    }

    componentDidMount() {
        this.reloadKv(Object.assign({} , this.props.nodeParams , utils.getUrlParams()));
    }

    componentDidUpdate(preProps){
        if(!_.isEqual(preProps.nodeParams , this.props.nodeParams)){//前端api方式配置
            this.reloadKv(Object.assign({} , this.props.nodeParams , utils.getUrlParams()));
        }
    }

    reloadKv=(params)=>{
        const { mode, nodeId,openAction,renders } = this.props;
        //let params = Object.assign({} , nextProps.nodeParams , utils.getUrlParams());
        let api=utils.renderTemplateJsonObject(mode.config.api,params);
        this.setState({loading: true});
        return httpClient.get(api.url, {params: api.params||{}}).then(data => {
            let {dataIndex}=mode.config;
            if(dataIndex) data=data[dataIndex];
            let kvData = {},kvArray=mode.config.kvArray.filter((item=>{
                let {display}=item;
                return !(display&&(data[display]===false||data[display]==="false"));
            }));
            kvArray.map((item)=>{
                let {dict={}}=item;
                let displayValue=dict[data[item.key]+""]||data[item.key];
                if(item.type == 'link'){
                    kvData[item.name] = {
                        value: item.key.indexOf('$(') !== -1 ? utils.renderTemplateString(item.key , data) :  displayValue,
                        type:"link",
                        href:item.href
                    }
                }else if(item.type == 'render'){
                    kvData[item.name] = {
                        value: <JSXRender jsx={utils.renderTemplateString(item.render ,  data)}/>,
                        type:"render",
                    }
                }else{
                    kvData[item.name] = item.key.indexOf('$(') !== -1 ? utils.renderTemplateString(item.key , data) :  displayValue
                }
                let {actionBar}=item;
                if(actionBar){
                    let kvItem= kvData[item.name];
                    if(!kvItem.value){
                        kvData[item.name]={
                            value:kvItem
                        }
                    }

                    kvData[item.name].value=(
                        <div style={{display:'flex'}}>
                            <span>{kvData[item.name].value}</span>
                            <span style={{marginLeft:16}}><ActionsRender {...this.props} {...actionBar} nodeId={nodeId} record={data} /></span>
                        </div>
                    )
                }
            });
            if(mode.config.type == 'table_list'){
                let newKvData = []
                Object.keys(kvData).map((item)=>{
                    newKvData.push({
                        key : item,
                        value: kvData[item]
                    })
                })
                kvData = newKvData
            }
            this.setState({kvData,loading: false});
        })


    };

    render() {
        const { mode, nodeId } = this.props;
        let {kvArray , colNum , title , type , border} = mode.config;
        return (
            <Spin spinning={this.state.loading}>
                <Card title={title ? title : ''} className="common-kv-list" bordered={border} size="small">
                    {type == 'table_list' ? <Table columns={this.column} showHeader={false} dataSource={Array.isArray(this.state.kvData) ? this.state.kvData : []}/> : <TCommonKv 
                        colNum={colNum}
                        kvData={this.state.kvData}
                    />}
                </Card>
            </Spin>
            
        )
    }

    column = [
        {
            title:'key',
            dataIndex:'key'
        },
        {
            title:'value',
            dataIndex:'value'
        }
    ]
}

export default CommonKvList;