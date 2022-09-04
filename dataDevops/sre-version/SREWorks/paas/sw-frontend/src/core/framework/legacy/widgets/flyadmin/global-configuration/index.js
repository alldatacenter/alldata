import React from "react";
import flyAdminService from "../service";
import { message, Spin } from "antd";
import uuidv4 from "uuid/v4";
import FlyAdminEditable from "../component/Editable";


class FlyAdminGlobalConfiguration extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      allCanEdit: false,
      canDelete: true,
      columns: [
        {
          title: "变量名称",
          dataIndex: "name",
          width: "30%",
          editable: true,
          required: true,
        },
        {
          title: "默认值",
          dataIndex: "defaultValue",
          editable: true,

        },
        {
          title: "备注信息",
          dataIndex: "comment",
          editable: true,
        },
      ],
      dataSource: [],
      count: 2,
    };
  }

  getList = () => {
    this.setState({
      loading: true,
    });
    flyAdminService.getGlobalConfiguration(this.props.nodeParams.app_id)
      .then(res => {
        res.map(item => {
          if (!item.key) {
            item.key = uuidv4();
          }
        });
        this.setState({
          loading: false,
          dataSource: res,
        });
      })
      .catch(err => {
        this.setState({
          loading: false,
        });
      });
  };

  componentDidMount() {
    this.getList();
  }

  handleSubmit = res => {
    let _arr = [];
    for (let data of res) {
      if (!data.name) {
        // document.getEle
        // console.log(document.getElementByDataRowKey(data.key));
        // let element = document.querySelector(`tr[data-row-key=${data.key}]`);
        message.error("有变量名未填写");
        return;
      } else {
        _arr.push(data.name);
      }
    }
    let _res = []; //
    _arr.sort();
    for (let i = 0; i < _arr.length;) {
      let count = 0;
      for (let j = i; j < _arr.length; j++) {
        if (_arr[i] === _arr[j]) {
          count++;
        }
      }
      _res.push([_arr[i], count]);
      i += count;
    }
    //_res 二维数维中保存了 值和值的重复数
    for (let i = 0; i < _res.length; i++) {
      // console.log(_res[i][0] + "重复次数:" + _res[i][1]);
      if (_res[i][1] > 1) {
        message.error(`${_res[i][0]}重复`);
        return;
      }
    }
    this.setState({ loading: true });
    flyAdminService.postGlobalConfiguration(res, this.props.nodeParams.app_id)
      .then(res => {
        message.success("保存成功");
        this.setState({
          loading: false,
          allCanEdit: false,
        }, this.getList);
      })
      .catch(err => {
        this.setState({
          loading: false,
        });
      });
  };

  render() {
    let { loading } = this.state;
    return (
      <div> {loading ? <Spin /> : <FlyAdminEditable canAdd {...this.props} {...this.state} onSubmit={this.handleSubmit} />}</div>
    );
  }
}

FlyAdminGlobalConfiguration.propTypes = {};
FlyAdminGlobalConfiguration.defaultProps = {};

export default FlyAdminGlobalConfiguration;