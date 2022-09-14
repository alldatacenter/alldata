/**
 * Created by caoshuaibiao on 2020/12/21.
 * 自定义快捷链接
 */
import React, { Component } from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Card, Avatar, Tooltip } from 'antd';
import _ from 'lodash';

const { Meta } = Card;
export default class QuickLink extends Component {

    render() {
        let {widgetData,widgetConfig}=this.props;
        let links = _.get(widgetConfig, 'links', widgetData||[]);
        if(!Array.isArray(links)){
            links=Array.isArray(widgetData)?widgetData:[];
        }
        let {width=140}=widgetConfig;
        return (
            <div style={{display:'flex',flexWrap:"wrap"}}>
                {
                    links.map(link=>{
                        let {icon="",title,href,description}=link;
                        if(!title||!href){
                            return null;
                        }
                        let src=null;
                        if(icon.includes("http")||icon.includes(".png")||icon.startsWith("data:")){
                            src=icon
                        }
                        return (
                            <Tooltip title={title}>
                                <Card hoverable size="small"
                                      style={{marginRight:48,width:width,marginBottom:12}}
                                >
                                    <Meta
                                        title={
                                            <div style={{display: "inline-block",height:24,fontSize:14,"line-height": 24,"vertical-align": "top"}}>
                                                <Avatar shape="square" size={24} icon={<LegacyIcon type={icon} />} src={src} >{!icon&&title[0]}</Avatar>
                                                <a style={{marginLeft:18,width:width-60}} href={href}>{title.length>5?(title.substring(0,4)+"..."):title}</a>
                                            </div>
                                        }
                                        description={description}
                                    />
                                </Card>
                            </Tooltip>
                        );
                    })
                }
            </div>
        );
    }
}