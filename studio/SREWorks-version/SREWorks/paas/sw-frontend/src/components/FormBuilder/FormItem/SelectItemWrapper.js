/**
 * Created by caoshuaibiao on 2019/7/9.
 * 选择类型的包装器,主要用于工厂输出value为option的场景,及后续的通用下拉选择组件等
 */

import React from 'react';
import { CheckOutlined, RedoOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import {
    Spin,
    Button,
    Card,
    Modal,
    Tooltip,
    List,
    Row,
    Col,
    Cascader,
    Select,
    Input,
    Divider,
} from 'antd';
import localeHelper from '../../../utils/localeHelper';
import debounce from 'lodash.debounce';
import api from '../api';
import _ from 'lodash';

const FormItem = Form.Item;
const { Option } = Select;
export default class SelectItemWrapper extends React.Component {

    constructor(props) {
        super(props);
        //初始化时,需要按照value得出option派发出去
        let { item, selectType } = this.props, initOptions = false;
        let { defModel = {} } = item;
        let { needOption, remote, enableSelectAll } = defModel;
        //暂不支持级联的option输出
        if (needOption && selectType !== 'cascader') {
            this.needOption = needOption;
            if (props.value) {
                let moptionsValue = item.optionValues || [];
                moptionsValue.forEach(function (option) {
                    if (Array.isArray(props.value)) {
                        if (props.value.includes(option.value)) {
                            initOptions = initOptions || [];
                            initOptions.push(option);
                        }
                    } else {
                        if (option.value === props.value) initOptions = option;
                    }
                });
            }
        }
        this.state = {
            selectValue: props.value,
            options: props.item.optionValues || [],
            fetching: false,
            initOptions: initOptions
        };
        if (remote) {
            this.params = defModel.params || [];
            this.remote = true;
            this.lastDependParams = null;
            this.fetchOptions = debounce(this.fetchOptions, 500);
        }
        this.enableSelectAll = enableSelectAll;
    }


    fetchOptions = (value) => {
        let { form, item } = this.props;
        this.setState({ options: [], fetching: true });
        let formParams = form.getFieldsValue(this.params), queryParams = {};
        Object.keys(formParams).forEach(key => {
            let paramValue = formParams[key];
            if (Array.isArray(paramValue)) {
                paramValue = paramValue.join(",");
            }
            queryParams[key] = paramValue;
        });
        api.getItemData(item.api, Object.assign({ [item.name]: value }, queryParams)).then(data => {
            let options = data.hasOwnProperty("options") ? data.options : data;
            if (data.hasOwnProperty("items")) {
                options = data.items;
            }
            let { defModel = {} } = item;
            let { remote, transform, optionMapping } = defModel;
            let { selectValue } = this.state;
            if (remote) {
                if (data.defaultValue || data.initValue) {
                    selectValue = data.defaultValue || data.initValue
                }
                if (transform) {
                    if (transform.initValue) {
                        selectValue = _.get(data, transform.initValue);
                    }
                    options = _.get(data, transform.options);
                }
                if (optionMapping) {
                    options = options.map(o => {
                        return Object.assign({}, o, {
                            label: _.get(o, optionMapping.label),
                            value: _.get(o, optionMapping.value)
                        })
                    });
                }
            }
            this.setState({
                options,
                selectValue,
                fetching: false
            });
        });
    };

    componentWillReceiveProps(nextProps) {
        if (!_.isEqual(this.state.selectValue, nextProps.value)) {
            this.setState({
                selectValue: nextProps.value
            });
        }
    }

    componentDidMount() {
        let { initOptions } = this.state, { form, item } = this.props;
        if (initOptions) {
            form.setFieldsValue({
                [item.name + "Option"]: initOptions,
            });
        }
        let { defModel = {} } = item;
        let { remote } = defModel;
        if (remote) {
            //remote 远程获取数据时
            //可编辑表格 一进来组件就先获取值列表避免获取不到值列表而无法显示label名称
            this.handleOnFocus();
        }
    }

    onChange = (value, option) => {
        let { onChange, selectType, form, item } = this.props;
        this.setState({
            selectValue: value
        });
        let selectOptions = null;
        if (this.needOption) {
            if (selectType === 'cascader') {
                selectOptions = option[option.length - 1];
            } else {
                if (Array.isArray(option)) {
                    selectOptions = option.map(o => {
                        return o.props || o;
                    })
                } else {
                    selectOptions = option.props.option || option.props || option;
                }
            }
            form.setFieldsValue({
                [item.name + "Option"]: selectOptions,
            });
        }
        onChange && onChange(value);
    };

    handleOnFocus = () => {
        //依赖项的值与上次比无变化时不进行查询
        let { form } = this.props;
        let nowParams = form.getFieldsValue(this.params);
        if (!_.isEqual(nowParams, this.lastDependParams)) {
            this.fetchOptions();
            this.lastDependParams = nowParams;
        }
    };

    handleSelectAll = () => {
        let { options = [] } = this.state;
        let allValues = options.map(op => op.value);
        this.setState({
            selectValue: allValues
        });
        this.onChange(allValues, options)
    };

    handleCancel = () => {
        this.onChange([], [])
    };

    render() {
        let { item, selectType, form } = this.props, selectItem = null, { selectValue, options = [], fetching, initOptions } = this.state, dynamicProps = {};
        if (this.remote) {
            dynamicProps.onSearch = this.fetchOptions;
            dynamicProps.notFoundContent = fetching ? <Spin size="small" /> : <span>{localeHelper.get('noData', "未找到")}</span>
            dynamicProps.onFocus = this.handleOnFocus;
        }
        if (this.enableSelectAll && item.type === 4) {
            dynamicProps.dropdownRender = (menu) => (
                <div>
                    {menu}
                    <Divider style={{ margin: '4px 0' }} />
                    <div
                        style={{ padding: '4px 8px', cursor: 'pointer' }}
                        onMouseDown={e => e.preventDefault()}
                    >
                        <div style={{ textAlign: 'center' }}>
                            <Button.Group style={{ width: '100%' }} >
                                <Button type="dashed" style={{ width: '50%' }} onClick={this.handleSelectAll}>
                                    <CheckOutlined />{localeHelper.get('checkall', "全选")}
                                </Button>
                                <Button type="dashed" style={{ width: '50%' }} onClick={this.handleCancel}>
                                    <RedoOutlined />{localeHelper.get('ButtonCancel', "清空")}
                                </Button>
                            </Button.Group>
                        </div>
                    </div>
                </div>
            )
        }
        const { getFieldDecorator } = form;
        if (selectType === 'select') {
            selectItem = (
                <Select
                    showSearch
                    allowClear={true}
                    mode={item.type === 4 ? "multiple" : ""}
                    optionFilterProp="children"
                    placeholder={item.inputTip ? item.inputTip : (localeHelper.get('common.placeInput', "请输入") + item.label)}
                    value={selectValue}
                    onChange={this.onChange}
                    {...dynamicProps}
                >
                    {
                        options.map(option => <Option key={option.label} value={option.value} option={option}>{option.label}</Option>)
                    }
                </Select>
            );
        } else if (selectType === 'cascader') {
            selectItem = <Cascader allowClear={true} options={options || []} {...item} onChange={this.onChange} />;
        }
        return (
            <div className="singleFilter">
                {selectItem}
                <div style={{ display: 'none' }}>
                    {
                        this.needOption ?
                            <FormItem key={item.name + "Option"}>
                                {getFieldDecorator(item.name + "Option", { rules: [{ required: false }], initialValue: initOptions })(
                                    <Input />
                                )}
                            </FormItem>
                            : null
                    }
                </div>
            </div>
        )
    }
}