import React from "react";
import { EditOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Table, Input, Button, Popconfirm, Card } from "antd";
import uuidv4 from "uuid/v4";
import "./index.scss"
const EditableContext = React.createContext();

const EditableRow = ({ form, index, ...props }) => (
  <EditableContext.Provider value={form}>
    <tr {...props} />
  </EditableContext.Provider>
);

const EditableFormRow = Form.create()(EditableRow);

class EditableCell extends React.Component {
  state = {
    editing: false,
    dataSource: [],
  };

  componentDidMount() {
    this.setState({
      dataSource: this.props.dataSource,
    });
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (JSON.stringify(prevProps.dataSource) !== JSON.stringify(this.props.dataSource)) {
      this.setState({
        dataSource: prevProps.dataSource,
      });
    }
  }

  save = e => {
    const { record, handleSave } = this.props;
    this.form.validateFields((error, values) => {
      handleSave({ ...record, ...values });
    });
  };

  renderCell = form => {
    this.form = form;
    const { children, dataIndex, record, title, required, allCanEdit } = this.props;
    return allCanEdit ? (
      <Form.Item style={{ margin: 0 }}>
        {form.getFieldDecorator(dataIndex, {
          rules: [
            {
              required: required,
              message: `请输入${title}`,
            },
          ],
          initialValue: record[dataIndex],
        })(<Input ref={node => (this.input = node)} onPressEnter={this.save} onBlur={this.save} />)}
      </Form.Item>
    ) : (
      <div
        className="editable-cell-value-wrap"
        style={{ paddingRight: 24, height: 18 }}
      >
        {children}
      </div>
    );
  };

  render() {
    const {
      editable,
      dataIndex,
      title,
      record,
      index,
      handleSave,
      children,
      ...restProps
    } = this.props;
    return (
      <td {...restProps}>
        {editable ? (
          <EditableContext.Consumer>{this.renderCell}</EditableContext.Consumer>
        ) : (
          children
        )}
      </td>
    );
  }
}

class FlyAdminEditable extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      allCanEdit: false,
      columns: [],
      dataSource: [],
      count: 2,
    };
  }


  componentDidMount() {
    this.setState({
      columns: this.props.columns,
      dataSource: this.props.dataSource,
      loading: this.props.loading,
    });
  }


  handleDelete = key => {
    const dataSource = [...this.state.dataSource];
    this.setState({ dataSource: dataSource.filter(item => item.key !== key) });
  };

  handleAdd = () => {
    const { dataSource } = this.state;
    const newData = {
      key: uuidv4(),
      name: ``,
      defaultValue: "",
      comment: "",
    };
    this.setState({
      dataSource: [newData, ...dataSource],
    });
  };

  handleSave = row => {
    const newData = [...this.state.dataSource];
    const index = newData.findIndex(item => row.key === item.key);
    const item = newData[index];
    newData.splice(index, 1, {
      ...item,
      ...row,
    });
    this.setState({ dataSource: newData });
  };

  handleEdit = () => {
    let { columns } = this.state;
    if (this.props.canDelete) {
      columns.push({
        title: "操作",
        dataIndex: "operation",
        render: (text, record) => <Popconfirm title="确认删除?" onConfirm={() => this.handleDelete(record.key)}>
          <a>删除</a>
        </Popconfirm>,
      });
    }
    this.setState({
      columns,
      allCanEdit: !this.state.allCanEdit,
    });
  };

  handleSubmit = () => {
    let operationList = this.state.columns.filter(a => a.dataIndex === "operation");
    let { columns } = this.state;
    if (!!operationList.length) {
      columns.splice(columns.length - 1, 1);
    }
    this.setState({
      columns,
    }, () => {
      this.props.onSubmit(this.state.dataSource);
    });

  };

  render() {
    const { dataSource, allCanEdit, loading } = this.state;
    const { canAdd } = this.props;
    const components = {
      body: {
        row: EditableFormRow,
        cell: EditableCell,
      },
    };
    const columns = this.state.columns.map(col => {
      if (!col.editable) {
        return col;
      }
      return {
        ...col,
        onCell: record => ({
          record,
          dataSource,
          allCanEdit: allCanEdit,
          required: col.required,
          editable: col.editable,
          dataIndex: col.dataIndex,
          title: col.title,
          handleSave: this.handleSave,
        }),
      };
    });
    return (
      <Card className="composition-card flyadmin-edit-table"
        size="small"
        title={
          <div style={{ display: "flex", justifyContent: "space-between", height: 48 }}>
            <div style={{ display: "flex" }}>
              <div style={{ alignItems: "center", display: "flex" }}>
                <div style={{ width: 4, height: 20, backgroundColor: "#2077FF" }}>
                  <div>
                    <b style={{ marginLeft: 12, marginRight: 12, fontSize: 14 }}>全局配置</b>
                  </div>
                </div>
              </div>
            </div>
            <div style={{ alignItems: 'center', float: "right" }}>
              <div style={{ height: 48, display: "flex", alignItems: "center" }}>
                {allCanEdit && canAdd &&
                  <Button size="small" onClick={this.handleAdd} style={{ marginRight: 10 }}>
                    添加数据
                  </Button>}
                {!allCanEdit &&
                  <Button icon={<EditOutlined />} size="small" onClick={this.handleEdit}>
                    编辑模式
                  </Button>}
                {allCanEdit && <Button size="small" type="primary" onClick={this.handleSubmit}>保存数据</Button>}
              </div>
            </div>
          </div>
        }>
        {/*<PagingTable otherTableProps={{loading,rowClassName:() => "editable-row"}} columns={columns} data={dataSource}/>*/}
        <Table
          loading={loading}
          components={components}
          rowClassName={() => "editable-row"}
          size="small"
          dataSource={dataSource}
          columns={columns}
        />
      </Card>
    );
  }
}

FlyAdminEditable.propTypes = {};
FlyAdminEditable.defaultProps = {};

export default FlyAdminEditable;