import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Input, Row, Col, Button, Select, Collapse, Radio, Tabs, Modal, Spin } from "antd";
import React from "react";

import 'brace/mode/python';
import 'brace/theme/github'
import 'brace/mode/javascript';
import 'brace/theme/monokai';
import "brace/mode/yaml";
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

class FlyAdminDeployComDetail extends React.Component {
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
  }

  close = () => {
    this.props.closeAction && this.props.closeAction();
  };

  render() {
    return <Spin>

    </Spin>;
  }

}


FlyAdminDeployComDetail.propTypes = {};
FlyAdminDeployComDetail.defaultProps = {};

export default FlyAdminDeployComDetail;