import React, { Component } from "react";
import { EditOutlined, MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Tabs, Select, Button, Row, Col, Collapse, Input } from "antd";
import "./index.less";
import FormElementFactory from '../../../components/FormBuilder/FormElementFactory';
import Constants from '../../framework/model/Constants';
import AceEditor from "react-ace";
import service from "../../services/appMenuTreeService";
import _ from "lodash";
import uuid from "uuid/v4";
import brace from 'brace';
import "brace/mode/json";
import 'brace/mode/javascript';
import 'brace/theme/monokai';
import 'brace/theme/xcode';

const Panel = Collapse.Panel;
const { TabPane } = Tabs;
const { Option } = Select;
const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 4 },
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 16 },
    md: { span: 16 },
  },
};
const api = Constants.DATASOURCE_API;
const functionItem = Constants.DATASOURCE_FUNCTION;
const json = Constants.DATASOURCE_JSONDATA;
const CMDB = Constants.DATASOURCE_CMDB;
const nodeParams = Constants.DATASOURCE_NODEDATA;
const FILTER = "FILTER", DS_TYPE_ENTITY = "entity", DS_TYPE_NONE = "none", DS_TYPE_JSON = "json";
let tempDS = {}, tempFs = [];
let themeType = localStorage.getItem('sreworks-theme');
class DataSource extends Component {
  constructor(props) {
    super(props);
    this.state = {
      visibleMap: {},
      columns: [],
      filters: [],
      typeList: [
        { label: "API", value: api },
        { label: "函数", value: functionItem },
        { label: "JSON对象", value: json },
        /*{label: "CMDB", value: CMDB},*/
        /*{label: "节点属性", value: nodeParams},*/
      ],
      tables: [],
      value: {
        params: {}
      },
    };
  }

  componentDidMount() {
    this.setState({
      value: Object.assign({
        beforeRequestHandler: 'function beforeRequest(nodeParams,){\n  return {}\n}',
        afterResponseHandler: 'function afterResponse(respData){\n  return respData\n}',
        method: 'GET',
        type: ''
      }, this.props.value)
    });
    //this.initTables();
  }

  componentWillReceiveProps(nextProps, nextContext) {
    if (!_.isEqual(this.props, nextProps)) {
      this.setState({
        value: nextProps.value || {},
      });
    }
  }

  initTables = () => {
    service.getAppTables({ stageId: 'dev' }).then(tables => {
      this.setState({
        tables,
      });
    });
  };

  handleTableChange = res => {
    service.getColumnList({ table: res, product: "tesla", stageId: 'dev' }).then(columns => {
      this.setState({
        columns: columns,
      });
    });
  };

  jsonEditorInitialized = (jsonEditor) => {
    this.jsonEditor = jsonEditor;
  };

  handleClick = dataIndex => {
    let { visibleMap, value } = this.state, { form } = this.props;
    let allValues = form.getFieldsValue();
    visibleMap[dataIndex] = !visibleMap[dataIndex];
    this.setState({
      visibleMap,
      value: Object.assign({}, value, allValues)
    });
  };

  hiddenDom = (dom, label, dataIndex) => {
    const { getFieldDecorator } = this.props.form;
    let { visibleMap, value } = this.state;
    return (
      <div>
        <Form.Item  {...formItemLayout} label={label}>
          <Button type="link" icon={<EditOutlined />} onClick={e => this.handleClick(dataIndex)}>
            {visibleMap[dataIndex] ? "收起内容" : "编辑内容"}
          </Button>
          {visibleMap[dataIndex] && <Form.Item>
            {getFieldDecorator(dataIndex, {
              initialValue: dataIndex === 'JSON' && typeof value[dataIndex] === 'object' ? JSON.stringify(value[dataIndex], null, 2) : value[dataIndex],
            })(
              dom,
            )}
          </Form.Item>}
        </Form.Item>
      </div>
    );
  };

  removeFilter = (fkey) => {
    let { filters } = this.state, newFilters = [], { form, onChange } = this.props;
    filters.forEach(f => {
      if (f.key !== fkey) {
        newFilters.push(f);
      }
    });
    this.setState({
      filters: newFilters,
    }, () => {
      let allValues = form.getFieldsValue();
      //console.log("allValues------>",allValues);
      // options.filters = {};
      // Object.keys(allValues).forEach(fk => {
      //   if (fk.startsWith(FILTER)) {
      //     let fv = allValues[fk];
      //     options.filters[fv.filterName] = fv.filterValue;
      //   }
      // });
      // onChange && onChange(tempDS);
    });
  };

  addFilter = () => {
    let { filters } = this.state;
    let newFilter = filters.concat({ key: FILTER + uuid() });
    tempFs = newFilter;
    this.setState({
      filters: newFilter,
    });
  };

  renderFormItem = (type) => {
    console.log(type);
    let { options = {}, source = "" } = (tempDS.expand_service_type || {}), mapping = [];
    const { getFieldDecorator } = this.props.form;
    let { tables, columns, filters, value } = this.state;
    return (
      <div>
        {[api].indexOf(type) !== -1 && FormElementFactory.createFormItem({
          type: 1,
          name: "api",
          initValue: value.api,
          required: true,
          label: "接口地址",
        }, this.props.form, formItemLayout)}
        {[api].indexOf(type) !== -1 && FormElementFactory.createFormItem({
          type: 10,
          name: "method",
          initValue: value.method,
          required: true,
          label: "请求方式",
          optionValues: [{ value: "GET", label: "GET" }, { value: "POST", label: "POST" }],
        }, this.props.form, formItemLayout)}
        {[api].indexOf(type) !== -1 && FormElementFactory.createFormItem({
          type: 83,
          name: "params",
          initValue: value.params,
          required: false,
          label: "请求参数",
          showDiff: false,
          defModel: { height: 220, disableShowDiff: true, mode: "json" },
          height: 220
        }, this.props.form, formItemLayout)}
        {[api].indexOf(type) !== -1 && this.hiddenDom(
          <AceEditor
            mode="javascript"
            style={{ height: 400, width: 700 }}
            fontSize={12}
            theme={themeType === 'light' ? "xcode" : "monokai"}
            showPrintMargin={true}
            showGutter={true}
            highlightActiveLine={true}
            setOptions={{
              enableBasicAutocompletion: false,
              enableLiveAutocompletion: false,
              enableSnippets: false,
              showLineNumbers: true,
              tabSize: 2,
            }}
          />,
          "请求前参数处理",
          "beforeRequestHandler",
        )}
        {[api].indexOf(type) !== -1 && this.hiddenDom(
          <AceEditor
            mode="javascript"
            style={{ height: 400, width: 700 }}
            fontSize={12}
            theme={themeType === 'light' ? "xcode" : "monokai"}
            showPrintMargin={true}
            showGutter={true}
            highlightActiveLine={true}
            setOptions={{
              enableBasicAutocompletion: false,
              enableLiveAutocompletion: false,
              enableSnippets: false,
              showLineNumbers: true,
              tabSize: 2,
            }}
          />,
          "返回后数据处理",
          "afterResponseHandler",
        )}
        {[functionItem].indexOf(type) !== -1 && this.hiddenDom(
          <AceEditor
            mode="javascript"
            style={{ height: 400, width: 700 }}
            fontSize={12}
            theme={themeType === 'light' ? "xcode" : "monokai"}
            showPrintMargin={true}
            showGutter={true}
            highlightActiveLine={true}
            setOptions={{
              enableBasicAutocompletion: false,
              enableLiveAutocompletion: false,
              enableSnippets: false,
              showLineNumbers: true,
              tabSize: 2,
            }}
          />,
          "函数",
          "function",
        )}
        {[json, nodeParams].indexOf(type) !== -1 && this.hiddenDom(
          <AceEditor
            mode="json"
            style={{ height: 400, width: 700 }}
            fontSize={12}
            theme={themeType === 'light' ? "xcode" : "monokai"}
            showPrintMargin={true}
            showGutter={true}
            highlightActiveLine={true}
            setOptions={{
              enableBasicAutocompletion: false,
              enableLiveAutocompletion: false,
              enableSnippets: false,
              showLineNumbers: true,
              tabSize: 2,
            }}
          />,
          "JSON",
          "JSON",
        )}
        {[CMDB].indexOf(type) !== -1 &&
          <div>
            <Form.Item label="节点实体" {...formItemLayout}>
              {getFieldDecorator("entityName", {
                initialValue: value.entityName,
              })(
                <Select placeholder="选择节点实体" showSearch onSelect={this.handleTableChange}>
                  {tables.map(t => {
                    return <Option value={t.tableName} key={t.tableName}>{t.alias}({t.tableName})</Option>;
                  })}
                </Select>,
              )}
            </Form.Item>
            <Form.Item label="节点名称" {...formItemLayout}>
              {getFieldDecorator("name", {
                initialValue: value.name,
              })(
                <Select placeholder="请选择列" showSearch>
                  {columns.map(c => {
                    return <Option value={c.columnName} key={c.columnName}>{c.comment}({c.columnName})</Option>;
                  })}
                </Select>,
              )}
            </Form.Item>
            {
              filters.map(filter => {
                return (
                  <Row key={filter.key} type="flex" justify="center">
                    <Col span={9}>
                      <Form.Item label="实体属性" {...formItemLayout}>
                        {getFieldDecorator(`${filter.key}.filterName`, {
                          initialValue: filter.filterName,
                        })(
                          <Select placeholder="请选择列" showSearch>
                            {columns.map(c => {
                              return <Option value={c.columnName} key={c.columnName}>{c.comment}({c.columnName})</Option>;
                            })}
                          </Select>,
                        )}
                      </Form.Item>
                    </Col>
                    <Col span={9}>
                      <Form.Item label="等于" {...formItemLayout} colon={false}>
                        {getFieldDecorator(`${filter.key}.filterValue`, {
                          initialValue: filter.filterValue,
                        })(
                          <Input placeholder="输入过滤值,父节点${父节点实体.列名}" />,
                        )}
                      </Form.Item>
                    </Col>
                    <Col span={1}>
                      <MinusCircleOutlined style={{ marginTop: 12 }} onClick={() => this.removeFilter(filter.key)} />
                    </Col>
                  </Row>
                );
              })
            }
            <Button type="dashed" onClick={this.addFilter} style={{ width: "60%", marginLeft: "20%", marginBottom: 10 }}>
              <PlusOutlined /> 添加过滤
            </Button>
          </div>

        }
      </div>
    );
  };

  handleSubmit = () => {
    this.props.form.validateFieldsAndScroll((err, values) => {
      if (!err) {
        this.props.onChange(values);
      }
    });
  };

  render() {
    const { getFieldDecorator, getFieldValue } = this.props.form;
    let { typeList, value } = this.state;
    let options = {
      modes: ["code", "tree"],
    };
    return <div className="card-tab-panel">
      <Form>
        <Form.Item {...formItemLayout} label="数据源类型">
          {getFieldDecorator("type", {
            rules: [{ required: false, message: "请选择数据源类型" }],
            initialValue: value.type,
          })(<Select placeholder="请选择数据源类型" allowClear>
            {typeList.map(item => (
              <Option key={item.value}>{item.label}</Option>
            ))}
          </Select>)}
        </Form.Item>
        {this.renderFormItem(getFieldValue("type"))}
      </Form>
      {!this.props.onValuesChange && <div style={{ textAlign: "center" }}>
        <Button type="primary" onClick={this.handleSubmit}>
          保存
        </Button>
      </div>}
    </div>;
  }
}

export default Form.create({
  onValuesChange: (props, changedValues, allValues) => {
    //存在隐藏项因此,需要进行值合并处理
    props.onValuesChange && props.onValuesChange(changedValues, allValues.type ? Object.assign({}, props.value, allValues) : null);
  },
})(DataSource);
// export default Form.create()(DataSource);