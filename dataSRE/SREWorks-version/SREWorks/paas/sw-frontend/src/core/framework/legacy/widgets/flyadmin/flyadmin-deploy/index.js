import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import {
  Input,
  Row,
  Col,
  Button,
  Select,
  Collapse,
  Radio,
  Tabs,
  Modal,
  Spin,
  message,
} from "antd";
import React from "react";
import SimpleForm from "../../../../../../components/FormBuilder/SimpleForm";
import flyAdminService from "../service";
import { connect } from "react-redux";
const { confirm } = Modal;
const { TabPane } = Tabs;
const RadioGroup = Radio.Group;
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
const needConfigComponent = ["K8S_JOB", "K8S_MICROSERVICE", "MICROSERVICE"];
@connect(({ node, global }) => ({
  userParams: Object.assign({}, { __currentUser__: global.currentUser }, node.userParams),
  userInfo: global.currentUser,
  product: global.currentProduct,
  nodeParams: Object.assign({}, node.nodeParams, node.urlParams, node.userParams, node.actionParams, node.remoteParams),
}))
class FlyAdminDeploy extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      components: [],
      loading: false,
      firstChoose: true,
      envList: [],
      currentEnv: "",
      listVar: [],
      loadingList: false,
      typeList: [],
      stageList: [],
      editTableConfig: {
        "dataSource": [],
        "pagination": true,
        "enableEdit": true,
        // "enableRemove": true,
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
            "title": "变量值",
          },
        ],
      },
    };
  }

  componentDidMount() {
    // this.setState({loading: true});
    // flyAdminService.getStageList({pageSize: 100, pageNumber: 1})
    //   .then(res => {
    //     if (res && res.items) {
    //       flyAdminService.getPackageTaskDetail({
    //         appId: this.props.app_id,
    //         appPackageTaskId: this.props.appPackageTaskId,
    //       })
    //         .then(detail => {
    //           if (detail.packageOptions && JSON.parse(detail.packageOptions)
    //             && JSON.parse(detail.packageOptions).components) {
    //             let components = JSON.parse(detail.packageOptions).components;
    //             let arr = [], typeList = [];
    //             components.map(item => {
    //               if (item.componentType) {
    //                 arr.push(item.componentType);
    //               }
    //             });
    //             Array.from(new Set(arr)).map(item => {
    //               typeList.push(this.formatComponentType(item));
    //             });
    //             this.setState({
    //               components,
    //               typeList,
    //               envList: res.items,
    //               loading: false,
    //             });
    //           }
    //         });
    //     }
    //   }).catch(err => {
    //   this.setState({loading: false});
    // });
    let { mode = {} } = this.props;
    let { config = {} } = mode;
    let params = {
      production: config.from === "localmart",
      pagination: false,
    };
    flyAdminService.getStageList(params)
      .then(res => {
        res.items.map(item => {
          item.label = item.stageName;
          item.value = item.stageId;
        });
        this.setState({
          stageList: res.items,
        });
      });
  }


  formatComponentType = type => {
    let obj = null;
    switch (type) {
      case "K8S_JOB":
        obj["name"] = "Job";
        obj["type"] = "K8S_JOB";
        break;
      case "K8S_MICROSERVICE":
      case "MICROSERVICE":
        obj["name"] = "微服务";
        obj["type"] = "K8S_MICROSERVICE";
        break;
    }
    return obj;
  };

  getListVar = () => {
    let { listVar, currentEnv } = this.state;
    this.setState({
      loadingList: true,
    });
    flyAdminService.getListVar(this.props.app_id)
      .then(res => {
        listVar = [];
        res.map(item => {
          listVar.push({ name: item.name, value: item[currentEnv] });
        });
        this.setState({ listVar, loadingList: false });
      }).catch(err => {
        this.setState({ loadingList: false });
      });
  };

  showConfirm = (env) => {
    confirm({
      title: "确认切换环境?",
      content: "切换环境会重新获取全局变量，请谨慎操作",
      onOk: () => {
        this.setState({ currentEnv: env }, this.getListVar);
      },
      onCancel: () => {
      },
    });
  };

  handleEnvChange = e => {
    if (!this.state.firstChoose && this.state.currentEnv && e.target.value && e.target.value !== this.state.currentEnv) {
      this.showConfirm(e.target.value);
    } else {
      this.setState({ currentEnv: e.target.value, firstChoose: false }, this.getListVar);
    }
  };

  handleSubmit = e => {
    let { mode = {} } = this.props;
    let { config = {} } = mode;
    if (this.preCustomButtonSubmit.validate()) {
      flyAdminService.postDeploy(this.preCustomButtonSubmit.validate().yaml, this.preCustomButtonSubmit.validate().autoEnvironment)
        .then(res => {
          message.success("部署成功");
          if (config.from === "localmart") {

          } else {
            window.open(`/#/${this.props.product.name}/dev/dev/package_manage?app_id=${this.props.app_id}&__tabsPath=deploy-setting`, "_blank");
          }
          this.props.onClose && this.props.onClose(true);
        });
    }
  };

  preCustomButtonSubmit = (params) => {
    // console.log("====params");
    return params;
  };

  close = () => {
    this.props.onClose && this.props.onClose();
  };

  render() {
    const { getFieldDecorator, getFieldValue } = this.props.form;
    let { envList, currentEnv, listVar, editTableConfig, loadingList, loading, typeList } = this.state;
    const onCancel = () => {

    }
    const displayItems = [{
      type: 3,
      name: "stage",
      initValue: "",
      required: true,
      label: "环境",
      optionValues: this.state.stageList,
    }, {
      type: 83,
      name: "yaml",
      initValue: "",
      required: true,
      label: "部署文件",
      defModel: {
        mode: "yaml",
        "disableShowDiff": true,
      },
    },
    {
      type: 16,
      name: "autoEnvironment",
      label: "是否继承环境变量",
    }];
    return <Spin spinning={loading}>
      <SimpleForm
        items={displayItems}
        onSubmit={this.preCustomButtonSubmit}
      />
      <div style={{ display: 'flex', justifyContent: 'center' }}>
        <Button onClick={this.close}>取消</Button>
        <Button style={{ marginLeft: 16 }} type="primary" onClick={this.handleSubmit}>确定</Button>
      </div>
    </Spin>;
  }

}


FlyAdminDeploy.propTypes = {};
FlyAdminDeploy.defaultProps = {};

export default Form.create()(FlyAdminDeploy);