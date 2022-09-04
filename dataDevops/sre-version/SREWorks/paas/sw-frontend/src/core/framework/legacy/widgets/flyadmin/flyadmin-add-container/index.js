import { Form } from "@ant-design/compatible";
import "@ant-design/compatible/assets/index.css";
import { Input, Row, Col, Button, Select, Collapse, Radio, Switch } from "antd";
import React from "react";
import SimpleForm from "../../../../../../components/FormBuilder/SimpleForm";

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

const Option = Select.Option;
const { TextArea } = Input;
const { Panel } = Collapse;

class FlyAdminAddContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      visible: false,
      reload: false,
      loading: false,
      data: {},
      expand: false,
      containerObjectList: [],
      nowContainer: {},
      editContainerConfig: {
        "dataSource": [],
        "pagination": true,
        "enableEdit": true,
        "enableRemove": true,
        "columns": [
          {
            "editProps": {
              "required": 2,
              "type": 1,
            },
            "dataIndex": "name",
            "title": "变量名",
          },
          {
            "editProps": {
              "required": 1,
              "type": 1,
            },
            "dataIndex": "value",
            "title": "值",
          },
        ],
        "enableAdd": true,
      },
    };
  }

  handleSubmit = () => {
    this.props.form.validateFieldsAndScroll((err, values) => {
      if (!err && this.preCustomButtonSubmit.validate()) {
        let { mode = {}, nowData } = this.props;
        let { config = {} } = mode;
        values = { ...nowData, ...values, ...this.preCustomButtonSubmit.validate() };
        values.type = nowData.name ? "edit" : "new";
        values.containerType = config.containerType;
        if (values.repoType === "INNER_REPO") {
          values.repoGroup = values.repoGroup || "alisre";
          values.repoDomain = values.repoDomain || "";
          values.repo = `${values.repoDomain}/${values.repoGroup}/` + values.appName + ".git";
        }
        if (config.type === "JOB") {

        } else {
          this.props.onOk(values);
        }
      }
    })

  };

  preCustomButtonSubmit = (params) => {
    return params;
  };

  render() {
    let { nowData } = this.props;
    let { editContainerConfig, loading } = this.state;
    let items = [{
      type: 1, name: "name",
      validateReg: "^[0-9a-zA-Z\-.]*$##名称只能包含字母数字和中划线",
      initValue: nowData.name || this.props.name,
      required: true, label: "容器名称",
    }, {
      type: 1, name: "dockerfileTemplate",
      initValue: nowData.dockerfileTemplate || this.props.dockerfileTemplate,
      required: false, label: "Dockerfile 模版",
    }, {
      type: 85,
      label: "Dockerfile 模版参数",
      name: "dockerfileTemplateArgs",
      initValue: nowData.dockerfileTemplateArgs || this.props.dockerfileTemplateArgs || [],
      defModel: { ...editContainerConfig },
    }, {
      type: 85,
      label: "构建参数",
      name: "buildArgs",
      initValue: nowData.buildArgs || this.props.buildArgs || [],
      defModel: { ...editContainerConfig },
    }, {
      type: 2,
      label: "启动命令",
      name: "command",
      initValue: nowData.command || this.props.command,
    }];
    const { getFieldDecorator, getFieldValue } = this.props.form;
    return <Form>
      <SimpleForm
        formItemLayout={formItemLayout}
        items={items}
        onSubmit={this.preCustomButtonSubmit}
      />
      <Form.Item label="代码仓库" {...formItemLayout}>
        {getFieldDecorator("repoType", {
          initialValue: (nowData.repoType) || "INNER_REPO",
        })(<Radio.Group>
          <Radio value="INNER_REPO">内置仓库</Radio>
          <Radio value="THIRD_REPO">第三方仓库</Radio>
        </Radio.Group>)}
      </Form.Item>
      {
        getFieldValue("repoType") === "THIRD_REPO" &&
        <div>
          <Form.Item label="Git 仓库地址" {...formItemLayout}>
            {getFieldDecorator("repo", {
              initialValue: (nowData.repo),
              rules: [{ required: true, message: "请输入 Git 仓库地址" }],
            })(
              <Input placeholder="请输入 Git 仓库地址" />,
            )}
          </Form.Item>
        </div>
      }
      {getFieldValue("repoType") === "INNER_REPO" && <Form.Item label="Git 仓库地址" {...formItemLayout}>
        {getFieldDecorator("appName", {
          initialValue: nowData.appName,
          rules: [{ required: true, message: "请输入 Git 仓库地址" }],
        })(
          <Input placeholder="请输入 Git 仓库地址"
            addonBefore={`${(nowData.repoDomain) || ""}/${(nowData.repoGroup) || "alisre"}/`}
            addonAfter=".git" />,
        )}
      </Form.Item>}
      <Form.Item label="默认分支" {...formItemLayout}>
        {getFieldDecorator("branch", {
          initialValue: (nowData.branch) || "master",
          rules: [
            {
              required: true,
              message: "请输入默认分支",
            },
          ],
        })(<Input placeholder="请输入默认分支" />)}
      </Form.Item>
      <Form.Item label="是否新建 Git 仓库" {...formItemLayout}>
        {getFieldDecorator("openCreateRepo", {
          valuePropName: "checked",
          initialValue: nowData.openCreateRepo,
        })(<Switch />)}
      </Form.Item>
      <Form.Item label="CI Account" {...formItemLayout}>
        {getFieldDecorator("ciAccount", {
          initialValue: (nowData.ciAccount),
          rules: [{ required: false, message: "请输入 CI Account" }],
        })(
          <Input placeholder="请输入 CI Account" />,
        )}
      </Form.Item>
      <Form.Item label="CI Token" {...formItemLayout}>
        {getFieldDecorator("ciToken", {
          initialValue: nowData.ciToken,
          rules: [{ required: false, message: "请输入 CI Token" }],
        })(
          <Input type="password" placeholder="请输入 CI Token" />,
        )}
      </Form.Item>
      <Row style={{ marginTop: 20 }}>
        <Col span={14} offset={6}>
          <Button loading={loading} type="primary" onClick={this.handleSubmit}>
            保存
          </Button>
        </Col>
      </Row>
    </Form>;
  }

}


FlyAdminAddContainer.propTypes = {};
FlyAdminAddContainer.defaultProps = {};

export default Form.create()(FlyAdminAddContainer);