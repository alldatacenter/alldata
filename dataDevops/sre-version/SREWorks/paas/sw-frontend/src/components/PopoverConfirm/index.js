/**
 * Created by caoshuaibiao on 2019/4/18.
 * 气泡弹出二次确认组件
 */
import React, { PureComponent, Component } from 'react';
import { Button, Popover } from 'antd';
import localeHelper from '../../utils/localeHelper';


export default class PopoverConfirm extends PureComponent {
  state = {
    visable: false,
  };
  show = (e) => {
    e && e.stopPropagation();
    this.setState({
      visable: true,
    })
  };
  hide = () => {
    this.setState({
      visable: false,
    })
  };
  handleVisibleChange = (visible) => {
    this.setState({ visable: visible });
  };
  render() {
    const { onOK, okText = localeHelper.get('common.okText', "确定"), title = localeHelper.get('popConfirm.delete?', "确认删除?"), target, children } = this.props;
    return (

      <Popover placement="top" title={title}
        visible={this.state.visable}
        onVisibleChange={this.handleVisibleChange}
        content={
          <div style={{ display: 'flex', justifyContent: 'space-around' }}>
            <Button type="danger" size={"small"} onClick={() => { onOK(); this.hide() }}>{okText}</Button>
            <Button size={"small"} onClick={this.hide}>{localeHelper.get('popConfirm.cancel', "取消")}</Button>
          </div>
        }
        trigger="click">
        <span onClick={this.show}>
          {target}
          {children}
        </span>
      </Popover>

    )
  }
}
