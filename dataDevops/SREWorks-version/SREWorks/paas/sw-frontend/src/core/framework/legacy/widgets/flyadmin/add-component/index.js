import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Radio, Input, Table, Row, Col, Button, message } from "antd";
import React from "react";
import flyAdminService from "../service";
import FormElementFactory from "../../../../../../components/FormBuilder/FormElementFactory";
import _ from "lodash";

const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 24 },
    md: { span: 6 },
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 24 },
    md: { span: 18 },
  },
};

class FlyAdminAddComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      columns: [{
        title: "服务标识",
        dataIndex: "addonId",
      },
      {
        title: "描述信息",
        dataIndex: "addonDescription",
      },
      {
        title: "服务版本",
        dataIndex: "addonVersion",
      }],
      dataSourceMap: {},
    };
  }

  componentDidMount() {
    if (this.props.id) {
      //编辑模式
    } else {
      this.getComponentList("RESOURCE_ADDON");
      this.getComponentList("INTERNAL_ADDON");
    }

  }

  getComponentList = type => {
    let params = {
      "addonType": type,
      "appId": this.props.nodeParams.app_id,
    };
    let { dataSourceMap } = this.state;
    dataSourceMap[type] = [];
    flyAdminService.getComponentList(params)
      .then(res => {
        res.items.map(item => {
          if (item.addonType === type) {
            dataSourceMap[type].push(item);
          }
        });
        this.setState({ dataSourceMap });
      });
  };
  onSelectChange = (selectedRowKeys, selectedRows) => {
    this.setState({ selectedRowKeys, selectedRows });
  };

  handleSubmit = () => {
    let { selectedRowKeys, selectedRows } = this.state;
    this.props.form.validateFields((err, values) => {
      if (!err) {
        if (this.props.id) {
          values = { ...this.props, ...values };
          let transformParams = {
            id: this.props.id,
            // addonMetaId: nowData.addonMetaId,
            spec: values.spec,
            addonName: this.props.name,
            appId: this.props.nodeParams.app_id,
          };
          let service = flyAdminService.postAddon;
          if (this.props.id) {
            service = flyAdminService.putAddon;
          }
          this.setState({
            loading: true,
          });
          service(transformParams).then(res => {
            if (res) {
              message.success("编辑成功");
              this.setState({ loading: false }, this.props.closeAction && this.props.closeAction(true));
            }
          });
        } else {
          if (!!selectedRowKeys.length) {
            if (values.addonType === "INTERNAL_ADDON") {
              values.name = selectedRows[0].addonId;
            }
            let params = {
              id: this.props.id,
              addonMetaId: selectedRows[0].id,
              appId: this.props.nodeParams.app_id,
              addonName: values.name,
              spec: values.spec,
            };
            this.setState({
              loading: true,
            });
            let service = flyAdminService.postAddon;
            if (this.props.id) {
              service = flyAdminService.putAddon;
            }
            service(params).then(res => {
              if (res) {
                message.success("创建成功");
                this.setState({ loading: false }, this.props.onClose && this.props.onClose(true));
              }
            });
          } else {
            message.error("请选择资源服务");
          }
        }
      }
    });
  };

  handleChange = res => {
    if (res) {
      this.selectedRows = [];
      this.setState({ selectedRowKeys: [], selectedRows: [] });
    }
  };

  render() {
    const { getFieldDecorator, getFieldValue } = this.props.form;
    let { componentsSchema } = this.props;
    let { selectedRowKeys, columns, dataSourceMap, loading, selectedRows } = this.state;
    const rowSelection = {
      type: "radio",
      selectedRowKeys,
      onChange: this.onSelectChange,
    };
    return (
      <Form>
        {!this.props.id && <Form.Item label="服务类型" {...formItemLayout}>
          {getFieldDecorator("addonType", {
            initialValue: "INTERNAL_ADDON",
            rules: [
              {
                required: true,
                message: "请选择 服务类型",
              },
            ],
          })(<Radio.Group onChange={this.handleChange}>
            <Radio value="INTERNAL_ADDON">内置 Addon </Radio>
            <Radio value="RESOURCE_ADDON">资源 Addon </Radio>
          </Radio.Group>)}
        </Form.Item>}
        {
          getFieldValue("addonType") === "RESOURCE_ADDON" && <Form.Item label="服务 名称" {...formItemLayout}>
            {getFieldDecorator("name", {
              rules: [
                {
                  required: true,
                  message: "请输入 服务 名称",
                }, {
                  validator: (rule, value, callback) => {
                    if (value) {
                      let reg = /^[a-zA-Z][a-zA-Z0-9\-]{2,40}$/;
                      let v = reg.test(value);
                      if (v) {
                        callback();
                      } else {
                        callback("名称只能包含字母数字和中划线");
                      }

                    } else {
                      callback();
                    }
                  },
                },
              ],
            })(<Input placeholder="请输入 服务 名称" disabled={this.props.id} />)}
          </Form.Item>
        }
        {!this.props.id && <Form.Item label="开启 服务 " {...formItemLayout}>
          {getFieldDecorator("addonMetaId")(<Table rowKey="addonId"
            size="small"
            pagination={false}
            rowSelection={rowSelection}
            dataSource={dataSourceMap[getFieldValue("addonType")]}
            columns={columns} />)}
        </Form.Item>}
        {(!_.isEmpty(componentsSchema) ||
          (selectedRows && selectedRows[0] && selectedRows[0].componentsSchema && selectedRows[0].componentsSchema)) &&
          FormElementFactory.createFormItem({
            type: 79,
            name: "spec",
            required: false,
            defModel: {
              defaultValue: this.props.spec || (componentsSchema && componentsSchema.defaultValue) || (selectedRows[0] && selectedRows[0].componentsSchema && selectedRows[0].componentsSchema.defaultValue),
              schema: (componentsSchema && componentsSchema.schema) ||
                (selectedRows[0] && selectedRows[0].componentsSchema && selectedRows[0].componentsSchema.schema),
            },

            // componentsSchema ||
            // (selectedRows[0] && selectedRows[0].componentsSchema),
            initValue: this.props.spec, label: "设置",
          }, this.props.form)}
        <Row style={{ marginTop: 20 }}>
          <Col span={14} offset={6}>
            <Button loading={loading} type="primary" onClick={this.handleSubmit}>
              提交
            </Button>
          </Col>
        </Row>
      </Form>
    );
  }
}

FlyAdminAddComponent.propTypes = {};
FlyAdminAddComponent.defaultProps = {};

export default Form.create()(FlyAdminAddComponent);