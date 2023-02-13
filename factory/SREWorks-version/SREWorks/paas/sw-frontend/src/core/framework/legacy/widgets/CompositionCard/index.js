/**
 * Created by caoshuaibiao on 2019/3/7.
 * 复合卡片组件,支持标题、链接、子区域配置
 */
import React from 'react';
import { RollbackOutlined } from '@ant-design/icons';
import { Spin, Button, Card, Modal, Tooltip, List, Row, Col, Menu, Divider, Radio } from 'antd';
import { Link } from 'dva/router';
import OamWidget from '../../../OamWidget';
import ToolBar from '../../../ToolBar';

const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;



export default class CompositionCard extends React.Component  {

    state = {
        tab: ''
    };

    onTabChange = (e) => {
        this.setState({ tab: e.target.value});
    };
    render() {
        let {mode,nodeId,...contentProps}=this.props;
        let children=mode.children,dynamicProps={size:'small',tabList:[]},{size,link,back,title, cStyle, buttonStyle,toolBar}=mode.config,tabMapping={},disElems=[];
        let defkey='__default';
        if(children.length>0||size||link){
            if(size){dynamicProps.size=size}
            if(link){
                let extra=null;
                if(link.path.startsWith("http")){
                    extra=<a onClick={()=>window.open(link.path,"_blank")}>{link.label}</a>
                }else{
                    extra=<Link to={link.path}>{link.label}</Link>
                }
                dynamicProps.extra=extra
            }
            if(title){
                dynamicProps.title=title;
                if(title.path){
                    let {label,path}=title;
                    if(path.startsWith("http")){
                        dynamicProps.title=<a onClick={()=>window.open(path,"_blank")}>{label}</a>;
                    }else{
                        dynamicProps.title=<Link to={path}>{label}</Link>;
                    }
                }
            }
            children.forEach(c=>{
                tabMapping[c.title]=c.components||c.elements;
            });
            if(children.length>1){
                dynamicProps.tabList=Object.keys(tabMapping).map(title=>{return {key:title,tab:title}});
                dynamicProps.activeTabKey=this.state.tab?this.state.tab:children[0].title;
                dynamicProps.onTabChange=this.onTabChange;
                defkey=dynamicProps.activeTabKey;
                disElems=tabMapping[defkey];
            }else{
                dynamicProps.tabList=[]
            }
            //只存在一个元素的时候配置的兼容
            if(children.length===1){
                let child=children[0];
                disElems=child.components||child.elements;
            }

        }

        let noTitleContent=!toolBar&&!title&&!dynamicProps.tabList.length,history=this.props.history;
        return (
            <Card
                style={cStyle || {}}
                className={"composition-card"} 
                size={dynamicProps.size}
                extra={dynamicProps.extra}
                title={
                    noTitleContent?null:
                    <div style={{display:'flex',justifyContent:"space-between",height:toolBar||dynamicProps.tabList.length>1?42:32}}>
                        <div style={{display:'flex'}}>
                            <div style={{ alignItems: 'center',display: "flex"}}>
                                {
                                    dynamicProps.title?
                                        <div style={{width:4,height:20,backgroundColor:'#2077FF'}} >
                                        </div>:null
                                }
                                <div>
                                    <b style={{marginLeft:12,marginRight: dynamicProps.tabList.length?12:0,fontSize:14}}>{dynamicProps.title}</b>
                                </div>
                                {
                                    back? <div><Divider type="vertical" /></div>:null
                                }
                                {
                                    back?
                                        <div style={{marginTop:8,...back.cStyle}}>
                                            <span><h5>
                                                {
                                                    back.isBackPrevious&&history.length>2?
                                                        <a onClick={()=>history.goBack()}><RollbackOutlined />{back.label}</a>
                                                        :
                                                        <Link to={back.path}><RollbackOutlined />{back.label}</Link>
                                                }
                                            </h5></span>
                                        </div>:null
                                }
                            </div>
                            <div style={{ alignItems: 'center',marginTop:12}}>
                                <RadioGroup defaultValue={dynamicProps.activeTabKey} onChange={this.onTabChange} size="small" buttonStyle={buttonStyle}>
                                    {dynamicProps.tabList.map(tab=>
                                        <RadioButton value={tab.key} key={tab.key}>{tab.tab}</RadioButton>
                                    )
                                }
                                </RadioGroup>
                            </div>
                        </div>
                        {
                            toolBar?
                                <div style={{ alignItems: 'center', float:"right"}}>
                                    <ToolBar {...contentProps} {...toolBar} nodeId={nodeId} />
                                </div>:
                                null
                        }

                    </div>
                }
            >
                <List
                    dataSource={disElems}
                    key={defkey}
                    renderItem={item => {
                        return (
                            <List.Item>
                                <OamWidget {...contentProps} widget={item} nodeId={nodeId} />
                            </List.Item>
                        )
                    }
                    }
                />
            </Card>
        );
    }
}