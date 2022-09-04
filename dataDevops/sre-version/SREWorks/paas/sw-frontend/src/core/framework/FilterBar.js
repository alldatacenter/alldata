/**
 * Created by caoshuaibiao on 2019/11/24.
 * 过滤器
 */
import React, { PureComponent } from 'react';
import { Form, Icon as LegacyIcon } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { DownOutlined } from '@ant-design/icons';
import {
    Card,
    Row,
    Col,
    Alert,
    Divider,
    Dropdown,
    Button,
    Menu,
    Select,
    Input,
    Popover,
} from 'antd';
import FormElementFactory from '../../components/FormBuilder/FormElementFactory';
import safeEval from '../../utils/SafeEval';
import OamWidget from './OamWidget';
import debounce from 'lodash.debounce';
import localeHelper from '../../utils/localeHelper';

const { Option } = Select;

class FilterBar extends PureComponent {

    constructor(props) {
        super(props);
        let { onSubmit, items } = props, defaultFilter = false;

        if (onSubmit) {
            onSubmit.validate = this.validate;
        }
        items.forEach(ni => {
            let { defModel = {} } = ni;
            if (defModel.defaultFilter) {
                defaultFilter = ni;
            }
        });
        if (!defaultFilter) defaultFilter = items[0];
        this.state = {
            displayMode: 'base',
            defaultFilter: defaultFilter
        }
        this.hasType = false;
        this.autoSubmit = debounce(this.autoSubmit, 500);
        this.prevSubmitValues = {};
    }


    validate = () => {
        let pass = false, { onSubmit, items } = this.props;
        this.props.form.validateFieldsAndScroll((err, values) => {
            if (!err) {
                let allValues = {};
                /*items.forEach((item,index)=> {
                    let {defModel={}}=item;
                    let {displayType}=defModel;
                    if(displayType==='advanced'){
                        allValues[item.name]=null;
                    }
                });*/
                Object.assign(allValues, values);
                this.prevSubmitValues = Object.assign({}, allValues);
                if (onSubmit) onSubmit(allValues);
                pass = allValues;
            }
        });
        return pass
    };

    handleKeyUp = (event) => {
        event.preventDefault();
        if (event.keyCode === 13) {
            this.validate();
        }
    };

    componentDidMount() {
        let { enterSubmit } = this.props;
        if (enterSubmit) {
            this.formContainer.addEventListener("keyup", this.handleKeyUp);
        }
    }

    componentWillUnmount() {
        let { enterSubmit } = this.props;
        if (enterSubmit) {
            this.formContainer.removeEventListener("keyup", this.handleKeyUp);
        }
    }

    handleDisplayChanged = () => {
        this.setState({
            displayMode: this.state.displayMode === 'base' ? 'advanced' : 'base'
        });
    };

    handleFilterChanged = (value, options) => {
        this.setState({
            defaultFilter: this.props.items.filter(it => it.name === value)[0]
        });
    };

    handleParamsChanged = (params) => {
        let { onSubmit } = this.props;
        if (onSubmit) onSubmit(params);
    };

    handleSceneChanged = (menu) => {
        let { onSubmit, scenes = [], form } = this.props;
        let selectScene = scenes.filter(sc => sc.title === menu.key)[0];
        if (onSubmit && selectScene) {
            let values = form.getFieldsValue();
            Object.keys(values).forEach(k => values[k] = Array.isArray(values[k]) ? [] : null);
            form.setFieldsValue(Object.assign(values, selectScene.values));
            onSubmit(selectScene.values);
        }
    };

    autoSubmit = () => {
        let submitValues = this.props.form.getFieldsValue(), needSubmit = false, prevValues = this.prevSubmitValues;
        //对比上次和本次的值发生改变了再进行查询,因为存在表单项个数不确定的场景(高级查询),因此需要相互对比。
        Object.keys(submitValues).forEach(key => {
            let subCompValue = submitValues[key], prevCompValue = prevValues[key];
            if (Array.isArray(subCompValue)) subCompValue = subCompValue.join("");
            if (Array.isArray(prevCompValue)) prevCompValue = prevCompValue.join("");
            if ((subCompValue && subCompValue !== prevCompValue) || (!subCompValue && prevCompValue)) {
                needSubmit = true;
            }
        });

        if (needSubmit) {
            this.validate();
        }
    };

    render() {
        //let formChildrens=this.buildingFormElements();
        let { colCount, items, form, formItemLayout, actionData, nodeParam, extCol, action, scenes = [], autoSubmit, single } = this.props;
        let { displayMode, defaultFilter } = this.state, rows = false, normalItems = [], base = [], advs = [];
        if (!defaultFilter) return null;
        //把高级查询条件放置到最后
        let initParams = {};
        items.forEach(item => {
            let { defModel = {} } = item;
            let isVisable = true, { displayType } = defModel;
            initParams[item.name] = item.initValue;
            try {
                if (item.visibleExp && item.visibleExp.length > 4) {
                    isVisable = safeEval(item.visibleExp, { formValues: Object.assign(initParams, nodeParam, form.getFieldsValue()) });
                }
            } catch(e) {
                isVisable = true;
                return true
            }
            if (displayType === 'advanced' && displayMode === 'base') {
                this.hasType = true;
            }
            if (isVisable) {
                if (defModel.displayType === 'advanced') {
                    advs.push(item);
                } else {
                    base.push(item)
                }
            }
        });
        if (autoSubmit) this.autoSubmit();
        if (single) {
            return (
                <Form key="_filter_form" autoComplete="off">
                    <div style={{ minWidth: 170 }} className="oam-filter-bar-font">
                        {
                            FormElementFactory.createFormItem(Object.assign({}, defaultFilter, { label: '' }), form)
                        }
                    </div>
                </Form>
            )
        }
        const sceneMenu = (
            <Menu onClick={this.handleSceneChanged}>
                {
                    scenes.map(scene => {
                        return (
                            <Menu.Item key={scene.title}>
                                <span className="oam-filter-bar-font">{scene.title}</span>
                            </Menu.Item>
                        )
                    })
                }
            </Menu>
        );
        return (
            <div ref={r => this.formContainer = r}>
                <Form key="_filter_form" style={{ marginTop: 8 }} autoComplete="off">
                    <div className="singgle-filter-bar" style={{ display: 'flex', justifyContent: 'center', paddingTop: 8 }}>
                        <div style={{ marginTop: 12 }} className="oam-filter-bar-font">
                            <Select defaultValue={defaultFilter.name} onChange={this.handleFilterChanged}>
                                {
                                    base.map(normal => {
                                        return (
                                            <Option value={normal.name} key={normal.name}>
                                                <span className="oam-filter-bar-font">{normal.label}</span>
                                            </Option>
                                        )
                                    })
                                }
                            </Select>
                        </div>
                        <div style={{ marginTop: 8, marginLeft: -2, minWidth: 170 }} className="oam-filter-bar-font">
                            {
                                FormElementFactory.createFormItem(Object.assign({}, defaultFilter, { label: '' }), form)
                            }
                        </div>
                        {
                            extCol && extCol.length ?
                                <div style={{ marginTop: 12, marginLeft: 8 }}>
                                    {extCol}
                                </div>
                                : null
                        }
                        {
                            this.hasType ?
                                <div style={{ marginTop: 12, marginLeft: 8 }}>
                                    <Popover title="高级搜索" content={
                                        <OamWidget nodeParams={Object.assign({}, nodeParam)}
                                            widget={{
                                                type: 'ACTION_FORM',
                                                config: { action: action.name, advanced: false }
                                            }} nodeId={action.nodeId}
                                            actionData={actionData}
                                            handleParamsChanged={this.handleParamsChanged} />
                                    }
                                        trigger="click"
                                        placement="bottom"
                                        visible={displayMode === 'advanced'}
                                        overlayStyle={{ width: '80%' }}

                                    >
                                        <Button style={{ fontSize: 12, padding: 3 }} onClick={this.handleDisplayChanged}>{displayMode === 'base' ? localeHelper.get("common.advanced.query", "高级查询") : localeHelper.get("Search", "普通查询")}<LegacyIcon style={{ marginLeft: 5 }} type={displayMode === 'base' ? "down" : "up"} /></Button>
                                    </Popover>
                                </div>
                                : null
                        }
                        {
                            scenes.length ?
                                <div style={{ marginTop: 12, marginLeft: 8 }}>
                                    <Dropdown overlay={sceneMenu}>
                                        <Button style={{ fontSize: 12, padding: 3 }}>
                                            快捷过滤 <DownOutlined />
                                        </Button>
                                    </Dropdown>
                                </div>
                                : null
                        }
                    </div>
                </Form>
            </div>
        );
    }
}


export default Form.create()(FilterBar);
