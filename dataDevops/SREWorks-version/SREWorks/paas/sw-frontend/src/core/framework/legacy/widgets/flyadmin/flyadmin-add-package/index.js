import React from "react";
import { Form, Icon as LegacyIcon } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { CheckCircleFilled } from '@ant-design/icons';
import {
  Table,
  Steps,
  Input,
  Checkbox,
  Row,
  Col,
  Collapse,
  Button,
  message,
  Select,
  Spin,
} from "antd";
import flyAdminService from "../service";
import "./index.scss";
import localeHelper from "../../../../../../utils/localeHelper";
import { connect } from "react-redux";
import cacheRepository from '../../../../../../utils/cacheRepository'

const { Panel } = Collapse;
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

const { Step } = Steps;

const options = [
  { value: "MICROSERVICE", label: "微服务" },
  { value: "RESOURCE_ADDON", label: "资源 Addon" },
  { value: "INTERNAL_ADDON", label: "内置 Addon" },
  { value: "K8S_JOB", label: "Job" },
  // {value: "K8S_MICROSERVICE", label: "K8S 微服务"},
];

const typeList = ["MICROSERVICE", "RESOURCE_ADDON", "K8S_MICROSERVICE", "INTERNAL_ADDON", "K8S_JOB"];
@connect(({ node, global }) => ({
  userParams: Object.assign({}, { __currentUser__: global.currentUser }, node.userParams),
  userInfo: global.currentUser,
  product: global.currentProduct,
  nodeParams: Object.assign({}, node.nodeParams, node.urlParams, node.userParams, node.actionParams, node.remoteParams)
}))
class FlyAdminAddPackage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      current: 0,
      packageData: {},
      microserviceList: [],//微服务列表
      resourceAddonList: [],//依赖服务组件列表
      internalAddonList: [],//内置服务组件列表
      k8SMicroserviceList: [],
      jobList: [],
      expand: {},
      serviceMap: {
        "MICROSERVICE": [],
        "RESOURCE_ADDON": [],
        "INTERNAL_ADDON": [],
        "K8S_MICROSERVICE": [],
      },
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
            "dataIndex": "defaultValue",
            "title": "值",
          },
        ],
        "enableAdd": true,
      },
      dataOutputs: [],//输出参数列表
      selectedMicroServiceRowKeys: [],
      selectedInternalAddonRowKeys: [],
      selectedResourceAddonRowKeys: [],
      selectedK8SMicroServiceRowKeys: [],
      selectedJobRowKeys: [],
      columns: [{
        title: "名称",
        dataIndex: "componentLabel",
      }, {
        title: "类型",
        dataIndex: "componentType",
        render: (text) => {
          let textMap = {
            "MICROSERVICE": "进程模式",
            "RESOURCE_ADDON": "资源 Addon",
            "INTERNAL_ADDON": "内置 Addon",
            "K8S_MICROSERVICE": "容器模式（Kubernetes）",
            "K8S_JOB": "Job"
          };
          return textMap[text];
        },
      }, {
        title: "标识",
        dataIndex: "componentName",
      }],
      versionList: [],
      listMap: {
        "MICROSERVICE": "microserviceList",
        "RESOURCE_ADDON": "resourceAddonList",
        "INTERNAL_ADDON": "internalAddonList",
        "K8S_MICROSERVICE": "K8S_microserviceList",
      },
      loading: false,
      allSelectList: [],
      selectMicroserviceList: [],
      selectResourceAddonList: [],//依赖服务组件列表
      selectInternalAddonList: [],//内置服务组件列表
    };
  }

  componentDidMount() {
    this.getApplicationVersion();//获取应用版本号
    this.getApplicationServiceList();//获取应用中全部类型服务
  }

  getApplicationServiceList = () => {
    let params = {
      appId: this.props.nodeParams.app_id,
    };
    let { serviceMap } = this.state;
    serviceMap = {
      "MICROSERVICE": [],
      "RESOURCE_ADDON": [],
      "INTERNAL_ADDON": [],
      "K8S_MICROSERVICE": [],
      "K8S_JOB": [],
    };
    flyAdminService.getApplicationServiceList(params)
      .then(data => {
        data.map(item => {
          if (item.componentType && typeList.indexOf(item.componentType) !== -1) {
            (serviceMap[item.componentType] || []).push(item);
          }
        });
        this.setState({
          serviceMap,
        });
      });
  };

  getApplicationVersion = () => {
    let params = {
      appId: this.props.nodeParams.app_id,
    };
    let { packageData } = this.state;
    flyAdminService.getApplicationVersion(params)
      .then(data => {
        packageData["version"] = data;
        this.setState({
          packageData,
        });
      });
  };

  onChange = res => {
    console.log(res);
  };

  handleSubmit = res => {
    let {
      resourceAddonList,
      internalAddonList,
      k8SMicroserviceList,
      packageData,
      current,
      jobList,
    } = this.state;
    let params = {
      appId: this.props.nodeParams.app_id,
      version: packageData.version,
      tags: packageData.tags,
    };
    let components = [];
    for (let i in jobList) {
      if (!jobList[i].version) {
        message.error(`请选择或输入 Job ${jobList[i].componentName}的版本号`);
        return;
      }
    }
    for (let i in k8SMicroserviceList) {
      if (!k8SMicroserviceList[i].version) {
        message.error(`请选择或输入微服务${k8SMicroserviceList[i].componentName}的版本号`);
        return;
      }
    }
    jobList.map(item => {
      let paramBinderList = [];
      item.envList.map(env => {
        let envTem = {};
        if (env.valueType === "select") {
          envTem.componentName = env.componentName;
          envTem.dataInputName = env.name;
          envTem.componentType = env.componentType;
          envTem.dataOutputName = env.dataOutputName;
        } else {
          envTem.dataInputName = env.name;
          envTem.paramDefaultValue = env.paramDefaultValue;
        }
        paramBinderList.push(envTem);
      });
      let tem = {
        "componentLabel": item.componentLabel,
        "componentName": item.componentName,
        "componentType": item.componentType,
        "version": item.version,
        "dependComponentList": item.dependComponentList,
        paramBinderList,
      };
      components.push(tem);
    });
    k8SMicroserviceList.map(item => {
      let paramBinderList = [];
      item.envList.map(env => {
        let envTem = {};
        if (env.valueType === "select") {
          envTem.componentName = env.componentName;
          envTem.dataInputName = env.name;
          envTem.componentType = env.componentType;
          envTem.dataOutputName = env.dataOutputName;
        } else {
          envTem.dataInputName = env.name;
          envTem.paramDefaultValue = env.paramDefaultValue;
        }
        paramBinderList.push(envTem);
      });
      let tem = {
        "componentLabel": item.componentLabel,
        "componentName": item.componentName,
        "componentType": item.componentType,
        "version": item.version,
        "dependComponentList": item.dependComponentList,
        paramBinderList,
      };
      components.push(tem);
    });
    resourceAddonList.map(item => {
      let tem = {
        "componentLabel": item.componentLabel,
        "componentName": item.componentName,
        "componentType": item.componentType,
        "version": item.componentVersion,
      };
      components.push(tem);
    });
    internalAddonList.map(item => {
      let tem = {
        "componentLabel": item.componentLabel,
        "componentName": item.componentName,
        "componentType": item.componentType,
        "version": item.componentVersion,
      };
      components.push(tem);
    });
    params.components = components;
    this.setState({ loading: true });
    flyAdminService.postPackage(params)
      .then(res => {
        this.setState({ loading: false, current: current + 1 });
        // this.props.closeAction && this.props.closeAction(true);
      })
      .catch(err => {
        this.setState({ loading: false });
      });
  };

  onSelectMicroServiceChange = (res, type) => {
    this.setState({ [type]: res });
  };

  onSelectResourceAddonChange = res => {
    this.setState({ selectedResourceAddonRowKeys: res });
  };
  onSelectInternalAddonChange = res => {
    this.setState({ selectedInternalAddonRowKeys: res });
  };

  onSelectK8SMicroServiceChange = res => {
    this.setState({ selectedK8SMicroServiceRowKeys: res });
  };

  next = () => {
    this.props.form.validateFields((err, values) => {
      if (!err) {
        let { current, selectedMicroServiceRowKeys, jobList, selectedJobRowKeys, selectedResourceAddonRowKeys, selectedInternalAddonRowKeys, serviceMap, microserviceList, k8SMicroserviceList, resourceAddonList, internalAddonList } = this.state;
        //处理微服务的逻辑
        microserviceList = [];//微服务列表
        resourceAddonList = [];//依赖服务组件列表
        internalAddonList = [];//内置服务组件列表
        k8SMicroserviceList = [];
        jobList = [];
        selectedMicroServiceRowKeys.map(item => {
          //进程模式和容器模式合并显示
          //进程模式
          serviceMap["MICROSERVICE"].map(service => {
            if (service.componentName === item) {
              k8SMicroserviceList.push(service);
            }
          });
          //容器模式
          serviceMap["K8S_MICROSERVICE"].map(service => {
            if (service.componentName === item) {
              k8SMicroserviceList.push(service);
            }
          });
        });
        selectedJobRowKeys.map(item => {
          serviceMap["K8S_JOB"].map(service => {
            if (service.componentName === item) {
              jobList.push(service);
            }
          });
        });
        selectedResourceAddonRowKeys.map(item => {
          // resourceAddonList.push(serviceMap["RESOURCE_ADDON"][item]);
          serviceMap["RESOURCE_ADDON"].map(service => {
            if (service.componentName === item) {
              resourceAddonList.push(service);
            }
          });
        });
        selectedInternalAddonRowKeys.map(item => {
          // internalAddonList.push(serviceMap["INTERNAL_ADDON"][item]);
          serviceMap["INTERNAL_ADDON"].map(service => {
            if (service.componentName === item) {
              internalAddonList.push(service);
            }
          });
        });
        this.setState({
          microserviceList,
          resourceAddonList,
          internalAddonList,
          k8SMicroserviceList,
          jobList,
          allSelectList: microserviceList.concat(resourceAddonList).concat(internalAddonList).concat(k8SMicroserviceList).concat(jobList),
          packageData: { ...values },
          current: current + 1,
        }, () => {
          jobList.map((item, index) => {
            this.getListComponentVersions("jobList", index, item);
          });
          k8SMicroserviceList.map((item, index) => {
            this.getListComponentVersions("k8SMicroserviceList", index, item);
          });
        });
      }
    });
  };

  prev = () => {
    let { current } = this.state;
    this.setState({
      current: current - 1,
    });
  };

  //底部操作按钮
  footBar = () => {
    let { current } = this.state;
    return <div>
      <Row className="steps-action">
        <Col span={14} offset={5}>
          {current < 1 && (
            <Button type="primary" onClick={this.next}>
              下一步
            </Button>
          )}
          {current === 1 && (
            <Button style={{ margin: "0 8px 0 8px" }} onClick={() => this.prev()}>
              上一步
            </Button>
          )}
          {current === 1 && (
            <Button type="primary" onClick={this.handleSubmit}>
              提交
            </Button>
          )}</Col>
      </Row>
    </div>;
  };

  getDataInputList = (item, index, type) => {
    let list = this.state[type];//通用组件列表
    let params = { id: item.id, appId: this.props.nodeParams.app_id }
    flyAdminService.getMicroservice(params)
      .then(res => {
        res.envList.map(evn => {
          evn.valueType = "select";
        });
        list[index].branch = res.containerObjectList && res.containerObjectList[0] && res.containerObjectList[0].branch;
        list[index].envList = res.envList;
        this.setState({ [type]: list });
      });
  };

  getListComponentVersions = (type, index, res) => {
    let list = this.state[type];
    let params = {
      componentLabel: res.componentLabel,
      appId: this.props.nodeParams.app_id,
      componentName: res.componentName,
      componentType: res.componentType,
      componentVersion: res.componentVersion,
      id: res.id,
    };
    flyAdminService.getListComponentVersions(params)
      .then(data => {
        list[index].valueType = "select";
        list[index].versionList = data;
        list[index].version = data[0].name;
        this.setState({ [type]: list }, () => {
          this.getDataInputList(list[index], index, type);
        });
      });
  };

  onChangeMicroService = (res, item, record, field, type) => {
    let { k8SMicroserviceList, resourceAddonList } = this.state;
    let params = {};
    let list = this.state[type];
    list.map(k8s => {
      if (k8s.componentName === item.componentName) {
        k8s.envList.map(env => {
          if (env.name === record.name) {
            if (field === "componentName") {
              resourceAddonList.map(resource => {
                if (resource.componentName === res) {
                  env.componentName = resource.componentName;
                  env.componentType = resource.componentType;
                  env.componentVersion = resource.componentVersion;
                  params.appId = this.props.nodeParams.app_id;
                  env.dataOutputName = env.defaultValue;
                  params.addonName = resource.componentName;
                }
              });
              flyAdminService.getListDataOutputs(params)
                .then(data => {
                  env.dataOutputs = data;
                  this.setState({ [type]: list });
                });
            } else {
              env[field] = res;
              this.setState({ [type]: list });
            }
          }
        });
      }
    });

  };

  handleExpand = name => {
    let { expand } = this.state;
    expand[name] = !expand[name];
    this.setState({
      expand,
    });
  };

  formatK8SMicroServiceSelect = (item, index, type) => {
    let { resourceAddonList, allSelectList, expand } = this.state;
    return (
      <div>
        {/*选择k8s微应用大版本*/}
        <Row type="flex" align="middle" style={{ fontWeight: 500 }}>{index + 1}、{item.componentLabel}</Row>
        <Row type="flex" align="middle" className="env-title">
          <Col span={4} className="text-align-right">
            代码分支：
          </Col>
          <Col span={20}>
            <Input value={item.branch}
              placeholder="输入代码分支"
              onChange={res => this.handleChangeK8SMicroservice(res.target && res.target.value, index, "branch", type)} />
          </Col>
        </Row>
        <Row type="flex" align="middle" className="env-title">
          <Col className="text-align-right" span={4}>
            组件版本：
          </Col>
          <Col span={4}>
            <Select defaultValue="select"
              onChange={res => this.handleChangeK8SMicroservice(res, index, "valueType", type)}>
              <Option value="select">选择</Option>
              <Option value="input">输入</Option>
            </Select>
          </Col>
          <Col span={16}>
            {item.valueType === "select" && <Select value={item.version} placeholder={"请选择组件版本"}
              onChange={res => this.handleChangeK8SMicroservice(res, index, "version", type)}>
              {(item.versionList || []).map(option => {
                return <Option value={option.name}>{option.label}</Option>;
              },
              )}
            </Select>}
            {item.valueType === "input" &&
              <Input value={item.version}
                placeholder="输入版本号"
                onChange={res => this.handleChangeK8SMicroservice(res.target && res.target.value, index, "version", type)} />}
          </Col>
        </Row>
        <Row>
          <Col span={14} offset={4}><a className="trigger" onClick={() => this.handleExpand(item.componentName)}>
            {expand[item.componentName] ? localeHelper.get("common.close", "收起") : localeHelper.get("common.open", "展开")}
            <LegacyIcon
              type={expand[item.componentName] ? "up" : "down"} />
          </a>
          </Col>
        </Row>
        {expand[item.componentName] && <div>
          <Row type="flex" align="middle" className="env-title">
            <Col className="text-align-right" span={4}>
              依赖服务：
            </Col>
            <Col span={20}>
              <Select
                mode="multiple"
                value={item.dependComponentList || []}
                placeholder={"请选择依赖服务"}
                onChange={res => this.handleChangeK8SMicroservice(res, index, "dependComponentList", type)}>
                {allSelectList.filter(k8s => k8s.componentName !== item.componentName).map(option => {
                  return <Option value={option.componentName}>{option.componentLabel}</Option>;
                })}
              </Select>
            </Col>
          </Row>
          {/*微服务字段配置*/}
          <Row type="flex" align="middle" className="env-title">
            <Col className="text-align-right" span={4}>字段配置：</Col>
            <Col span={20}>
              <Table
                size="small"
                columns={[
                  {
                    title: "名称",
                    dataIndex: "name",
                    width: 200,
                  },
                  {
                    title: "输入方式",
                    dataIndex: "valueType",
                    width: 100,
                    render: (text, record, index) => {
                      return <Select defaultValue="select"
                        onChange={res => this.onChangeMicroService(res, item, record, "valueType", type)}>
                        <Option value="select">选择</Option>
                        <Option value="input">输入</Option>
                      </Select>;
                    },
                  },
                  {
                    title: "依赖组件",
                    dataIndex: "componentName",
                    render: (text, record) => {
                      return record.valueType === "input" ? "-" : <Select
                        value={text || undefined}
                        placeholder="请选择依赖组件"
                        onChange={res => this.onChangeMicroService(res, item, record, "componentName", type)}>
                        {resourceAddonList.map(option => {
                          return <Option value={option.componentName}>{option.componentLabel}</Option>;
                        })}
                      </Select>;
                    },
                  },
                  {
                    title: "输出字段",
                    dataIndex: "dataOutputName",
                    render: (text, record) => {
                      return record.valueType === "input" ? <Input
                        onChange={res => this.onChangeMicroService(res.target && res.target.value, item, record, "paramDefaultValue", type)}
                        value={record.paramDefaultValue} placeholder={"请输入输出字段"} /> :
                        <Select onChange={res => this.onChangeMicroService(res, item, record, "dataOutputName", type)}
                          value={record.dataOutputName || undefined}
                          placeholder={"请选择输出字段"}>
                          {record && (record.dataOutputs || [])
                            .map(option => {
                              return <Option value={option.name}>{option.name}</Option>;
                            })}
                        </Select>;
                    },
                  },
                ]} dataSource={item.envList} pagination={true} />
            </Col>
          </Row>
        </div>}

      </div>
    );
  };

  handleChangeK8SMicroservice = (res, index, name, type) => {
    let list = this.state[type];
    list[index][name] = res;
    this.setState({ [type]: list });
  };

  //选择微服务后续参数
  selectMicroService = () => {
    let { jobList, resourceAddonList, internalAddonList, k8SMicroserviceList } = this.state;
    const { getFieldDecorator, getFieldValue } = this.props.form;
    return <Row type="flex" align="middle" className="steps-action" style={{ marginBottom: 15 }}>
      <Col span={21} offset={3}> <Collapse defaultActiveKey={getFieldValue("packageTypes")} onChange={this.onChange}>
        <Panel header="微服务" key="K8S_MICROSERVICE">
          {!!k8SMicroserviceList.length ? k8SMicroserviceList.map((item, index) => {
            return this.formatK8SMicroServiceSelect(item, index, "k8SMicroserviceList");
          }) : <div>没有需要打包的微服务</div>}
        </Panel>
        <Panel header="Job" key="K8S_JOB">
          {!!jobList.length ? jobList.map((item, index) => {
            return this.formatK8SMicroServiceSelect(item, index, "jobList");
          }) : <div>没有需要打包的微服务</div>}
        </Panel>
        <Panel header="资源 Addon" key="RESOURCE_ADDON">
          <div>
            已选择的资源 Addon
          </div>
          <div>
            {
              !!resourceAddonList.length ? resourceAddonList.map((item, index) => {
                return <Row type="flex" align="middle">
                  {item.componentLabel}
                </Row>;
              }) : <div>没有需要打包的资源 Addon</div>
            }
          </div>
        </Panel>
        <Panel header="内置服务组件" key="INTERNAL_ADDON">
          <div>
            已选择的内置 Addon
          </div>
          <div>
            {
              !!internalAddonList.length ? internalAddonList.map(item => {
                return <Row type="flex" align="middle">
                  {item.componentLabel}
                </Row>;
              }) : <div>没有需要打包的内置 Addon</div>
            }
          </div>
        </Panel>
      </Collapse>
      </Col>
    </Row>;
  };

  render() {
    const { getFieldDecorator, getFieldValue } = this.props.form;
    let { packageData, serviceMap, selectedMicroServiceRowKeys, selectedJobRowKeys, selectedK8SMicroServiceRowKeys, loading, selectedResourceAddonRowKeys, selectedInternalAddonRowKeys, columns, current, microserviceList } = this.state;
    return (
      <Spin spinning={loading}>
        <div className="flyadmin-package"><Form labelCol={{ span: 5 }} wrapperCol={{ span: 12 }}>
          <Row className="steps-action" style={{ marginBottom: 15 }}>
            <Col span={14} offset={5}>
              <Steps size="small" current={current}>
                <Step title="基本信息" />
                <Step title="应用打包" />
                <Step title="完成" />
              </Steps>
            </Col>
          </Row>
          <div style={{ marginTop: 20 }}>
            {current === 0 && <div>
              <Form.Item label="版本号" {...formItemLayout}>
                {getFieldDecorator("version", {
                  initialValue: packageData.version,
                  rules: [{ required: true, message: "请输入版本号" }],
                })(<Input />)}
              </Form.Item>
              <Form.Item label="打包内容" {...formItemLayout}>
                {getFieldDecorator("packageTypes", {
                  initialValue: packageData.packageTypes || [],
                  rules: [{ required: true, message: "请选择需要打包的内容" }],
                })(<Checkbox.Group options={options} />)}
              </Form.Item>
              <Form.Item label="标签" {...formItemLayout}>
                {getFieldDecorator("tags", {
                  rules: [{ required: true, message: "请填写标签" }],
                  initialValue: packageData.tags || [],
                })(<Select mode="tags" style={{ width: "100%" }} placeholder="Tags Mode">
                </Select>)}
              </Form.Item>
              {!!getFieldValue("packageTypes").length && <Row style={{ marginBottom: 10 }}>
                <Col span={18} offset={6}>
                  <Collapse defaultActiveKey={getFieldValue("packageTypes")} onChange={this.onChange}>
                    {getFieldValue("packageTypes").includes("MICROSERVICE") &&
                      <Panel header="微服务" key="MICROSERVICE">
                        {/*没有定义rowKey selectedRowKeys直接用index标识*/}
                        <Table pagination={false}
                          size="small"
                          rowKey="componentName"
                          rowSelection={{
                            selectedRowKeys: selectedMicroServiceRowKeys,
                            onChange: res => this.onSelectMicroServiceChange(res, "selectedMicroServiceRowKeys"),
                          }}
                          columns={columns}

                          dataSource={serviceMap["MICROSERVICE"].concat(serviceMap["K8S_MICROSERVICE"])} />
                      </Panel>}
                    {getFieldValue("packageTypes").includes("K8S_JOB") &&
                      <Panel header="Job" key="K8S_JOB">
                        <Table pagination={false}
                          size="small"
                          rowSelection={{
                            selectedRowKeys: selectedJobRowKeys,
                            onChange: res => this.onSelectMicroServiceChange(res, "selectedJobRowKeys"),
                          }}
                          rowKey="componentName"
                          columns={columns}
                          dataSource={serviceMap["K8S_JOB"]} />
                      </Panel>}
                    {getFieldValue("packageTypes").includes("RESOURCE_ADDON") &&
                      <Panel header="资源 Addon" key="RESOURCE_ADDON">
                        <Table pagination={false}
                          size="small"
                          rowSelection={{
                            selectedRowKeys: selectedResourceAddonRowKeys,
                            onChange: this.onSelectResourceAddonChange,
                          }}
                          rowKey="componentName"
                          columns={columns}
                          dataSource={serviceMap["RESOURCE_ADDON"]} />
                      </Panel>}
                    {getFieldValue("packageTypes").includes("INTERNAL_ADDON") &&
                      <Panel header="内置 Addon" key="INTERNAL_ADDON">
                        <Table pagination={false}
                          size="small"
                          rowSelection={{
                            selectedRowKeys: selectedInternalAddonRowKeys,
                            onChange: this.onSelectInternalAddonChange,
                          }}
                          rowKey="componentName"
                          columns={columns}
                          dataSource={serviceMap["INTERNAL_ADDON"]} />
                      </Panel>}
                    {getFieldValue("packageTypes").includes("K8S_MICROSERVICE") &&
                      <Panel header="K8S微服务" key="K8S_MICROSERVICE">
                        <Table pagination={false}
                          size="small"
                          rowSelection={{
                            selectedRowKeys: selectedK8SMicroServiceRowKeys,
                            onChange: this.onSelectK8SMicroServiceChange,
                          }}
                          rowKey="componentName"
                          columns={columns}
                          dataSource={serviceMap["K8S_MICROSERVICE"]} />
                      </Panel>}
                  </Collapse>
                </Col>
              </Row>}
            </div>}
            {current === 1 && this.selectMicroService()}
            {current === 2 && <div style={{ textAlign: "center" }}>
              <CheckCircleFilled className="success-icon" />
              <div className="tooltip-text">应用打包成功</div>
              <div>
                <Button type="primary"
                  onClick={() => this.props.closeAction && this.props.closeAction(true)}>返回列表</Button>
              </div>
            </div>}
          </div>
          {this.footBar()}
        </Form>
        </div>
      </Spin>
    );
  }

}

FlyAdminAddPackage.propTypes = {};
FlyAdminAddPackage.defaultProps = {};

export default Form.create()(FlyAdminAddPackage);