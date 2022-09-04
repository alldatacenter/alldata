import React, {Component} from "react";
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import {
  Avatar,
  Card,
  List,
  Popover,
  Spin,
  Alert,
  Drawer,
  Button,
  Row,
  Col,
  Tooltip,
} from "antd";
import FormElementFactory from "../../../components/FormBuilder/FormElementFactory";
import uuid from "uuid/v4";
import AceEditor from "react-ace";
import "brace/mode/javascript";
import "brace/theme/monokai";

const {Meta} = Card;
const colors = ["#90ee90", "#2191ee", "#9a69ee", "#41ee1a", "#484aee", "#6B8E23", "#48D1CC", "#3CB371", "#388E8E", "#1874CD"];
const formItemLayout = {
  labelCol: {
    xs: {span: 24},
    sm: {span: 6},
  },
  wrapperCol: {
    xs: {span: 24},
    sm: {span: 12},
    md: {span: 10},
  },
};

class FunctionEditor extends Component {
  constructor(props) {
    super(props);
    this.state = {
      data: [],
      visible: false,
      nowData: {},
    };
  }

  onClose = () => {
    this.setState({
      visible: false,
      nowData: {},
    });
  };

  handleAdd = () => {
    this.setState({
      visible: true,
    });
  };

  getItems = () => {
    let {nowData} = this.state;
    return [{
      type: 1,
      name: "name",
      initValue: nowData.name,
      required: true,
      label: "函数名称",
    },
      {
        type: 2,
        name: "description",
        initValue: nowData.description,
        required: false,
        label: "描述信息",
      }];

  };

  handleSave = () => {
    this.props.form.validateFields((err, values) => {
      if (!err) {
        let {nowData, data} = this.state;
        let params = {...nowData, ...values};
        if (params.id) {
          data.map((item, index) => {
            if (item.id === params.id) {
              data.splice(index, 1, params);
            }
          });
        } else {
          values.id = uuid();
          data.push({
            ...values,
          });
        }
        this.setState({
          data,
          nowData: {},
          visible: false,
        }, () => {
          this.props.onValuesChange && this.props.onValuesChange(data);
        });
      }
    });
  };

  handleDelete = index => {
    let {data} = this.state;
    data.splice(index, 1);
    this.setState({
      data,
    }, () => {
      this.props.onValuesChange && this.props.onValuesChange(data);
    });
  };

  handleEdit = (record, index) => {
    this.setState({
      nowData: record,
      visible: true,
    });
  };

  render() {
    let {data, nowData} = this.state;
    const {getFieldDecorator} = this.props.form;
    return (
      <div style={{padding: 10}}>
        <Alert message="页面中所使用的函数信息在此处配置" type="info" closable/>
        <div style={{display: "flex", flexWrap: "wrap"}}>
          <div style={{flex: "0 0 calc(25% - 20px)", margin: 10}}>
            <Card
              onClick={this.handleAdd}
              style={{height: 145, border: "1px dashed #e8e8e7"}}
              hoverable>
              <div style={{
                color: "#8e8e8e",
                left: "50%",
                position: "absolute",
                top: "50%",
                transform: "translate(-50%, -50%)",
                textAlign: "center",
              }}>
                <PlusOutlined /> 新建函数
              </div>
            </Card>
          </div>
          {
            data.map((item, index) => {
              let avatar = <Avatar style={{
                backgroundColor: colors[index % 10],
                verticalAlign: "middle",
                fontSize: "18px",
              }}>
                {(item.name).substring(0, 1)}
              </Avatar>;
              return (
                <div style={{flex: "0 0 calc(25% - 20px)", margin: 10}}>
                  <Card
                    hoverable
                    actions={[
                      <span onClick={() => this.handleEdit(item, index)}><EditOutlined key="edit" /> 编辑</span>,
                      <span onClick={() => this.handleDelete(index)}><DeleteOutlined key="delete" /> 删除</span>,
                    ]}>
                    <Meta
                      avatar={avatar}
                      title={item.name}
                      description={<div style={{
                        whiteSpace: "nowrap",
                        textOverflow: "ellipsis",
                        overflow: "hidden",
                        wordBreak: "break-all",
                      }}>
                        <Tooltip title={item.description}>{item.description}</Tooltip></div>}
                    />
                  </Card>
                </div>
              );
            })
          }
        </div>
        <Drawer
          width={800}
          destroyOnClose
          title={nowData.id ? "编辑函数" : "新建函数"}
          placement="right"
          onClose={this.onClose}
          visible={this.state.visible}
        >
          <Form>
            {this.getItems().map(item => {
              return FormElementFactory.createFormItem(item, this.props.form);
            })}
            <Form.Item  {...formItemLayout} label="函数">
              {getFieldDecorator("function", {
                initialValue: nowData.function,
              })(
                <AceEditor
                  mode="javascript"
                  fontSize={12}
                  theme="monokai"
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
              )}
            </Form.Item>
            <div style={{textAlign: "right"}}>
              <Button type="primary" onClick={this.handleSave} style={{marginRight: 10}}>保存</Button>
              <Button onClick={this.onClose}>取消</Button>
            </div>
          </Form>
        </Drawer>
      </div>
    );
  }
}

export default Form.create()(FunctionEditor);