import { message, Spin } from "antd";
import React from "react";
import flyAdminService from "../service";
import FlyAdminEditable from "../component/Editable";
import uuidv4 from "uuid/v4";

class FlyAdminNamespaceList extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      columns: [
        {
          title: "变量名称",
          dataIndex: "name",
        },
      ],
      dataSource: [],
    };
  }

  componentDidMount() {
    let { columns } = this.state;
    this.setState({
      loading: true,
    });
    flyAdminService.getStageList({ pageSize: 100, pageNumber: 1 })
      .then(res => {
        if (res && res.items) {
          res.items.map(item => {
            columns.splice(1, 0, {
              title: item.stageName,
              editable: true,
              dataIndex: item.stageId,
            });
          });
          this.setState({ columns }, this.getList);
        }
      });
  }

  getList = () => {
    flyAdminService.getListVar(this.props.nodeParams.app_id)
      .then(res => {
        res.map(item => {
          if (!item.key) {
            item.key = uuidv4();
          }
        });
        this.setState({
          dataSource: res,
          loading: false,
        });
      });
  };

  handleSubmit = res => {
    this.setState({ loading: true });
    flyAdminService.postVarEnv(res, this.props.nodeParams.app_id)
      .then(res => {
        message.success("保存成功");
        this.setState({
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
      <div> {loading ? <Spin /> :
        <FlyAdminEditable  {...this.props} {...this.state} onSubmit={this.handleSubmit} />}</div>
    );
  }

}

FlyAdminNamespaceList.propTypes = {};
FlyAdminNamespaceList.defaultProps = {};

export default FlyAdminNamespaceList;