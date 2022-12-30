/**
 * Created by caoshuaibiao on 2019/4/25.
 * 级联选择组,用于多个select之间有相互影响依赖的场景,如A 的选择值会影响 B 与C,那么ABC为一组级联选择组,初始级只支持单选,后面依赖者支持多选
 */

import React, { PureComponent } from 'react';
import { InfoCircleOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Select, Spin, Cascader, Input, Tooltip } from 'antd';
import localeHelper from '../../../utils/localeHelper';
import debounce from 'lodash.debounce';
import api from '../api';
import JSXRender from "../../../components/JSXRender";
import _ from 'lodash';

const FormItem = Form.Item;

const Option = Select.Option;
class CascadeGroup extends PureComponent {

    constructor(props) {
        super(props);
        let { model } = this.props, modelJson = model.defModel, dynamicOptions = {};
        if (modelJson) {
            let { formInitParams } = model.extensionProps || {};
            modelJson.items.forEach(sit => {
                if (formInitParams && formInitParams[sit.name]) {
                    sit.initValue = formInitParams[sit.name];
                }
                dynamicOptions[`${sit.name}Options`] = sit.options ? sit.options : [];
                dynamicOptions[`${sit.name}`] = sit.initValue;
            });
            this.inline = modelJson.inline;
        }
        this.state = {
            fetching: false,
            selectItems: modelJson.items,
            loading: false,
            ...dynamicOptions
        };
        this.fetchRemoteItemOption = debounce(this.fetchRemoteItemOption, 500);
        //引用一个查询计数器,解决链式反应后请求等待问题。
        this.reqCount = 0;
    }

    componentWillMount() {
        this.loadSelectItemsOption(this.state.selectItems);
    }

    getItemData = (item, dependParams) => {
        //存在静态数据源
        if (item.data) {
            let result = [];
            if (!dependParams) {
                result = item.data;
            } else {
                let defaultValue, hasDef = false;
                item.data.forEach(option => {
                    let { group } = option, inGroup = true;
                    Object.keys(dependParams).forEach(dk => {
                        if (group[dk] && group[dk] !== dependParams[dk]) {
                            inGroup = false;
                        }
                    });
                    if (inGroup) {
                        if (option.default) {
                            hasDef = true;
                            defaultValue = option.value;
                        }
                        result.push({ label: option.label, value: option.value });
                    }
                });
                if (hasDef) {
                    result = {
                        defaultValue: defaultValue,
                        options: result
                    }
                }
            }
            return Promise.resolve(result);
        }
        return api.getItemData(item.url, dependParams);
    }
    //加载初始无依赖项
    loadSelectItemsOption = (selectItems, params) => {
        if (selectItems.length === 0) return;
        let req = [], reqSelectItems = [];
        selectItems.forEach(sit => {
            if (sit.url || sit.data) {
                if (!sit.depend) {
                    //req.push(api.getItemData(sit.url));
                    req.push(this.getItemData(sit));
                    reqSelectItems.push(sit);
                }
                if (sit.depend && params) {
                    if (Array.isArray(sit.depend)) {
                        let { form } = this.props, allParamsReady = true, dependParams = Object.assign({}, params);
                        //依赖项全部有值的情况下进行查询
                        sit.depend.forEach(dname => {
                            if (!params[dname]) {
                                if (!form.getFieldValue(dname)) {
                                    allParamsReady = false;
                                } else {
                                    dependParams[dname] = form.getFieldValue(dname);
                                }
                            }
                        });
                        if (allParamsReady) {
                            //req.push(api.getItemData(sit.url,dependParams));
                            req.push(this.getItemData(sit, dependParams));
                            reqSelectItems.push(sit);
                        }
                    } else {
                        //req.push(api.getItemData(sit.url,params));
                        req.push(this.getItemData(sit, params));
                        reqSelectItems.push(sit);
                    }
                }
            }
            if (sit.initValue) {
                this.handleChange(sit, sit.initValue)
            }
        });
        let dynStates = {}, { form } = this.props;
        if (req.length) {
            this.setState({
                loading: true
            });
            this.reqCount++;
        }
        Promise.all(req).then(datas => {
            this.reqCount--;
            datas.forEach((data, index) => {
                let selectItem = reqSelectItems[index];
                let dataOptions = data.options || data, defalutValue = data.defaultValue || data.initValue;
                let { data2option, transform, optionMapping } = selectItem;
                if (transform) {
                    if (transform.initValue) {
                        defalutValue = _.get(data, transform.initValue);
                        selectItem.initValue = defalutValue;
                    }
                    dataOptions = _.get(data, transform.options);
                }
                let mapping = data2option || optionMapping;
                if (mapping) {
                    dynStates[`${selectItem.name}Options`] = dataOptions.map(di => {
                        return {
                            value: _.get(di, mapping.value),
                            label: _.get(di, mapping.label)
                        }
                    })
                } else {
                    dynStates[`${selectItem.name}Options`] = dataOptions;
                }
                if (defalutValue) {
                    form.setFieldsValue({
                        [selectItem.name]: defalutValue,
                    });
                    this.handleChange(selectItem, defalutValue)
                }

            });
            this.setState({
                loading: this.reqCount > 0,
                ...dynStates
            });
        })
    };

    fetchRemoteItemOption = (item, searchText) => {
        let { form } = this.props, depend = Array.isArray(item.depend) ? item.depend : [item.depend];
        let params = form.getFieldsValue(depend);
        this.setState({
            fetching: true
        });
        params[item.name] = searchText;
        api.getItemData(item.url, params).then(result => {
            this.setState({
                fetching: false,
                [`${item.name}Options`]: result
            });
        });
    };

    handleChange = (item, value, option = {}) => {
        let { form, onChange } = this.props, { selectItems } = this.state, loadItems = [], loadParams = {}, clearOptions = {};
        //检查所有项目看是否有依赖改变的项目,有依赖则需要更新依赖其的可选数据
        const clearDepend = function (item) {
            selectItems.forEach(sit => {
                if (sit.depend === item.name || (Array.isArray(sit.depend) && sit.depend.includes(item.name))) {
                    form.setFieldsValue({
                        [sit.name]: sit.mode === "multiple" ? [] : "",
                    });
                    clearOptions[`${sit.name}Options`] = [];
                    clearDepend(sit);
                }
            });
        };
        selectItems.forEach(sit => {
            if (sit.depend === item.name || (Array.isArray(sit.depend) && sit.depend.includes(item.name))) {
                loadItems.push(sit);
            }
        });
        //依赖其的选项的值及可选值需要清空
        clearDepend(item);
        //清理依赖项的下拉选择内容
        if (Object.keys(clearOptions).length > 0) {
            this.setState({
                ...clearOptions
            })
        }
        if (item.mode === 'cascader') {
            if (item.fullPath) {
                loadParams[item.name] = Array.isArray(value) ? value.join(",") : value;
            } else {
                loadParams[item.name] = value[value.length - 1];
            }
        } else {
            loadParams[item.name] = Array.isArray(value) ? value.join(",") : value;
        }
        this.loadSelectItemsOption(loadItems, loadParams);
        //onChange&&onChange(value);
        //needOption是为了下拉选择把整个option选项输出
        if (item.needOption) {
            let selectOptions = null;
            if (Array.isArray(option)) {
                selectOptions = option.map(o => {
                    return o.props || o;
                })
            } else {
                if (option.props && option.props.optionData) {
                    selectOptions = option.props.optionData;
                } else {
                    selectOptions = option.props || option;
                }
            }
            form.setFieldsValue({
                [item.name + "Option"]: selectOptions,
            });
        }
    };

    render() {
        const { selectItems, loading, fetching } = this.state, { model } = this.props;
        const { getFieldDecorator } = this.props.form, fetchRemoteItemOption = this.fetchRemoteItemOption;
        let formItemLayout = this.props.layout || {
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
        let selectFormItem = [];
        if (this.inline) {
            formItemLayout = {
                wrapperCol: {
                    xs: { span: 24 },
                    sm: { span: 24 },
                    md: { span: 24 },
                },
            }
        }
        selectItems.forEach(sit => {
            let label = sit.label, dynamicProps = {};
            if (sit.tooltip) {
                label = (
                    <span key={sit.label}>
                        {label}
                        <em className="optional">
                            {sit.mark ? "  (" + sit.mark + ")" : null}
                            <Tooltip title={sit.tooltip}>
                                <InfoCircleOutlined style={{ marginRight: 4 }} />
                            </Tooltip>
                        </em>
                    </span>
                )
            }

            if (sit.searchType === 'remote') {
                dynamicProps.onSearch = (searchText) => fetchRemoteItemOption(sit, searchText);
                dynamicProps.notFoundContent = fetching ? <Spin size="small" /> : <span>{localeHelper.get('noData', "未找到")}</span>
            }

            selectFormItem.push(
                <div key={sit.name}>
                    <FormItem
                        {...formItemLayout}
                        label={label}
                        key={sit.name}
                    >
                        {getFieldDecorator(sit.name, {
                            initialValue: sit.initValue,
                            rules: [{
                                required: sit.required, message: sit.label + localeHelper.get('fromItem.required', "必选"),
                            }],
                        })(
                            sit.mode === 'cascader' ? <Cascader options={this.state[[`${sit.name}Options`]]} onChange={(value) => this.handleChange(sit, value)} {...dynamicProps} /> :
                                <Select
                                    mode={sit.mode}
                                    showSearch
                                    allowClear={true}
                                    placeholder={sit.tip ? sit.tip : localeHelper.get('pleaseChoose', "请选择")}
                                    optionFilterProp="children"
                                    onChange={(value, option) => this.handleChange(sit, value, option)}
                                    key={sit.name}
                                    style={{ width: '100%', minWidth: this.inline ? (sit.minWidth || 100) : 100 }}
                                    {...dynamicProps}
                                >
                                    {this.state[[`${sit.name}Options`]].map(d => <Option optionData={d} value={d.value} key={d.value}>{d.label}</Option>)}
                                </Select>
                        )}
                    </FormItem>
                    <div style={{ display: 'none' }}>
                        {
                            sit.needOption ?
                                <FormItem
                                    {...formItemLayout}
                                    label={sit.label + "xxx"}
                                    key={sit.name + "Option"}
                                >
                                    {getFieldDecorator(sit.name + "Option", {
                                        rules: [{ required: false }]
                                    })(
                                        <Input />
                                    )}
                                </FormItem>
                                : null
                        }
                    </div>
                    {
                        sit.jsxRemark ?
                            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
                                <JSXRender jsx={sit.jsxRemark} />
                            </div> : null
                    }
                </div>
            );
        });
        return (
            <Spin spinning={loading} key={model.name}>
                {
                    this.inline ? <div style={{ display: 'flex', minWidth: selectFormItem.length * 110 }}>{model.label ? <label style={{ width: '25%', marginTop: 8, textAlign: 'end' }}>{model.label}&nbsp;:&nbsp;</label> : ""}{selectFormItem}</div>
                        : selectFormItem
                }
            </Spin>
        );
    }
}

export default CascadeGroup;