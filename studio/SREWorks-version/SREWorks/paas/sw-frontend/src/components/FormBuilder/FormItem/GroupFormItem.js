/**
 * Created by caoshuaibiao on 2019/1/22.
 * 分组表单
 */
import React, { PureComponent } from 'react';
import FormElementFactory from '../FormElementFactory'
import { Collapse, Row, Col } from 'antd';

const Panel = Collapse.Panel;

class GroupFormItem extends PureComponent {

    constructor(props) {
        super(props);
        let inline = false;
        let { model } = props;
        //存在一个inline为true就认为是一个分组一行布局
        model.groups.forEach(group => {
            group.items.map((item, index) => {
                if (item.layout && item.layout.inline) {
                    inline = true;
                }
            })
        });
        this.inline = inline;
    }

    buildingFormElements = () => {
        const formItemLayout = this.props.layout || {
            labelCol: {
                xs: { span: 24 },
                sm: { span: 6 },
            },
            wrapperCol: {
                xs: { span: 24 },
                sm: { span: 12 },
                md: { span: 10 },
            },
        };
        let formChildrens = [], inline = this.inline;
        let { model, form } = this.props;
        model.groups.forEach(group => {
            if (inline) {
                formChildrens.push(
                    <div style={{ display: 'flex', flexWrap: "wrap" }}>
                        <div style={{ margin: 8 }}>
                            <h4>{group.name}</h4>
                        </div>
                        <div style={{ marginLeft: 8 }}>
                            <Row gutter={24}>
                                {
                                    group.items.map((item, index) => {
                                        if (item.layout && item.layout.span) {
                                            return (
                                                <Col span={parseInt(item.layout.span)} key={index}>
                                                    {FormElementFactory.createFormItem(item, form, formItemLayout)}
                                                </Col>
                                            )
                                        }
                                        return FormElementFactory.createFormItem(item, form, formItemLayout);
                                    })
                                }
                            </Row>
                        </div>
                    </div>
                )
            } else {
                formChildrens.push(
                    <Panel header={group.name} key={group.name}>
                        <Row gutter={24}>
                            {
                                group.items.map((item, index) => {
                                    if (item.layout && item.layout.span) {
                                        return (
                                            <Col span={parseInt(item.layout.span)} key={index}>
                                                {FormElementFactory.createFormItem(item, form, formItemLayout)}
                                            </Col>
                                        )
                                    }
                                    return FormElementFactory.createFormItem(item, form, formItemLayout);
                                })
                            }
                        </Row>
                    </Panel>
                )
            }
        });
        return formChildrens;

    };

    render() {
        let formChildrens = this.buildingFormElements();
        let { groups=[] } = this.props.model;
        if (this.inline) {
            return <div>{formChildrens}</div>;
        }
        return (
            <Collapse defaultActiveKey={groups.map(g => g.name)} bordered={false} style={{ marginBottom: 12 }}>
                {formChildrens}
            </Collapse>
        )

    }
}

export default GroupFormItem;
