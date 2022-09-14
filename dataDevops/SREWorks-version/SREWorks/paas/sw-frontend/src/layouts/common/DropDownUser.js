import React from "react";
import localeHelper from "../../utils/localeHelper";
import * as util from "../../utils/utils";
import { LogoutOutlined } from "@ant-design/icons";
import {
  Menu,
  Avatar,
  Switch,
  Dropdown,
  Radio,
  Button,
  Tooltip
} from "antd";
import { connect } from "dva";
import cacheRepository from "../../utils/cacheRepository";
import NProgress from "nprogress";
import httpClient from "../../utils/httpClient";
import properties from "appRoot/properties";
import { CirclePicker } from "react-color";
import _ from 'lodash'
import Bus from '../../utils/eventBus'

const SubMenu = Menu.SubMenu;
const Item = Menu.Item;
const MenuItemGroup = Menu.ItemGroup;
let envs = [{ label: "开发", value: "dev" }, { label: "生产", value: "prod" }];
const colors = ["#f44336", "#e91e63", "#9c27b0", "#673ab7" , "#3f51b5", "#2196f3",
"#03a9f4","#00bcd4","#009688","#4caf50","#8bc34a","#cddc39",
"#ffeb3b","#ffc107","#ff6808","#ff5722","#795548","#607d8b"]
@connect(({ global, node }) => ({
  currentProduct: global.currentProduct,
  currentUser: global.currentUser,
  nodeParams: node.nodeParams,
}))
export default class DropDownUser extends React.Component {
  constructor(props) {
    super(props);
    const { currentProduct } = this.props;
    if (properties.deployEnv === "daily") {
      envs = [{ label: "日常", value: "dev" }];
    } else if (properties.deployEnv === "prepub") {
      envs = [{ label: "预发", value: "pre" }];
    }
    // if (properties.deployEnv !== "local" && !currentProduct.isNeedDailyEnv) {
    //   envs = envs.filter(env => env.value !== "dev");
    // }
    envs = currentProduct.environments;
    // 将stageId为空的过滤
    envs = envs.filter(function (item) { return item.stageId != "" });
    // envs.push({stageId:'dev'})
    // 配合后端暂时给每个应用都配置dev/prod环境
    this.state={
      selectedColor: '#00c1de'
    }
  }

  onSwitchTheme = () => {
    let themeType = (localStorage.getItem("sreworks-theme") === "navyblue" ? "light" : "navyblue");
    const { dispatch } = this.props;
    dispatch({ type: "global/switchTheme", theme: themeType });
    {/* global THEMES */
    }
    Bus.emit('themeChange',themeType);
    let color = localStorage.getItem("theme_color") || '#00c1de';
    let obj = _.cloneDeep(THEMES[themeType]);
    obj['@primary-color'] = color;
    obj['@link-color'] = color;
    window.less.modifyVars(obj);
    //window.location.reload();
  };

  onRoleChange = (roleId) => {
    /*let app=this.props.global.currentProduct.productId;
    cacheRepository.setRole(app,roleId);
    window.location.href=window.location.href.split("#")[0]+"#/"+app;
    window.location.reload();*/
    const { dispatch } = this.props;
    dispatch({ type: "global/switchRole", roleId: roleId });
  };


  onEnvChang = (envItem) => {
    const { currentProduct } = this.props;
    let app = currentProduct.productId;
    cacheRepository.setEnv(app, envItem);
    cacheRepository.setAppBizId(app, util.getNewBizApp().split(",")[1], envItem);
    window.location.reload();
  };

  onLanguageChange = (language) => {
    //暂时只支持两种语言
    let lng = localStorage.getItem("t_lang_locale") === "zh_CN" ? "en_US" : "zh_CN";
    localStorage.setItem("t_lang_locale", lng);
    //localeHelper.changeLocale(lng);
    const { dispatch } = this.props;
    dispatch({ type: "global/switchLanguage", language: lng });
    //window.location.reload();
  };

  onLogout = () => {
    const { dispatch } = this.props;
    dispatch({ type: "global/logout" });
  };
  setPrimaryColor = (targetObj) => {
    let color = targetObj.hex;
    let fadeColor = color+'21';
    this.setState({
      selectedColor: color
    })
    let themeType = (localStorage.getItem("sreworks-theme") === "navyblue" ? "navyblue" : "light");
    if (!window.less) {
      return
    } else {
      let obj = _.cloneDeep(THEMES[themeType]);
      obj['@primary-color'] = color;
      obj['@link-color'] = color;
      window.less.modifyVars(obj).then(res => {
        document.documentElement.style.setProperty('--PrimaryColor', color);
        document.documentElement.style.setProperty('--PrimaryBackColor', fadeColor);
        localStorage.setItem("theme_color", color);
        localStorage.setItem("back_color", fadeColor);
      })
    }
  }
  componentDidMount() {
    let themeType = (localStorage.getItem("sreworks-theme") === "navyblue" ? "navyblue" : "light");
    if (!window.less) {
      return
    }
    let color = localStorage.getItem("theme_color") || '#00bcd4';
    let backColor = localStorage.getItem("back_color") || '#00bcd421';
    this.setState({
      selectedColor: color
    })
    let obj = _.cloneDeep(THEMES[themeType]);
    obj['@primary-color'] = color;
    obj['@link-color'] = color;
    window.less.modifyVars(obj).then(res => {
      document.documentElement.style.setProperty('--PrimaryColor', color);
      document.documentElement.style.setProperty('--PrimaryBackColor', backColor);
      localStorage.setItem("theme_color", color);
      localStorage.setItem("back_color", backColor);
    })
  }
  render() {
    let { currentUser, currentProduct, isOnlyLogout } = this.props;
    let {selectedColor} = this.state;
    const roles = currentUser.roles, cacheRole = cacheRepository.getRole(currentProduct.productId);
    const currentRole = roles.filter(role => role.roleId === cacheRole)[0];
    const UserTitle = () => (
      <div style={{ maxWidth: 100, display: "flex", alignItems: "center", height: 50, marginLeft: 10,marginRight:20 }}>
        <Avatar
          style={{ verticalAlign: "middle" }}
          size={24}
          shape="square"
          src={currentUser.avatar ? currentUser.avatar : undefined}
          icon={currentUser.avatar ? undefined :
            <svg t="1627292435491" className="icon" viewBox="0 0 1024 1024" version="1.1"
              style={{ marginTop: 4 }}
              xmlns="http://www.w3.org/2000/svg" p-id="11495" width="24" height="24">
              <path
                d="M283.083845 290.799062c0 126.207423 102.313224 228.5186 228.511437 228.5186 126.2064 0 228.514507-102.311177 228.514507-228.5186 0-126.202307-102.308107-228.51553-228.514507-228.51553C385.390929 62.283532 283.083845 164.596755 283.083845 290.799062L283.083845 290.799062zM647.796314 519.854898c-39.302121 25.200962-86.044702 39.814798-136.202055 39.814798-50.154283 0-96.894817-14.613836-136.197962-39.814798-171.106006 56.998155-294.485011 218.435964-294.485011 408.697239 0 11.157107 0.422625 22.218024 1.254573 33.164331l858.852706 0c0.831948-10.946306 1.255597-22.007223 1.255597-33.164331 0-190.261275-123.372865-351.698061-294.483988-408.697239L647.796314 519.854898zM647.796314 519.854898"
                fill="#515151" p-id="11496"></path>
            </svg>
          }
        />
        <div style={{ display: "flex", flexDirection: "column", paddingLeft: "8px" }}>
          <span style={{ maxWidth: 50, lineHeight: 1.6, paddingTop: 0 }}
            className="text-overflow">{!currentUser ? "" : currentUser.nickNameCn || currentUser.nickName || currentUser.loginName || currentUser.name}</span>
        </div>
      </div>
    );
    return <Dropdown placement="bottomRight" overlay={
      <Menu style={{ width: 280 }}>
        {!isOnlyLogout && <MenuItemGroup title={localeHelper.get("common.theme.setting", "主题设置")}>
          <Menu.Item key="theme">
            <Switch onChange={this.onSwitchTheme}
              defaultChecked={localStorage.getItem("sreworks-theme") === "light"}
              size="small" checkedChildren="亮"
              unCheckedChildren="暗" />
          </Menu.Item>
        </MenuItemGroup>}
        <MenuItemGroup title="主色设置">
          <Menu.Item key="primaryColor">
            {/* <Tooltip title="点击切换">
          <div onClick={()=> this.setPrimaryColor()} style={{width:45,height:25,backgroundColor:'var(--PrimaryColor)'}}></div>
          </Tooltip> */}
            <CirclePicker colors={colors} color={selectedColor} onChange={this.setPrimaryColor} />
            {/* <Button onClick={()=> this.setPrimaryColor()}>主色设置</Button> */}
          </Menu.Item>
        </MenuItemGroup>
        {/* <MenuItemGroup title={localeHelper.get("common.language.setting", "语言设置")}>
          <Menu.Item key="language">
            <Switch onChange={this.onLanguageChange} checked={localStorage.getItem("t_lang_locale") === "zh_CN"}
                    size="small"
                    checkedChildren={localeHelper.get("common.language.zh_CN", "中文")}
                    unCheckedChildren={localeHelper.get("common.language.en_US", "英文")}/>
          </Menu.Item>
        </MenuItemGroup> */}
        <MenuItemGroup title={localeHelper.get("common.switchEnv", "环境切换")}>
          <Menu.Item key="switchEnv" style={{ marginBottom: 12 }}>
            <div onClick={(e) => e.stopPropagation()}>
              <Radio.Group defaultValue={util.getNewBizApp() && util.getNewBizApp().split(",")[2]}
                buttonStyle="solid" onChange={(e) => this.onEnvChang(e.target.value)}>
                {
                  envs.map(env =>
                    <Radio.Button value={env.stageId}
                      key={env.stageId}>{env.stageId}</Radio.Button>)
                }
              </Radio.Group>
            </div>
          </Menu.Item>
        </MenuItemGroup>
        {currentUser && currentUser.roles && !!currentUser.roles.length &&
          <MenuItemGroup title={localeHelper.get("common.switchRole", "角色切换")}>
            <Menu.Item key="switchRole" style={{ marginBottom: 12 }}>
              <div onClick={(e) => e.stopPropagation()}>
                <Radio.Group defaultValue={cacheRole} buttonStyle="solid"
                  onChange={(e) => this.onRoleChange(e.target.value)}>
                  {
                    currentUser.roles.map(role => <Radio.Button value={role.roleId}
                      key={role.roleId}>{role.roleName}</Radio.Button>)
                  }
                </Radio.Group>

              </div>
            </Menu.Item>
          </MenuItemGroup>}
        <MenuItemGroup>
          <Menu.Item key='logout'>
            <a
              onClick={this.onLogout}><LogoutOutlined /><span>{localeHelper.get("MainMenulogout", "登出")}</span></a>
          </Menu.Item>
        </MenuItemGroup>
      </Menu>
    }>
      <a>
        {this.props.children || <UserTitle />}
      </a>
    </Dropdown>;
  }
}
