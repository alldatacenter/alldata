/**
 * Created by caoshuaibiao on 2019/1/22.
 * 参数映射器
 */
import React, { PureComponent, Component } from 'react';
import { PlusOutlined } from '@ant-design/icons';
import { Tabs, Card, Row, Col, Button, Tooltip } from 'antd';
import ParameterMappingForm from './ParameterMappingForm';
import ParameterMappingTree from './ParameterMappingTree';
import localeHelper from '../../utils/localeHelper';
const TabPane = Tabs.TabPane;
let addSeq = 0;

class ParameterMappingBuilder extends PureComponent {

    constructor(props) {
        super(props);
        addSeq = 0;
        let parameterDefiner = this.props.parameterDefiner, parameters = this.props.parameterDefiner.getParameters();
        parameterDefiner.validate = this.validate;
        //console.log("parameterDefiner------>",parameterDefiner.noBinding);
        parameterDefiner.noBinding = this.props.mode === 'noBinding';
        this.state = {
            parameterDefiner: parameterDefiner,
            parameters: parameters,
            activeKey: parameters[0] ? parameters[0].name : '',
        };
    }

    componentDidMount() {
        if (this.props.onChange) {
            this.props.onChange(this.props.parameterDefiner)
        }
    }

    validate = () => {
        //校验当前编辑的参数tab是否合法
        return this.onTabChange(this.state.activeKey);
    };

    onTabChange = (activeKey) => {
        let nowKey = this.state.activeKey, parameterDefiner = this.state.parameterDefiner;
        if (!nowKey) return true;
        let nowParamMapping = parameterDefiner.getParamMapping(nowKey);
        //当修改了参数定义的name时,nowKey可能不存在于所有的的参数中,因此直接设置activeKey
        if (!nowParamMapping) {
            let parameters = parameterDefiner.getParameters();
            let defaultActive = parameters[parameters.length - 1];
            this.setState({ activeKey: (defaultActive && defaultActive.name) || activeKey });
        }
        //通过校验才能切换至下一个tab页
        if (nowParamMapping && nowParamMapping.parameter.validate()) {
            this.setState({ activeKey: activeKey });
            return true;
        }
        return false;
    };

    onEdit = (targetKey, action) => {
        this[action](targetKey);
    };

    add = () => {
        addSeq++;
        let parameterDefiner = this.state.parameterDefiner, defaultName = 'arg-' + addSeq;
        parameterDefiner.addDefaultParam(defaultName);
        this.setState({ parameters: parameterDefiner.getParameters(), activeKey: defaultName });
        this.onTabChange(defaultName);
    };

    remove = (targetKey) => {
        let parameterDefiner = this.state.parameterDefiner;
        parameterDefiner.removeParam(targetKey);
        this.setState({ parameters: parameterDefiner.getParameters(), activeKey: parameterDefiner.getParameters()[0] ? parameterDefiner.getParameters()[0].name : '' });
    };

    /**
     * 关闭参数绑定树中存在默认值的tab页
     */
    closeBindingTreeDefaultValueTabs = () => {
        let parameterDefiner = this.state.parameterDefiner;
        parameterDefiner.removeBindingHasDefaultValueParam();
        this.setState({ parameters: parameterDefiner.getParameters(), activeKey: parameterDefiner.getParameters()[0] ? parameterDefiner.getParameters()[0].name : '' });
    };

    render() {
        let { parameters, parameterDefiner } = this.state, { mode } = this.props;
        let panes = [], cardkey = "_param_binding_key", paramTree = parameterDefiner.bindingParamTree, formSpan = 12, extConfig = parameterDefiner.extConfig || {};
        paramTree.forEach(pt => {
            cardkey = cardkey + "_" + pt.id;
        });
        let hasBinding = (mode !== 'noMapping' && mode !== 'noBinding');
        if (mode === 'instance') {
            formSpan = 14;
        }
        let { showCard, tabTitle, renderTabExt, colCount, layout } = extConfig;
        parameters.forEach(parameter => {
            let useLayout = layout;
            if (!useLayout) {
                if (hasBinding) {
                    useLayout = false
                } else {
                    useLayout = {
                        labelCol: {
                            xs: { span: 24 },
                            sm: { span: 24 },
                            md: { span: 6 },
                        },
                        wrapperCol: {
                            xs: { span: 24 },
                            sm: { span: 24 },
                            md: { span: 12 },
                        },
                    }
                }
            }

            panes.push(
                <TabPane tab={parameter.label} key={parameter.name} closable={true}>
                    <Row gutter={16}>
                        <Col span={hasBinding ? formSpan : 24} key="__params_def_key">
                            <Card bodyStyle={{ padding: 1 }} title={extConfig.showCard !== false && <div style={{ fontSize: '14px', fontWeight: 'normal' }}>{localeHelper.get('common.paramDefined', '参数定义')}</div>} bordered={false}>
                                <ParameterMappingForm key={parameter.name} parameter={parameter} mode={this.props.mode}
                                    colCount={colCount}
                                    formItemLayout={useLayout}
                                />
                            </Card>
                        </Col>
                        {hasBinding ?
                            <Col span={24 - formSpan} key="__bing_tree_key">
                                <Card bodyStyle={{ padding: 1 }} title={
                                    <div style={{ fontSize: '14px', fontWeight: 'normal' }}>
                                        {localeHelper.get('common.bindSet', '绑定设置')}
                                        <Tooltip title={localeHelper.get('common.bindSetTitle', '将关闭作业参数中存在默认值的参数绑定tab页')}>
                                            <Button type="dashed" size={"small"} style={{ marginLeft: 12 }} onClick={this.closeBindingTreeDefaultValueTabs}>{localeHelper.get('common.closeDefaultBind', '关闭默认值绑定')}</Button>
                                        </Tooltip>
                                    </div>
                                }
                                    bordered={false}>
                                    <ParameterMappingTree key={parameter.name} parameter={parameter} parameterDefiner={parameterDefiner} />
                                </Card>
                            </Col> : null
                        }
                    </Row>
                </TabPane>
            );
        });
        if (showCard === false) {
            return (
                <Tabs activeKey={this.state.activeKey}
                    hideAdd
                    animated
                    renderTabBar={(props, DefaultTabBar) => {
                        return (
                            <div>
                                <h4 style={{ float: "left", marginRight: 16, marginTop: 8 }}>{tabTitle}</h4>
                                <DefaultTabBar {...props} />
                            </div>
                        )
                    }
                    }
                    tabBarExtraContent={<div style={{ display: "flex" }}><a onClick={() => this.onEdit(null, 'add')} style={{ marginRight: 12, fontSize: 16 }}><PlusOutlined /><span style={{ fontSize: 14 }}>添加表单项</span></a>{renderTabExt}</div>}
                    onChange={this.onTabChange}
                    type="editable-card"
                    onEdit={this.onEdit}
                >
                    {panes}
                </Tabs>
            );
        }
        return (
            <Card size="small" title={<div style={{ fontSize: '14px' }}>{extConfig.cardTitle || localeHelper.get('common.paramSet', '参数设置')}{hasBinding && <em className="optional">{localeHelper.get('common.paramSetMsg', '(左侧定义提单界面录入参数,右侧勾选代表作业参数与此参数绑定)')}</em>}</div>} key={cardkey}>
                <Tabs activeKey={this.state.activeKey} onChange={this.onTabChange} type="editable-card" onEdit={this.onEdit} animated>
                    {panes}
                </Tabs>
            </Card>
        );
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.parameterDefiner) {
            let parameterDefiner = nextProps.parameterDefiner;
            let { parameters } = this.state;
            if (parameterDefiner.getParameters().length !== parameters.length) {
                this.state = {
                    parameterDefiner: parameterDefiner,
                    parameters: parameterDefiner.getParameters(),
                    activeKey: parameterDefiner.getParameters()[0] ? parameterDefiner.getParameters()[0].name : ''
                };
                if (!parameterDefiner.validate) parameterDefiner.validate = this.validate;
            }
        }
    }
}

export default ParameterMappingBuilder;
