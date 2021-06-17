import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router';
import { Layout, Menu, Icon } from 'antd';
const { Header, Sider, Content } = Layout;
const { SubMenu } = Menu;

// 侧边栏菜单
class SiderMenu extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            collapsed: false,
        };
    }

    onCollapse = () => {
        this.setState({
            collapsed: !this.state.collapsed,
        });
    }

    render() {
        let currentNav = this.props.currentNav;
        return (
            <Sider
                // trigger={null}
                onCollapse={this.onCollapse}
                collapsible
                collapsed={this.state.collapsed}
                className="g_siderMenu"
            >
                {/* {<div className="logo"></div>} */}
                <div className="title">{!this.state.collapsed ? "system-devops-web" : "system-devops-web"}</div>
                <Menu
                    theme="dark"
                    mode="inline"
                    selectedKeys={[currentNav]}
                    defaultOpenKeys={['k8s']}
                >
                    <SubMenu key="k8s" title={<span><Icon type="code-o" /><span>K8S</span></span>}>
                        <Menu.Item key="project">
                            <Link to="/k8s/project"><Icon type="user" /><span>项目管理</span></Link>
                        </Menu.Item>
                        <Menu.Item key="machine">
                            <Link to="/k8s/machine"><Icon type="video-camera" /><span>机器管理</span></Link>
                        </Menu.Item>
                    </SubMenu>
                </Menu>
            </Sider>
        );
    }
}

SiderMenu.contextTypes = {
    router: PropTypes.object
}

export default SiderMenu;