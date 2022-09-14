/**
 * Created by caoshuaibiao on 2020/2/14.
 * 复合Tabs组件,用于整块功能的分类展示,支持toobar配置
 */
import React from 'react';
import { Spin, Button, Card, Modal,Icon,Tooltip,List,Row,Col, Menu, Divider, Radio,Tabs} from 'antd';
import { Link } from 'dva/router';
import OamWidget from '../../../OamWidget';
import ToolBar from '../../../ToolBar';
import * as util from "../../../../../utils/utils";
import './index.less';

const { TabPane } = Tabs;

const tabsPathParamKey="__tabsPath";
export default class CompositionTabs extends React.Component  {

    constructor(props) {
        super(props);
        //重入的时候约定__tabsPath为tab页重入路径,嵌套tab以","分割tab层次路径
        let {mode}=this.props,defaultTab="",tabsPath=util.getUrlParams()[tabsPathParamKey];
        let {children}=mode;
        let {name,title}=children[0];
        if(!tabsPath){
            tabsPath=name||title;
        }
        let tabs=tabsPath.split(",");
        children.forEach(child=>{
            let tabKey=child.name||child.title;
            //注意路径暂时不进行层次匹配,按照扁平的结构处理,嵌套的tabs要求tab的name或者title不重复
            if(tabs.includes(tabKey)){
                defaultTab=tabKey;
            }
        });
        if(!defaultTab){
            defaultTab=name||title;
        }
        this.reentryUrl(defaultTab,defaultTab);
        this.state={
            tab:defaultTab
        };
    }

    reentryUrl=(activeKey,tab)=>{
        let {history,mode}=this.props;
        if(history){
            let urlParams=util.getUrlParams(),{children}=mode;
            let tabsPath=urlParams[tabsPathParamKey];
            if(!tabsPath){
                tabsPath=activeKey;
            }
            let tabs=tabsPath.split(",");
            //同层次的只选当前的,但仍存在多层tab嵌套不同顶级的tab中的仍然会追加上的问题,待后续完善
            children.forEach(child=>{
                let tabKey=child.name||child.title;
                //注意路径暂时不进行层次匹配,按照扁平的结构处理,嵌套的tabs要求tab的name或者title不重复
                let kIndex=tabs.indexOf(tabKey);
                if(kIndex>-1&&tabKey!==activeKey){
                    tabs.splice(kIndex,1);
                }
            });
            if(!tabs.includes(activeKey)){
                tabs.push(activeKey);
            }
            let oldTabIndex=tabs.indexOf(tab);
            if(oldTabIndex>-1){
                tabs.splice(oldTabIndex,1,activeKey);
            }
            urlParams[tabsPathParamKey]=tabs.join(",");
            history.push({
                pathname:history.location.pathname,
                search:util.objectToUrlParams(urlParams)
            });
        }
    };

    onTabChange = (activeKey) => {
        let {tab}=this.state;
        this.setState({ tab: activeKey});
        this.reentryUrl(activeKey,tab);
    };

    render() {
        let {mode,nodeId,...contentProps}=this.props,{tab}=this.state;
        let children=mode.children,{size,link,back,toolBar,type,position="top",tabStyle={}}=mode.config;
        let extra=null;
        if(children.length>0||size||link){
            if(link){
                if(link.path.startsWith("http")){
                    extra=<a onClick={()=>window.open(link.path,"_blank")}>{link.label}</a>
                }else{
                    extra=<Link to={link.path}>{link.label}</Link>
                }
            }
        }

        let noTitleContent=!toolBar;

        return (
            <div className={"globalBackground composition_tabs"} style={tabStyle}>
                <Tabs defaultActiveKey={tab}
                      size={size}
                      tabPosition={position}
                      type={type}
                      onChange={this.onTabChange}
                      tabBarExtraContent={
                          noTitleContent?null:
                              <div style={{display:'flex',justifyContent:"space-between"}}>
                                  <div style={{display:'flex'}}>
                                      <div style={{ alignItems: 'center',display: "flex"}}>
                                          {
                                              back? <div><Divider type="vertical" /></div>:null
                                          }
                                          {
                                              back?
                                                  <div style={{marginTop:8}}>
                                                      <span><h5><Link to={back.path}><Icon type="rollback" />{back.label}</Link></h5></span>
                                                  </div>:null
                                          }
                                      </div>
                                  </div>
                                  {
                                      toolBar?
                                          <div style={{ alignItems: 'center', float:"right"}}>
                                              <ToolBar {...contentProps} {...toolBar} nodeId={nodeId} />
                                          </div>:
                                          null
                                  }
                                  {
                                      extra?
                                          <div style={{ alignItems: 'center', marginLeft:12,marginRight:12}}>{extra}</div>:
                                          null
                                  }
                              </div>
                      }
                >
                    {
                        children.map(child=>{
                            return (
                                <TabPane tab={<span>{child.icon?<Icon type={child.icon} />:null}{child.title}</span>} key={child.name||child.title}>
                                    <div style={{ marginLeft:8,marginRight:8}}>
                                        <List
                                            dataSource={child.components||child.elements}
                                            key={child.title}
                                            renderItem={item => {
                                                return (
                                                    <List.Item>
                                                        <OamWidget {...contentProps} widget={item} nodeId={nodeId} />
                                                    </List.Item>
                                                )
                                            }
                                            }
                                        />
                                    </div>
                                </TabPane>
                            )
                        })
                    }
                </Tabs>
            </div>
        )
    }
}