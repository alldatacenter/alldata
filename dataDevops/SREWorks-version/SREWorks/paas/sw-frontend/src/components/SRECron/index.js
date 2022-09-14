import React, { Component } from 'react';
import { Form, Button, Input, Row, Col, Modal } from "antd";
import localeHelper from '../../utils/localeHelper';
import httpClient from '../../utils/httpClient';
import moment from 'moment';
import Cron from './Cron';
class SRECron extends Component {
    constructor(props) {
        super(props);
        this.state = {
            cronModalVisible: false,
            cronText: '0 0 0 * * ? *',
        }
    }
    componentDidMount(){
        const { getFieldDecorator, setFieldsValue, getFieldValue } = this.props.form;
        const { model = {} } = this.props;
        let targetName = model.name;
        this.setState({
            cronText: getFieldValue(targetName)
        })
    }
    render() {
        const { getFieldDecorator, setFieldsValue, getFieldValue } = this.props.form;
        const { model = {} } = this.props;
        let targetName = model.name;
        let { cronText} = this.state;
        return (
            <div>
                <Form.Item key={model.name} style={{ display: 'inline-block' }}>
                    {<div>
                        <Input style={{ width: 170, marginRight: 2 }} initValue={model.initValue} value={cronText} placeholder={localeHelper.get('cron.expression', '定时表达式')} />
                        <Button onClick={() => this.setState({ cronModalVisible: true })}>{localeHelper.get('cron.autogen', '帮我配置')}</Button>
                        <Button onClick={() =>
                            httpClient.get(`gateway/sreworks-job/quartz/checkCron?cron=${cronText}`).then((res) => {
                                if (res === "OK") {
                                    httpClient.get(`gateway/sreworks-job/quartz/getNextTriggerTime?cron=${cronText}&size=5`).then(resSub => {
                                        Modal.success({
                                            title: '校验正确',
                                            content: (
                                                <div>
                                                    <Row style={{ marginLeft: 0 }}>
                                                        <Col span={11}>最近5次执行的时间:</Col>
                                                        <Col span={13}>
                                                            {resSub.map((lit) =>
                                                                <div style={{ marginBottom: 8 }}>{moment(lit).format('YYYY-MM-DD HH:mm:ss')}</div>
                                                            )}
                                                        </Col>
                                                    </Row>
                                                </div>
                                            )
                                        });
                                    })
                                } else {
                                    Modal.error({
                                        title: '校验失败',
                                        content: res.message,
                                    });
                                }
                            })
                        }>{localeHelper.get('cron.checkExpression', '校验表达式')}</Button>
                    </div>}
                </Form.Item>
                <Modal title={localeHelper.get('runOnTime', '定时运行')}
                    visible={this.state.cronModalVisible}
                    closable={false}
                    width={700}
                    footer={<div>
                        <span style={{ float: 'left' }}>{this.state.cronText}</span>
                        <Button onClick={() => this.setState({ cronModalVisible: false })}>
                            {localeHelper.get('common.cancel', '取消')}
                        </Button>
                        <Button onClick={() => this.setState({ cronModalVisible: false }, () => setFieldsValue({ [targetName]: this.state.cronText }))}>
                            {localeHelper.get('common.determine', '确定')}
                        </Button>
                    </div>}
                >
                    <Cron
                        onChange={cronText => this.setState({ cronText })}
                        i18n={{
                            'zh_CN': 'cn',
                            'en_US': 'en',
                            'zh_MO': 'mo',
                        }[window.APPLICATION_LANGUAGE]}
                    />
                </Modal>
            </div>
        );
    }
}

export default SRECron;