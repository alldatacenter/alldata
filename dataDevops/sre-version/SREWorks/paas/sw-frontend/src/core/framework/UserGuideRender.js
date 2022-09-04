/**
 * Created by caoshuaibiao on 2020/4/3.
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Dropdown, Menu, Button, Popover, Tooltip, List } from 'antd';
import { Link } from 'dva/router';
import * as util from "../../utils/utils";

export default class UserGuideRender extends React.Component  {
    //处理配置的path本来就带有一部分的固定参数等情况
    handleUrl=(url,rowData)=>{
        if(!url) {
            return 
        }
        let rowParams={};
        if(rowData){
            Object.keys(rowData).forEach(rk=>{
                rowParams[rk]=(rowData[rk].value?rowData[rk].value:rowData[rk])
            });
        }
        let path=url.startsWith("/")?url.substring(1):url,start=url.indexOf("?"),initParam="";
        if(start>0){
            path=path.substring(0,start-1);
            initParam="&"+url.substring(start+1);
        }
        return {
            href:path+"?"+util.objectToUrlParams(rowParams)+initParam,
            search:util.objectToUrlParams(rowParams)+initParam
        }
    };



    render() {
        let {docs,label,icon="question-circle",title}=this.props,menuItems=[];
        docs.forEach((doc,index)=>{
            let {icon,path='',label,params={}}=doc;
            let genlink=this.handleUrl(path,params);
            menuItems.push(
                <Menu.Item key={index}>
                    <div>
                        {
                            path.startsWith("http")?
                                <a href={(genlink && genlink.href) || ''} target="_blank">{icon?<LegacyIcon type={icon} />:null}{label}</a>:
                                <Link to={{
                                    pathname:path,
                                    search:(genlink && genlink.search) || ''
                                }}>{icon?<LegacyIcon type={icon} />:null}{label}</Link>
                        }
                    </div>
                </Menu.Item>
            );
        });
        let content=(
            <List
                size="small"
                dataSource={docs}
                renderItem={doc =>{
                    let {icon,path,label,params={}}=doc;
                    let genlink=this.handleUrl(path,params);
                    if(!genlink) {
                        return null
                    }
                    return (
                        <List.Item>
                            <div style={{fontSize: 12}}>
                            {
                                path.startsWith("http")?
                                    <a href={genlink.href} target="_blank">{icon?<LegacyIcon type={icon} />:null}{label}</a>:
                                    <Link to={{
                                        pathname:path,
                                        search:genlink.search
                                    }}>{icon?<LegacyIcon type={icon} />:null}{label}</Link>
                            }
                            </div>
                        </List.Item>
                    );
                } }
            />
        );
        return (
            <div className="oam-action-bar-item" style={{display: 'flex', alignItems: 'center'}}>
                <Popover content={content} title={title} placement="bottom">
                    <a><LegacyIcon type={icon} />{label}</a>
                </Popover>
            </div>
        );
    }
}