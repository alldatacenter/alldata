/**
 *
 * Created by caoshuaibiao on 2019/1/7.
 * 运维复合组件,有1到多个OamWidget及0到多个OamActionBar 组成
 */
import React from 'react';
import { Spin, Button, Card, Modal, Tooltip, List, Row, Col } from 'antd';
import OamWidget from '../../../OamWidget';

export default class CompositionWidget extends React.Component  {

    render() {
        let {mode,nodeId,bgFill,...contentProps}=this.props;
        let children=mode.children,fill=mode.config.fill;
        return (
            <div className={(fill===true||fill==="true")?"globalBackground":""}>
                {/*<OamActionBar {...contentProps}  actions={actions} nodeId={nodeId} />*/}
                <Row gutter={8}>
                    {
                        children.map((child,index)=>{
                            if(child.components&&child.components.length===0||child.elements&&child.elements.length===0){
                                return (
                                    <Col span={child.span} key={index}>
                                    </Col>
                                )
                            }
                            return (
                                <Col span={child.span} key={index}>
                                    <List
                                        grid={{ gutter: 8, xs: 1, sm: 1, md: child.column||2, lg: child.column||2, xl: child.column||2, xxl: child.column||2 }}
                                        dataSource={child.components||child.elements}
                                        renderItem={item => {
                                            return (
                                                <List.Item style={{height:child.height||''}}>
                                                    <OamWidget {...contentProps} widget={item} nodeId={nodeId} />
                                                </List.Item>
                                            )
                                        }
                                       }
                                    />
                                </Col>
                            )
                        })
                    }
                </Row>
            </div>
        )

    }
}