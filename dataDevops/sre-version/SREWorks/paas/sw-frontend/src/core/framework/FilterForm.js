/**
 * Created by caoshuaibiao on 2019/11/24.
 * 过滤器
 */
import React, { PureComponent } from 'react';
import { DeleteOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { Form, Icon as LegacyIcon } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
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
    Radio,
    Tooltip,
} from 'antd';
import FormElementFactory from '../../components/FormBuilder/FormElementFactory';
import safeEval from '../../utils/SafeEval';
import debounce from 'lodash.debounce';
import localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";
import './tabFilter/index.less';
import JSXRender from '../../components/JSXRender';

class FilterForm extends PureComponent {

    constructor(props) {
        super(props);
        let { onSubmit, items, scenes = [] } = props, defaultFilter = false, sceneTitle = '';

        if (onSubmit) {
            onSubmit.validate = this.validate;
        }
        items.forEach(ni => {
            let { defModel = {} } = ni;
            if (defModel.defaultFilter) {
                defaultFilter = ni;
            }
        });
        //重入时候的选中
        let urlParams = util.getUrlParams(), defaultScene = false, empty = false;
        scenes.forEach(s => {
            let isDefault = true, svs = s.values;
            Object.keys(svs).forEach(sk => {
                if (urlParams[sk] && svs[sk] != urlParams[sk] || (!urlParams.hasOwnProperty(sk) && svs[sk])) {
                    isDefault = false
                }
            });
            if (Object.keys(svs).length === 0) {
                isDefault = false;
                empty = s;
            }
            if (isDefault && !defaultScene) {
                defaultScene = s;
                sceneTitle = s.title;
            }
        });
        if (!defaultScene && empty) {
            sceneTitle = empty.title;
        }
        if (!defaultFilter) defaultFilter = items[0];
        this.state = {
            displayMode: 'base',
            defaultFilter: defaultFilter,
            sceneTitle: sceneTitle
        };
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


    handleParamsChanged = (params) => {
        let { onSubmit } = this.props;
        if (onSubmit) onSubmit(params);
    };

    handleSceneChanged = (sceneTitle) => {
        let { onSubmit, form, scenes = [] } = this.props;
        let scene = scenes.filter(sc => sc.title === sceneTitle)[0];
        if (onSubmit && scene) {
            let values = form.getFieldsValue();
            Object.keys(values).forEach(k => values[k] = Array.isArray(values[k]) ? [] : null);
            form.setFieldsValue(Object.assign(values, scene.values));
            onSubmit(scene.values);
        }
        this.setState({ sceneTitle: scene.title });
    };

    handleDeleteScene = (sceneTitle) => {
        let { handleDeleteScene } = this.props;
        if (handleDeleteScene && sceneTitle) handleDeleteScene(sceneTitle);
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
        let { colCount, items, form, formItemLayout, hintFunction, nodeParam, extCol, action, scenes = [], autoSubmit } = this.props;
        let { displayMode, defaultFilter, sceneTitle } = this.state, rows = false, normalItems = [], base = [], advs = [], categoryScenes = {};
        if (!defaultFilter) return null;
        //增加是否存在场景分类判断
        scenes.forEach(scene => {
            let { category } = scene;
            if (category) {
                if (!categoryScenes[category]) categoryScenes[category] = [];
                categoryScenes[category].push(scene);
            }
        });
        let cKeys = Object.keys(categoryScenes);
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
        if (displayMode === 'advanced') {
            base = base.concat(advs);
        }
        if (autoSubmit) this.autoSubmit();
        return (
            <div >
                <div className="oam-filter-form-bar" ref={r => this.formContainer = r}>
                    <Form key="_filter_form" autoComplete="off">
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                            <div style={{ display: 'flex', flexWrap: "wrap" }}>
                                {
                                    base.map((normal, index) => {
                                        let { defModel = {} } = normal;
                                        return (
                                            <div style={{ display: 'flex', marginRight: index === base.length - 1 ? 0 : 24 }} key={index}>
                                                {
                                                    normal.label &&
                                                    <div className="ant-form-item-label" style={{marginTop:3}}>
                                                        <label title={normal.label}>{normal.label}
                                                            {
                                                                normal.tooltip &&
                                                                <em className="optional">
                                                                    <Tooltip title={<JSXRender jsx={normal.tooltip} />}>
                                                                        <QuestionCircleOutlined />
                                                                    </Tooltip>
                                                                </em>
                                                            }
                                                        </label>
                                                    </div>
                                                }
                                                <div style={{ minWidth: defModel.width || 110, marginLeft: 8 }} className="oam-filter-bar-font">
                                                    {
                                                        FormElementFactory.createFormItem(Object.assign({ inputTip: normal.inputTip || normal.tooltip }, normal, { label: '', tooltip: '' }), form)
                                                    }
                                                </div>
                                            </div>
                                        );
                                    })
                                }
                                <div style={{ display: 'flex', alignItems: 'center', marginLeft: 8, marginTop: -2 }}>
                                    {extCol}
                                </div>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                                {
                                    this.hasType ?
                                        <div style={{ marginLeft: 12, width: "max-content" }}>
                                            <span><a style={{ fontSize: 12, padding: 3 }} onClick={this.handleDisplayChanged}>{displayMode === 'base' ? localeHelper.get("common.advanced.query", "高级查询") : localeHelper.get("Search", "普通查询")}<LegacyIcon style={{ marginLeft: 5 }} type={displayMode === 'base' ? "down" : "up"} /></a></span>
                                        </div>
                                        : null
                                }
                            </div>
                        </div>
                    </Form>
                </div>
                <div>
                    {
                        scenes.length && cKeys.length === 0 ?
                            <div style={{ display: 'flex', marginTop: displayMode === 'base' ? 3 : 8 }}>
                                <Radio.Group size="small" defaultValue={sceneTitle} buttonStyle="solid" onChange={(e) => this.handleSceneChanged(e.target.value)}>
                                    {
                                        scenes.map((scene, index) => {
                                            return (
                                                <Radio.Button style={{ marginLeft: index === 0 ? 0 : 8 }} key={scene.title} value={scene.title}>
                                                    {scene.title}
                                                    {
                                                        sceneTitle === scene.title && (action.scenes || []).filter(ac => ac.title === scene.title).length === 0 ?
                                                            <a onClick={() => this.handleDeleteScene(scene.title)}><DeleteOutlined style={{ color: 'red', marginRight: -12 }} /></a>
                                                            : null
                                                    }
                                                </Radio.Button>
                                            );
                                        })
                                    }
                                </Radio.Group>
                            </div>
                            : null
                    }
                    {
                        cKeys.length ?
                            <div>
                                {
                                    cKeys.map((ckey) => {
                                        let cScenes = categoryScenes[ckey];
                                        return (
                                            <div style={{ display: 'flex', marginTop: 8 }}>
                                                <h4>{ckey}&nbsp;:&nbsp;</h4>
                                                <Radio.Group size="small" value={sceneTitle} buttonStyle="solid" onChange={(e) => this.handleSceneChanged(e.target.value)}>
                                                    {
                                                        cScenes.map((scene, index) => {
                                                            return (
                                                                <Radio.Button style={{ marginLeft: index === 0 ? 0 : 8 }} key={scene.title} value={scene.title}>
                                                                    {scene.title}
                                                                    {
                                                                        sceneTitle === scene.title && (action.scenes || []).filter(ac => ac.title === scene.title).length === 0 ?
                                                                            <a onClick={() => this.handleDeleteScene(scene.title)}><DeleteOutlined style={{ color: 'red', marginRight: -12 }} /></a>
                                                                            : null
                                                                    }
                                                                </Radio.Button>
                                                            );
                                                        })
                                                    }
                                                </Radio.Group>
                                            </div>
                                        );
                                    })
                                }
                            </div>
                            : null
                    }
                </div>
            </div>
        );
    }
}
class FilterFormContainer extends React.Component {
    constructor(props) {
        super(props)
    }
    render() {
        const { actionData } = this.props;
        if (actionData && actionData.config.hasBackground) {
            return <Card>
                <FilterForm {...this.props} />
            </Card>
        } else {
            return <FilterForm {...this.props} />
        }
    }
}

export default Form.create()(FilterFormContainer);
