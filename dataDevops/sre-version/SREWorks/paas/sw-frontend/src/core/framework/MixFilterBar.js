/**
 * Created by caoshuaibiao on 2020/4/21.
 * 组合过滤,可根据过滤器表单定义的过滤项进行任意的组合进行
 */
import React, { PureComponent } from 'react';
import { Form, Icon as LegacyIcon } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { FilterOutlined } from '@ant-design/icons';
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
    Tag,
} from 'antd';
import FormElementFactory from '../../components/FormBuilder/FormElementFactory';
import safeEval from '../../utils/SafeEval';
import OamWidget from './OamWidget';
import debounce from 'lodash.debounce';
import { TweenOneGroup } from 'rc-tween-one';
import localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";

const { Option } = Select;

class MixFilterBar extends PureComponent {

    constructor(props) {
        super(props);
        let { onSubmit, items } = props, defaultFilter = false, urlParams = util.getUrlParams(), filterTags = [];
        if (onSubmit) {
            onSubmit.validate = this.validate;
        }
        items.forEach(ni => {
            let { defModel = {} } = ni;
            if (defModel.defaultFilter) {
                defaultFilter = ni;
            }
            //约定以","分割值
            let tagValue = urlParams[ni.name];
            if (tagValue) {
                (tagValue + "").split(",").forEach(tv => {
                    filterTags.push(
                        {
                            label: ni.label,
                            value: tv,
                            name: ni.name
                        }
                    )
                })
            }
        });
        if (!defaultFilter) defaultFilter = items[0];
        this.state = {
            displayMode: 'base',
            defaultFilter: defaultFilter,
            filterTags: filterTags
        };
        this.hasType = false;
        this.autoSubmit = debounce(this.autoSubmit, 500);
        this.prevSubmitValues = {};
    }


    validate = () => {
        let pass = false, { onSubmit, items } = this.props, { filterTags } = this.state;
        this.props.form.validateFieldsAndScroll((err, values) => {
            if (!err) {
                /*let allValues={};
                Object.assign(allValues,values);
                this.prevSubmitValues=Object.assign({},allValues);
                if(onSubmit) onSubmit(allValues);
                pass=allValues;*/
                this.handleKeyUp();
            }
        });
        return pass
    };

    handleKeyUp = (event) => {
        if (event) {
            event.preventDefault();
        }
        let { defaultFilter, filterTags } = this.state, { form } = this.props;
        let { defModel = {} } = defaultFilter, itemValue = form.getFieldValue(defaultFilter.name);
        if (!event || event.keyCode === 13) {
            //支持按照","批量输入
            if (!Array.isArray(itemValue)) {
                itemValue = (itemValue + "").split(",");
            }
            itemValue.forEach(iv => {
                let exist = false;
                filterTags.forEach(ft => {
                    if (ft.label === defaultFilter.label) {
                        if (defModel.filterMode === 'replace') {
                            ft.value = iv;
                            exist = true;
                        } else if (iv === ft.value) {
                            exist = true;
                        }
                    }
                });
                if (!exist) {
                    filterTags.push(
                        {
                            label: defaultFilter.label,
                            value: iv,
                            name: defaultFilter.name
                        }
                    );
                }
            });
            this.setState({ filterTags: [...filterTags] });
            this.handleTagsSubmit(filterTags);
            form.setFieldsValue({ [defaultFilter.name]: undefined });
        }
    };

    handleTagsSubmit = (tags) => {
        let tagsValues = {}, { onSubmit } = this.props;
        tags.forEach(tag => {
            if (tagsValues[tag.name]) {
                tagsValues[tag.name] = tagsValues[tag.name] + "," + tag.value;
            } else {
                tagsValues[tag.name] = tag.value;
            }
        });
        if (onSubmit) onSubmit(tagsValues);
    };

    componentDidMount() {
        this.formContainer.addEventListener("keyup", this.handleKeyUp);
    }

    componentWillUnmount() {
        this.formContainer.removeEventListener("keyup", this.handleKeyUp);
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

    forMap = tag => {
        const tagElem = (
            <Tag
                closable
                onClose={e => {
                    e.preventDefault();
                    this.handleRemovedTag(tag);
                }}
            >
                {tag.label + " : " + tag.value}
            </Tag>
        );
        return (
            <span key={tag.label + "_" + tag.value} style={{ display: 'inline-block' }}>{tagElem}</span>
        );
    };

    handleRemovedTag = removedTag => {
        const filterTags = this.state.filterTags.filter(tag => tag !== removedTag);
        this.setState({ filterTags });
        this.handleTagsSubmit(filterTags);
    };

    handleClearTags = () => {
        this.setState({ filterTags: [] });
        this.handleTagsSubmit([]);
    };

    render() {
        let { colCount, items, form, formItemLayout, actionData, nodeParam, extCol, action, scenes = [], autoSubmit, single, filterConfig = {} } = this.props;
        let { displayMode, defaultFilter, filterTags } = this.state, rows = false, base = [], advs = [];
        if (!defaultFilter) return null;
        //把高级查询条件放置到最后
        items.forEach(item => {
            let { defModel = {} } = item;
            let isVisable = true, { displayType } = defModel;
            try {
                if (item.visibleExp && item.visibleExp.length > 4) {
                    isVisable = safeEval(item.visibleExp, { formValues: Object.assign(nodeParam, form.getFieldsValue()) });
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
        base = base.concat(advs);
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
        const tagChild = filterTags.map(this.forMap), { selectWidth = 70, inputWidth = 170 } = filterConfig;
        return (
            <div ref={r => this.formContainer = r}>
                <Form key="_filter_form" autoComplete="off">
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <div style={{ display: 'flex', flexWrap: "wrap" }}>
                            <div style={{ zIndex: 9 }} className="oam-filter-bar-font">
                                <Select defaultValue={defaultFilter.name} onChange={this.handleFilterChanged} style={{ minWidth: selectWidth }}>
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
                            <div style={{ marginTop: -4, marginLeft: -2, minWidth: inputWidth }} className="oam-filter-bar-font">
                                {
                                    FormElementFactory.createFormItem(Object.assign({}, defaultFilter, { label: '', initValue: undefined }), form)
                                }
                            </div>
                            {
                                extCol && extCol.length ?
                                    <div style={{ marginLeft: 8 }}>
                                        {extCol}
                                    </div>
                                    : null
                            }
                        </div>
                        <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                            {
                                this.hasType ?
                                    <div style={{ marginLeft: 12, width: "max-content" }}>
                                        <span><a style={{ fontSize: 12, padding: 3 }} onClick={this.handleDisplayChanged}>{displayMode === 'base' ? "高级查询" : "普通查询"}<LegacyIcon style={{ marginLeft: 5 }} type={displayMode === 'base' ? "down" : "up"} /></a></span>
                                    </div>
                                    : null
                            }
                        </div>
                    </div>
                </Form>
                {
                    filterTags.length ?
                        <div style={{ marginBottom: 8, display: 'flex', fontSize: 12, alignItems: 'center' }}>
                            <div>
                                <FilterOutlined style={{ marginLeft: 3, marginRight: 8 }} />检索项：
                            </div>
                            <div>
                                <TweenOneGroup
                                    enter={{
                                        scale: 0.8,
                                        opacity: 0,
                                        type: 'from',
                                        duration: 100,
                                        onComplete: e => {
                                            e.target.style = '';
                                        },
                                    }}
                                    leave={{ opacity: 0, width: 0, scale: 0, duration: 200 }}
                                    appear={false}
                                >
                                    {tagChild}
                                </TweenOneGroup>
                            </div>
                            <a style={{ marginLeft: 8 }} onClick={this.handleClearTags}>清除</a>
                        </div>
                        : null
                }
                {
                    displayMode === 'advanced' ?
                        <OamWidget nodeParams={Object.assign({}, nodeParam)}
                            widget={{
                                type: 'ACTION_FORM',
                                config: { action: action.name, advanced: false }
                            }} nodeId={action.nodeId}
                            actionData={actionData}
                            handleParamsChanged={this.handleParamsChanged} />
                        : null
                }
            </div>
        );
    }
}


export default Form.create()(MixFilterBar);
