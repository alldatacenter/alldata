/**
 * Created by caoshuaibiao on 2020/11/3.
 * 菜单树导航
 */
import React from "react";
import PropTypes from "prop-types";

import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import Bus from '../../../utils/eventBus'

import { Form, Icon as LegacyIcon } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import {
  Tree,
  Input,
  Popover,
  Popconfirm,
  Menu,
  Dropdown,
  Modal,
  Switch,
  Badge,
  message,
  Spin,
  Button,
  Select,
  Row,
  Col,
  InputNumber,
  Avatar,
  Tooltip,
  Radio,
} from "antd";
import _ from "lodash";
import "./index.less";
import uuid from "uuid/v4";
import service from "../../services/appMenuTreeService";
import * as util from '../../../utils/utils';
import Constants from '../../framework/model/Constants';

const { SubMenu } = Menu;
const { TextArea } = Input;
const { TreeNode, DirectoryTree } = Tree;
const Search = Input.Search;
const Option = Select.Option;
const { Item: MenuItem } = Menu;
const FormItem = Form.Item;
const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;

let description = "";
let appId = "app-dev";

class MenuNavigation extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      dirTree: [],
      expandedKeys: [],
      searchValue: "",
      autoExpandParent: true,
      nodeTempName: "",
      loading: false,
      nodeType: "custom",
      versionList: [],
      currentVersion: null,
      formKey: uuid(),
      allRoles: [],
      filterRoles: [],
      allAppTrees: [],
      selectedTreeServiceTypes: [],
      defaultExpandedKeys: [],
      nodeRoles: []
    };
    this.treeList = [];
    this._onExpand = this._onExpand.bind(this);
    this._onSearch = this._onSearch.bind(this);
    this._selectNode = this._selectNode.bind(this);
    appId = props.appId;
  }


  componentWillMount() {
    this._getDirTree();
    this.getRootRoles();
  }
  componentDidMount() {
    Bus.on('refreshDirTree', (msg) => {
      this._getDirTree(msg)
    });
  }
  componentWillUnmount() {
    Bus.off('refreshDirTree', (msg) => { });
  }
  componentWillReceiveProps(nextProps, nextContext) {
    if (!_.isEqual(this.props.bizKey, nextProps.bizKey) || !_.isEqual(this.props.parentId, nextProps.parentId)) {
      this._getDirTree();
    }
  }

  getRootRoles() {
    service.getRoles(appId).then(resp => {
      this.setState({
        allRoles: resp.items,
        filterRoles: resp.items,
      });
    });
  }
  _getDirTree(callbackFlag=false) {
    this.setState({
      loading: true,
    });
    let pathNodeMapping = {};
    const genPathNodeMapping = (nodeItem) => {
      pathNodeMapping[nodeItem.nodeTypePath] = nodeItem;
      let { children } = nodeItem;
      if (children && children.length) {
        children.forEach(child => {
          genPathNodeMapping(child)
        })
      }
    };
    service.getMenuTree(appId).then(res => {
      genPathNodeMapping(res);
      this.setState({
        dirTree: [res],
        selectedKeys: [],
        defaultExpandedKeys: [res.nodeTypePath],
        loading: false,
        pathNodeMapping: pathNodeMapping
      },()=> {
        if(callbackFlag){
          Bus.emit('returnTreeData',[res]);
          this.setState({
            selectedKeys:[callbackFlag]
          })
        }
      });
    });
  }


  _onExpand(expandedKeys) {
    this.setState({
      expandedKeys,
      autoExpandParent: false,
    });
  }

  _onSearch(e) {
    const value = e.target.value;
    const expandedKeys = this.treeList.map((item) => {
      if (item.label.indexOf(value) > -1 || item.name.indexOf(value) > -1) {
        return item.path;
      }
      return null;
    }).filter((item, i, self) => item && self.indexOf(item) === i);
    this.setState({
      expandedKeys,
      searchValue: value,
      autoExpandParent: true,
    });
  }

  // 递归查找

  _modifyNode(node) {
    const { allRoles } = this.state;
    this.getNodeLayout(node, this.state.dirTree, "modify");
    this.node = node;
    this.isAdd = false;
    if (node && node.level && node.level === 1) {
      this.getRootRoles();
    } else {
      let target = this._findNodeTrace(this.state.dirTree[0].children, node.level, { nodeTypePath: node.parentNodeTypePath })
      let selectedRoleList = (target && target.config && target.config.roleList) || []
      this.setState({
        filterRoles: allRoles.filter(item => {
          return selectedRoleList.includes(item.roleId)
        })
      })
    }

    this.setState({
      routeNodeName: node.name,
      routeNodeLabel: node.label,
      routeNodeKeywords: node.keywords,
      routeNodeConfig: node.config,
      visible: true,
      currentVersion: node.version,
      nodeRoles: node.config.roleList || []
    });
    // extData = node.extData ? JSON.parse(node.extData) : {}
    // service.getNodeRoles({nodeTypePath: node.nodeTypePath}).then(resp => {
    //     this.setState({
    //         routeNodeName: node.name,
    //         routeNodeLabel: node.label,
    //         routeNodeKeywords: node.keywords,
    //         routeNodeConfig: node.config,
    //         visible: true,
    //         currentVersion: node.version,
    //         nodeRoles:(resp.items || []).map(role => role.roleId)
    //     });
    // })

  }
  _findNodeTrace(arr = [], level, onlyKey) {
    let flatArr = [];
    if (!level || !onlyKey) {
      return {}
    }
    const deepFind = (arr) => {
      arr.forEach(item => {
        if (item.level === level - 1) {
          flatArr.push(item)
        } else if (item.level < level) {
          item.children && deepFind(item.children)
        } else {
          return
        }
      })
    }
    deepFind(arr);
    return _.find(flatArr, onlyKey)
  }
  _addNode(node) {
    let { dirTree, allRoles } = this.state;
    if (node && node.isRootNode) {
      this.getRootRoles();
    } else {
      this.setState({
        filterRoles: allRoles.filter(item => {
          return node.roleList.includes(item.roleId)
        })
      })
    }
    node.level > 1 && this.getNodeLayout(node, this.state.dirTree, "modify");
    this.isAdd = true;
    this.node = node;
    this.setState({
      visible: true,
    });
    // service.getAllServiceTypes({appTreeId: `${dirTree[0].name}|${dirTree[0].treeType}`})
    // .then(res => {
    //   this.allServiceType = res.items;
    //   console.log("this.allServiceType", this.allServiceType);
    //   node.level > 1 && this.getNodeLayout(node, this.state.dirTree, "modify");
    //   this.isAdd = true;
    //   this.node = node;
    //   this.setState({
    //     visible: true,
    //   });
    // });

  }

  _deleteNode(e, node) {
    e.stopPropagation();
    let params = {
      nodeTypePath: node.nodeTypePath
    }
    service.deleteNode(params)
      .then(res => {
        message.success("删除该菜单成功");
        this._getDirTree();
      });
  }

  _assignRoles(node) {
    let { allRoles } = this.state;
    service.getNodeRoles({ nodeTypePath: node.nodeTypePath }).then(resp => {
      let roles = [];
      Modal.confirm({
        title: "指定节点所属角色",
        maskClosable: false,
        width: 480,
        content: (
          <div style={{ display: "flex" }}>
            <span style={{ marginTop: 8 }}>所属角色:</span>
            <span style={{ width: 300 }}>
              <Select
                mode="multiple"
                style={{ width: "100%" }}
                defaultValue={(resp.items || []).map(role => role.roleId)}
                onChange={(selects) => roles = selects}
              >
                {
                  allRoles.map(role => {
                    return <Option key={role.roleId}>{role.name}</Option>;
                  })
                }
              </Select>
            </span>
          </div>
        ),
        onOk: () => {
          service.updateNodeRoles({
            roleIds: roles,
            nodeTypePath: node.nodeTypePath,
          }).then(res => {
            message.success("指定所属角色成功");
          }).catch(error => {
            message.error("指定所属角色出错:" + error.toString());
          });
        },
        onCancel: () => {
        },
      });
    });
  }

  getNodeLayout(node, dirTree, type) {
    dirTree.map((item) => {
      if (item.nodeTypePath == node.parentNodeTypePath) {
        this.parentNode = item;
        this.setState({ loading: false });
      } else {
        if (item.children && item.children.length > 0) {
          this.getNodeLayout(node, item.children, type);
        }
      }
    });
  }

  _selectNode(res) {
    console.log("_selectNode------->", res);
    this.props.onNodeClick(res.key[0]);
  }


  checkPathIsExist(rule, value, callback) {
    const form = this.props.form;
    let isTree = form.getFieldValue("routeNodeConfigIsTree");
    if (isTree) {
      if (value.indexOf("-") != -1) {
        callback("当为树节点时，标识中不能带\"-\"!");
      }
    }
    if (this.isAdd) {
      _.forEach(this.allServiceType, function (item) {
        if (item.serviceType === value) {
          callback("标识全局唯一，现树中已经存在同标识的菜单,请重新命名!");
        }
      });
      callback();
    } else {
      callback();
    }
  }


  menuTitleRender = (item) => {
    item.config = item.config || {};
    const menu = <Menu onClick={e => {
      e.domEvent.stopPropagation();
      switch (e.key) {
        case "modify-node":
          this._modifyNode(item);
          break;
        case "add-node":
          this._addNode(item);
          break;
        case "delete":
          break;
        case "assign-roles":
          this._assignRoles(item);
          break;
        case "verify-ds":
          this._handleVerifyDs(item);
          break;
      }
    }}>
      {!item.shortcutFlag && !item.isRootNode && <MenuItem key="modify-node"><span><EditOutlined style={{ color: 'var(--PrimaryColor)' }} />修改菜单信息</span></MenuItem>}
      {<MenuItem key="add-node"><span><PlusOutlined style={{ color: 'var(--PrimaryColor)' }} />新增子菜单</span> </MenuItem>}
      {/*{<MenuItem key="assign-roles"><span><Icon type="user" style={{color: '#2db7f5' }}/>指定所属角色</span> </MenuItem>}*/}
      {item.level !== 0 && item.config.type !== "buildIn" && (item.children === null || item.children && item.children.length === 0 || !item.children) &&
        <MenuItem key="delete" onClick={e => e.domEvent.stopPropagation()}>
          <span style={{ color: "red" }}>
            <Popconfirm title={"确认删除该菜单"} onConfirm={(e) => this._deleteNode(e, item)}>
              <a onClick={e => e.stopPropagation()}><DeleteOutlined style={{ color: 'var(--PrimaryColor)', marginRight: 6 }} />删除该菜单</a>
            </Popconfirm>
          </span>
        </MenuItem>}
    </Menu>;
    return (
      <span>
        <span>{item.label}</span>
        <Dropdown overlay={menu} trigger={["click"]}>
          <SettingOutlined
            style={{ marginLeft: 5, verticalAlign: "middle" }}
            onClick={e => e.stopPropagation()} />
        </Dropdown>
      </span>
    );
  };

  onSelect = (selectedKeys, e) => {
    let { pathNodeMapping } = this.state;
    this.setState({
      selectedKeys: selectedKeys,
    });
    let selectedKey = selectedKeys[0];
    if (selectedKey === `${appId}|app|T:`) {
      // message.info("应用根节点不支持定义页面！");
      return;
    }
    //console.log("pathNodeMapping------->",pathNodeMapping);
    this.props.onNodeClick && this.props.onNodeClick(selectedKey, pathNodeMapping[selectedKey]);

  }

  render() {
    const { menuFold } = this.props;
    const { filterRoles } = this.state;
    const { getFieldDecorator, validateFields } = this.props.form;
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
    const submitFormLayout = {
      wrapperCol: {
        xs: { span: 24, offset: 0 },
        sm: { span: 10, offset: 7 },
      },
    };
    // if (menuFold) {
    //   return <div style={{textAlign: "center"}}><Icon type="setting"/></div>;
    // }
    const { searchValue, defaultExpandedKeys, selectedKeys, allRoles, nodeRoles } = this.state;
    const loop = data => data.map((item) => {
      item.config = item.config || {};
      var name = "";
      if (searchValue !== "") {
        const index = item.label.indexOf(searchValue);
        const beforeStr = item.label.substr(0, index);
        const afterStr = item.label.substr(index + searchValue.length);
        name = index > -1 ? (
          <span>
            {beforeStr}
            <span style={{ color: "#f50" }}>{searchValue}</span>
            {afterStr}
          </span>
        ) : (item.label === "root" ? <span>/</span> : <span>{item.label}</span>);
      } else {
        name = (item.label === "root" ? <span>/</span> : <span>{item.label}</span>);
      }
      if (item.children && !!item.children.length) {
        return (
          <TreeNode key={item.nodeTypePath} title={this.menuTitleRender(item)} selectable={!item.isRootNode}>
            {loop(item.children)}
          </TreeNode>
        );
      }
      return <TreeNode isLeaf key={item.nodeTypePath} title={this.menuTitleRender(item)} />;
    });
    const tree = () => {
      return <DirectoryTree selectedKeys={selectedKeys} defaultExpandedKeys={defaultExpandedKeys} onSelect={this.onSelect} onExpand={this.onExpand}>
        {loop(this.state.dirTree)}
      </DirectoryTree>;
    };
    return (
      <div style={this.props.style}>
        <Spin spinning={this.state.loading}>
          {menuFold && <Popover placement="right" content={this.state.dirTree.length && tree()}>
            <div style={{ textAlign: "center", marginTop: 10 }}><SettingOutlined /></div>
          </Popover>}
          {!menuFold && (this.state.dirTree.length !== 0) && tree()}
        </Spin>
        {this.node && <Modal
          width={800}
          title={this.isAdd ? this.node.label ? "菜单(" + this.node.label + ")下新增子菜单" : "新增子菜单" :
            <div><span>修改菜单({this.node.label || ''})信息</span>
            </div>}
          destroyOnClose
          visible={this.state.visible}
          onCancel={this.handleCancel.bind(this)}
          footer={[<Button onClick={this.handleCancel.bind(this)}>取消</Button>,
          <Button type='primary' loading={this.state.modalLoading}
            onClick={this.handleOk.bind(this)}>{this.isAdd ? "新增" : "修改"}</Button>]}
        >
          {<div style={{ marginTop: 5 }}>

            <Form className="migrate-form" key={this.state.formKey}>
              <div>
                {<FormItem label="菜单路径" {...formItemLayout}>
                  {getFieldDecorator("routeNodeName", {
                    initialValue: this.isAdd ? "" : this.node.config.name,
                    rules: [{
                      required: true,
                      validator: (rule, value, callback) => {
                        if (!value) {
                          callback(new Error("请输入菜单路径"))
                        }
                        let regE = new RegExp('^[0-9a-zA-Z_-]{1,}$');
                        if (!regE.test(value)) {
                          callback(new Error('路径只限输入数字、字母、下划线、中短横'))
                        } else {
                          callback()
                        }
                      }
                    }],
                  })(
                    <Input placeholder="请输入菜单路径"
                      disabled={!this.isAdd && this.node.config.type == "buildIn" ? true : false} />,
                  )}
                </FormItem>
                }
                <FormItem label="菜单名称" {...formItemLayout}>
                  {getFieldDecorator("routeNodeLabel", {
                    initialValue: this.isAdd ? "" : this.node.config.label,
                    rules: [{
                      required: true,
                      message: "请输入菜单名称",
                    }],
                  })(
                    <Input placeholder="请输入菜单名称"
                      disabled={!this.isAdd && this.node.config.type == "buildIn" ? true : false} />,
                  )}
                </FormItem>
                <FormItem label="所属角色" {...formItemLayout}>
                  {getFieldDecorator("routeNodeRoles", {
                    initialValue: this.isAdd ? filterRoles.map(role => { return role.roleId }) : nodeRoles,
                  })(
                    <Select mode="multiple" placeholder="选择该菜单可被那些角色访问">
                      {
                        filterRoles.map(role => {
                          return <Option key={role.roleId}>{role.name}</Option>;
                        })
                      }
                    </Select>
                  )}
                </FormItem>
                <FormItem label="菜单序号" {...formItemLayout}>
                  {getFieldDecorator("routeNodeConfigOrder", {
                    initialValue: this.isAdd ? 0 : this.node.config.order,
                    rules: [{
                      required: false,
                      message: "请输入菜单序号",
                    }],
                  })(
                    <InputNumber placeholder="请输入菜单名称" style={{ width: "100%" }} min={0} />,
                  )}
                </FormItem>
                <FormItem label={<span>菜单Icon <Tooltip
                  title={<span>填写<a href='https://ant.design/components/icon-cn/' target='_blank'>antd icon</a>的type</span>}><QuestionCircleOutlined style={{ verticalAlign: "middle" }} /></Tooltip></span>} {...formItemLayout}>
                  {getFieldDecorator("routeNodeConfigIcon", {
                    initialValue: this.isAdd ? "" : this.node.config.icon,
                  })(
                    <Input placeholder="请输入菜单Icon" style={{ width: "100%" }} onChange={e => this.setState({
                      nodeConfigIcon: e.target.value,
                    })} />,
                  )}
                  <LegacyIcon type={this.state.nodeConfigIcon} style={{ marginLeft: 5, position: "absolute", top: 3 }} />
                </FormItem>
                <FormItem
                  label={"菜单类型"}
                  {...formItemLayout}
                >
                  {getFieldDecorator("routeNodeType", {
                    initialValue: this.isAdd ? "default" : (this.node.config.redirect && this.node.config.redirect.path ? "link" : "default"),
                  })(
                    <RadioGroup>
                      <Radio value="default">普通</Radio>
                      <Radio value="link">超链接</Radio>
                    </RadioGroup>,
                  )}
                </FormItem>
                {this.props.form.getFieldValue("routeNodeType") === "default" &&
                  <FormItem
                    label={"页面布局"}
                    {...formItemLayout}
                  >
                    {getFieldDecorator("pageLayoutType", {
                      initialValue: this.isAdd ? Constants.PAGE_LAYOUT_TYPE_FLUID : this.node.config.pageLayoutType || Constants.PAGE_LAYOUT_TYPE_FLUID,
                    })(
                      <RadioGroup>
                        <Radio disabled={true} value={Constants.PAGE_LAYOUT_TYPE_CUSTOM}>自定义布局</Radio>
                        <Radio value={Constants.PAGE_LAYOUT_TYPE_FLUID}>流式布局</Radio>
                      </RadioGroup>,
                    )}
                  </FormItem>
                }
                {this.props.form.getFieldValue("routeNodeType") === "default" &&
                  <FormItem label="是否隐藏" {...formItemLayout}>
                    {getFieldDecorator("routeNodeConfigHidden", {
                      valuePropName: "checked",
                      initialValue: this.isAdd ? false : this.node.config.hidden,
                    })(
                      <Switch checkedChildren="是" unCheckedChildren="否" />,
                    )}
                  </FormItem>
                }
                {this.props.form.getFieldValue("routeNodeType") === "default" && this.node && this.node.children.length > 0 &&
                  <FormItem
                    label={"子菜单位置"}
                    {...formItemLayout}
                  >
                    {getFieldDecorator("routeNodeConfigLayout", {
                      initialValue: this.isAdd ? "left" : this.node.config.layout,
                    })(
                      <RadioGroup onChange={this.layoutchange.bind(this)} buttonStyle="solid">
                        <RadioButton value="left">左侧</RadioButton>
                        <RadioButton value={"top"}>顶部</RadioButton>
                        {
                          this.node.level === 1 ? <RadioButton value="popup">弹出</RadioButton> : null
                        }
                        <RadioButton value="custom"><Tooltip
                          title="子菜单的生成交给页面中的自定义组件,不按照统一模式进行生成">布局组件</Tooltip></RadioButton>
                      </RadioGroup>,
                    )}
                  </FormItem>
                }
                {this.props.form.getFieldValue("routeNodeType") === "default" && this.node && this.node.children.length > 0 &&
                  <FormItem label="子菜单头文本" {...formItemLayout}>
                    {getFieldDecorator("routeNodeConfigMenuBarHeader", {
                      initialValue: this.isAdd ? "" : this.node.config.menuBarHeader,
                    })(
                      <Input placeholder="请输入子菜单头文本，没有文本则不展示" />,
                    )}
                  </FormItem>
                }
                <FormItem label="子菜单url初始参数" {...formItemLayout}>
                  {getFieldDecorator("acceptUrlParams", {
                    initialValue: this.isAdd ? "" : this.node.config.acceptUrlParams,
                  })(
                    <Input placeholder="此节点下的子菜单url中继承的参数名列表，以‘,’分隔" />,
                  )}
                </FormItem>
                {this.props.form.getFieldValue("routeNodeConfigHidden") &&
                  <FormItem label="隐藏时选中路径" {...formItemLayout}>
                    {getFieldDecorator("selectPathInHidden", {
                      initialValue: this.isAdd ? "" : this.node.config.selectPathInHidden,
                      rules: [{
                        required: false,
                        message: "",
                      }],
                    })(
                      <Input placeholder="节点菜单隐藏时,当内容区展示的是此节点的内容时需要显示的菜单路径" />,
                    )}
                  </FormItem>
                }

                {this.props.form.getFieldValue("routeNodeType") === "link" &&
                  <FormItem label="链接跳转" {...formItemLayout}>
                    {getFieldDecorator("routeNodeConfigRedirectPath", {
                      initialValue: this.isAdd ? "" : (this.node.config.redirect ? this.node.config.redirect.path || "" : ""),
                      rules: [{
                        required: true,
                        message: "请输入链接跳转",
                      }],
                    })(
                      <Input placeholder="菜单点击时的跳转地址，如果点击菜单无需跳转，则不用填写" />,
                    )}
                  </FormItem>
                }
                <FormItem label="标签Tags" {...formItemLayout}>
                  {getFieldDecorator("routeNodeKeywords", {
                    initialValue: this.isAdd ? "" : (this.node.keywords ? this.node.keywords || "" : ""),
                  })(
                    <Input placeholder="该菜单搜索的检索关键字,多个关键字用','分割" />,
                  )}
                </FormItem>
              </div>

            </Form>
          </div>}
        </Modal>}
      </div>
    );
  }


  handleCancel() {
    this.setState({ visible: false });
  }

  layoutchange(e) {
    this.setState({ nodeTempLayout: e.target.value });
  }

  clearNodeLayout(items, values, desc) {
    items.map((item) => {
      if (item.config) {
        if (item.config.layout && item.config.type !== "service") {
          item.config.layout = "left";

          let params = {
            nodeTypePath: item.nodeTypePath,
            serviceType: item.serviceType,
            config: Object.assign({}, item.config),
          };
          service.updateNode(item.version, params, desc)
            .then(res => {
              this.setState({ visible: false, modalLoading: false });
              this._getDirTree();
              if (item.children && item.children.length > 0) {
                this.clearNodeLayout(item.children, values, desc);
              }

            }).catch(error => {
              this.setState({ modalLoading: false });
            });
        }
      }

    });
  }


  updateRoles = (newRoles, nodeTypePath) => {
    let { nodeRoles } = this.state;
    let params = {
      roleIds: newRoles,
      nodeTypePath: nodeTypePath,
    }
    if (!_.isEqual(newRoles, nodeRoles)) {
      service.updateNodeRoles(params)
    }
  };


  handleOk() {
    const { filterRoles } = this.state;
    this.props.form.validateFields((err, values) => {
      if (!err) {
        // console.log(this.props.getFieldsValue(),'获取所有字段值========menuNavigation');
        this.setState({ modalLoading: true });
        if (this.isAdd) {
          let params = {};
          if (values["sourceType"] == "linkFromCommonApp") {
            params = {
              config: {},
              version: 0,
              parentNodeTypePath: this.node.nodeTypePath,
              serviceTypeTreeType: values["treeType"],
              serviceTypeAppId: "common",
              serviceType: values["serviceType"],
            };
          } else {
            params = {
              category: "node",
              parentNodeTypePath: this.node.nodeTypePath,
              version: 0,
              serviceType: util.generateId(),
              config: {
                nodeRoleOptionLists: filterRoles,
                roleList: values.routeNodeRoles || [],
                name: values.routeNodeName,
                label: values.routeNodeLabel,
                keywords: values.routeNodeKeywords,
                order: values.routeNodeConfigOrder,
                type: "custom",
                icon: values.routeNodeConfigIcon,
                layout: values.routeNodeConfigLayout,
                hidden: values.routeNodeConfigHidden,
                isDefault: false,
                menuBarHeader: values.routeNodeConfigMenuBarHeader,
                isTree: false,
                filterMode: values.routeNodeConfigFilterMode,
                filterDefaultValue: values.routeNodeConfigFilterDefaultValue,
                defaultMonId: values.defaultMonId,
                childrenRouteType: values.childrenRouteType,
                selectPathInHidden: values.selectPathInHidden,
                datasources: values.datasources,
                hideActionHistory: values.hideActionHistory,
                redirect: {
                  path: values.routeNodeConfigRedirectPath,
                },
                pageLayoutType: values.pageLayoutType,
              },
            };
          }
          service.insertNode(params)
            .then(res => {
              this.setState({ visible: false, modalLoading: false });
              this._getDirTree();
            }).catch(error => {
              this.setState({ modalLoading: false });
            });

        } else {
          let params = {
            nodeTypePath: this.node.nodeTypePath,
            version: this.node.version || 0,
            parentNodeTypePath: this.node.parentNodeTypePath,
            //serviceType: values.routeNodeServiceType||values.routeNodeName,
            config: Object.assign({}, this.node.config, {
              roleList: values.routeNodeRoles || [],
              name: values.routeNodeName,
              label: values.routeNodeLabel,
              keywords: values.routeNodeKeywords,
              order: values.routeNodeConfigOrder,
              type: "custom",
              icon: values.routeNodeConfigIcon,
              layout: values.routeNodeConfigLayout == "left" ? "left" : values.routeNodeConfigLayout,
              hidden: values.routeNodeConfigHidden,
              isDefault: false,
              menuBarHeader: values.routeNodeConfigMenuBarHeader,
              isTree: false,
              filterMode: values.routeNodeConfigFilterMode,
              filterDefaultValue: values.routeNodeConfigFilterDefaultValue,
              defaultMonId: values.defaultMonId,
              childrenRouteType: values.childrenRouteType,
              selectPathInHidden: values.selectPathInHidden,
              datasources: values.datasources,
              actionBar: values.actionBar,
              hideActionHistory: values.hideActionHistory,
              redirect: {
                path: values.routeNodeConfigRedirectPath,
              },
              pageLayoutType: values.pageLayoutType,
            }),
          };
          service.updateNode(params)
            .then(res => {
              this.setState({ visible: false, modalLoading: false });
              this._getDirTree();
            }).catch(error => {
              this.setState({ modalLoading: false });
            });

          // this.setState({layout : values.routeNodeConfigLayout})
        }
      }
    });

  }
}


MenuNavigation.propTypes = {
  // 通用可传递的 className 与 style 属性
  className: PropTypes.any,
  style: PropTypes.object,

  // 自定义所需要的属性
  apiPathPrefix: PropTypes.string,
  isCanEdit: PropTypes.bool,
  onNodeClick: PropTypes.func,

};

MenuNavigation.defaultProps = {
  parentId: 0,
  isCanEdit: true,
};

export default Form.create()(MenuNavigation);

const typeColor = {
  "buildIn": "#2db7f5",
  "custom": "#82c188",
  "service": "#8fece8",
  "manage": "#8fece8",
};

const typename = {
  "buildIn": "内",
  "custom": "自",
  "service": "服",
  "manage": "服",
};

