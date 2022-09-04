/**
 * Created by wangkaihua on 2021/3/24.
 * @params
 * @isSingle boolean 单选模式
 * @isDelete boolean 是否删除模式
 * @dataSource array 数据源
 * @checked array 已选项
 * @onChange function 回调函数
 * @paramsMap Object 格式化返回值与组件值
 *  @paramsMap id: 唯一标识
 *  @paramsMap name: 标题
 *  @paramsMap description: 描述
 *  @paramsMap logo: 图标
 */
import React from "react";
import { MinusCircleFilled } from '@ant-design/icons';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Avatar } from "antd";
import "./index.less";
import _ from "lodash";
import EllipsisTip from '../EllipsisTip';
const colors = ['#90ee90', '#2191ee', '#9a69ee', '#41ee1a', '#484aee', '#6B8E23', '#48D1CC', '#3CB371', '#388E8E', '#1874CD'];

export default class GridCheckBox extends React.Component {


  constructor(props) {
    super(props);
    this.state = {
      checked: [],
      checkedMap: {},
    };
  }

  addApp = () => {
    const { dispatch } = this.props;
    dispatch({ type: "home/switchDeleteState" });
  };

  componentDidMount() {
    let { checked } = this.props;
    let { checkedMap } = this.state;
    checkedMap = {};
    checked.map(item => {
      checkedMap[item.id] = true;
    });
    this.setState({
      checkedMap,
    });
  }

  componentWillReceiveProps(nextProps, nextContext) {
    if (!_.isEqual(nextProps, this.props)) {
      let { checked } = nextProps;
      let { checkedMap } = this.state;
      checkedMap = {};
      checked.map(item => {
        checkedMap[item.id] = true;
      });
      this.setState({
        checkedMap,
      });
    }
  }

  handleRemoveItem = (r, child) => {
    let { dataSource } = this.props;
    dataSource.splice(r, 1);
    this.props.onChangeDataSource(dataSource, child.id);
  };

  handleClick = child => {
    const { dataSource, isSingle, isDelete } = this.props;
    if (isDelete) return;
    let { checkedMap } = this.state;
    let value = [];
    //isSingle 单选模式
    if (isSingle) {
      checkedMap = {};
      checkedMap[child.id] = true;
      value.push(child);
    } else {
      checkedMap[child.id] = !checkedMap[child.id];
      dataSource.map(item => {
        if (checkedMap[item.id]) {
          value.push(item);
        }
      });
    }
    this.setState({
      checkedMap,
    });
    this.props.onChange && this.props.onChange(value);
  };

  render() {
    const { dataSource = [], paramsMap = {}, isDelete } = this.props;
    let { checkedMap } = this.state;
    return (
      <div className="grid-checkbox">
        {
          dataSource.map((child, r) => {
            let IconRender = null;
            if (child[paramsMap.logo] || child.logo) {
              IconRender = <img style={{ width: 40, height: 40 }} className="middle-icon" src={child[paramsMap.logo] || child.logo} />
            } else {
              IconRender = <Avatar style={{
                backgroundColor: colors[r % 10],
                verticalAlign: 'middle',
                fontSize: '18px'
              }}>
                {(child[paramsMap.name] || child.name) && (child[paramsMap.name] || child.name).substring(0, 1)}
              </Avatar>
            }
            //isDelete flyadmin定制需求
            return (
              <div className="app-items" onClick={() => this.handleClick(child)}>
                {
                  isDelete && child.href && <span className="removeHandle"
                    onClick={(e) => this.handleRemoveItem(r, child)}>
                    <MinusCircleFilled />
                  </span>
                }
                {!isDelete && <LegacyIcon type="check-circle"
                  className="check-img"
                  style={{ color: checkedMap[child[paramsMap.id] || child.id] ? "#1890fe" : "#b1b1b1" }}
                  theme={checkedMap[child[paramsMap.id] || child.id] ? "filled" : "outlined"} />}
                <div className="icon-img">
                  {IconRender}
                </div>
                <div className="right-text">
                  <div className="app-name">
                    <EllipsisTip tooltip={child[paramsMap.name] || child.name}>{child[paramsMap.name] || child.name}</EllipsisTip>
                  </div>
                  <div className="description">
                    <EllipsisTip tooltip={child[paramsMap.description] || child.description}>{child[paramsMap.description] || child.description}</EllipsisTip>
                  </div>
                </div>
              </div>
            );
          })
        }
      </div>
    );
  }
}