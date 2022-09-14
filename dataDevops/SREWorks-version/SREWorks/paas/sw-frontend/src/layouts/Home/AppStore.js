/**
 * Created by caoshuaibiao on 2020/12/14.
 */
import React from "react";
import { connect } from 'dva';
import './index.less';
import GridCheckBox from "../../components/GridCheckBox";

@connect(({ home }) => ({
  home: home
}))
export default class AppStore extends React.Component {

  constructor(props) {
    super(props);
    this.state = {};
  }

  addApp = () => {
    const { dispatch } = this.props;
    dispatch({ type: 'home/switchDeleteState' });
  };

  handleChange = (res) => {
    const { dispatch } = this.props;
    let checkedAppStore = {};
    res.map(item => {
      checkedAppStore[item.id] = true;
    });
    dispatch({ type: "home/updateCheckedAppStore", checkedAppStore });
  };

  onChangeDataSource = (res, deleteId) => {
    const { home, dispatch } = this.props;
    let { customQuickList, workspaces, collectList } = home;
    for (let i = 0; i < collectList.length; i++) {
      if (collectList[i].id === deleteId) {
        collectList.splice(i, 1);
        break;
      }
    }
    for (let i = 0; i < workspaces.length; i++) {
      for (let j = 0; j < workspaces[i].items.length; j++) {
        if (workspaces[i].items[j].id === deleteId) {
          workspaces[i].items.splice(j, 1);
          break;
        }
      }
    }
    dispatch({ type: "home/setDesktopData", customQuickList: res.filter(item => item.href), workspaces, collectList });
  };

  render() {
    const { home, isDelete } = this.props;
    const { appStore = [], checkedAppStore = [], customQuickList } = home;
    let checked = [];
    let list = appStore.concat(customQuickList);//部署后应用数据与自定义的快捷方式整合的数据
    list.map(item => {
      if (item.id && checkedAppStore[item.id]) {
        checked.push(item);
      }
    });
    return (<GridCheckBox dataSource={list}
      isSingle={false}
      paramsMap={{
        name: "appName",
        logo: "logoUrl",
        description: "introduction",
      }}
      onChangeDataSource={this.onChangeDataSource}
      isDelete={isDelete}
      checked={checked}
      onChange={this.handleChange} />
    );
  }
}