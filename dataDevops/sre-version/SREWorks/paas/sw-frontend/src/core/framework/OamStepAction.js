/**
 * Created by caoshuaibiao on 2020/2/10.
 * 分步Action
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Steps, Button, message, Drawer, Modal, Row, Col, Card } from 'antd';
import OamAction from './OamAction';
import Bus from '../../utils/eventBus'
const { Step } = Steps;

const sizeMapping = {
    "small": {
        percent: 0.4,
        min: 360,
        labelSpan: 8
    },
    "default": {
        percent: 0.6,
        min: 640,
        labelSpan: 6
    },
    "large": {
        percent: 0.8,
        min: 900,
        labelSpan: 4
    },
};


export default class OamStepAction extends React.Component {
    constructor(props) {
        super(props);
        let { actionData } = props;
        let stepActions = actionData.config.stepActions;
        this.state = {
            current: 0,
            stepsFormValues: {},
            steps: stepActions && stepActions.map(actionData => {
                return {
                    title: actionData.config.label,
                    content: '',
                }
            })
        };
    }

    componentDidMount() {
        let { mode, actionData } = this.props;
        if ((mode === 'custom' || mode === 'step') && actionData) {
            this.openAction();
        }
    }

    next = (params) => {
        const current = this.state.current + 1;
        this.setState({ current, stepsFormValues: Object.assign({}, this.state.stepsFormValues, params) });
    };

    prev = () => {
        const current = this.state.current - 1;
        this.setState({ current });
    };

    onClose = () => {
        Bus.emit('stepFormClose', false);
        this.setState({
            dockVisible: false,
        });
    };

    openAction = () => {
        this.setState({
            dockVisible: true,
        });
    };

    render() {
        const { current, steps, stepsFormValues, dockVisible } = this.state, { actionData, mode, displayType, nodeParams, actionParams, userParams, nodeId, ...contentProps } = this.props;
        let action = actionData.config;
        let sizeDef = sizeMapping[action.size];
        if (!sizeDef) {
            sizeDef = sizeMapping.default;
        }
        let defaultSize = document.body.clientWidth * sizeDef.percent;
        if (sizeDef.min > defaultSize) {
            defaultSize = sizeDef;
        }
        if (sizeDef.min > document.body.clientWidth) {
            defaultSize = document.body.clientWidth;
        }
        let currentAction = action.stepActions[current];
        let stepActionContent = (
            <div style={{ margin: 24 }}>
                <div style={{ marginBottom: 24 }}>
                    <Steps current={current} type="navigation" size="small">
                        {steps.map(item => (
                            <Step key={item.title} title={item.title} />
                        ))}
                    </Steps>
                </div>
                <Card>
                    <Row>
                        <Col span={2} />
                        <Col span={20} pull={2}>
                            <OamAction {...contentProps} key={currentAction.elementId || currentAction.id} actionId={currentAction.id || currentAction.elementId} actionData={currentAction}
                                nodeParams={nodeParams} actionParams={Object.assign({}, userParams, actionParams, stepsFormValues)}
                                nodeId={nodeId}
                                isFirst={current === 0}
                                isLast={current === action.stepActions.length - 1}
                                onNext={this.next}
                                onPrev={this.prev}
                                onClose={this.onClose}
                                stepsFormValues={stepsFormValues}
                                mode="step" />
                        </Col>
                        <Col span={2} />
                    </Row>
                </Card>
            </div>
        );

        //增加直接在页面显示分步表单的功能
        let currentStep = () => {
            if (this.props.displayType === 'form' || this.props.displayType === 'panel') {
                return <Card size="small" bordered={false}>{stepActionContent}</Card>
                // return <div>null</div>
            } else {
                if (action.displayType === 'modal') {
                    return <Modal
                        title={action.label}
                        visible={dockVisible}
                        footer={null}
                        width={defaultSize}
                        maskClosable={true}
                        destroyOnClose={true}
                        onCancel={this.onClose}
                    >
                        {stepActionContent}
                    </Modal>
                } else {
                    return <Drawer
                        placement="right"
                        title={action.label}
                        width={defaultSize}
                        destroyOnClose={true}
                        onClose={this.onClose}
                        visible={dockVisible}
                    >
                        {stepActionContent}
                    </Drawer>
                }
            }
        }

        return (
            <div>
                {mode !== 'custom' ? <a onClick={this.openAction}><LegacyIcon type={actionData.config.icon || 'tool'} style={{ marginRight: 8 }} />{actionData.config.label}</a> : null}
                {currentStep()}
            </div>
        );
    }
}