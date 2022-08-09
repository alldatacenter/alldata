import React from 'react';
import { Layout, Menu, Icon, Popover, Button } from 'antd';
import { Link } from 'react-router';
import PropTypes from 'prop-types';
import InterfaceFn from 'js/interface.js';
const { Header } = Layout;
const SubMenu = Menu.SubMenu;

class HeaderContainer extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
        };
    }

    logout = () => {
        InterfaceFn.logout(() => {
            localStorage.setItem('pathname4redirect', '');
            localStorage.setItem('username', '');
            localStorage.setItem('roleData', '');
            this.context.router.push('/login');
        });
    }

    componentDidMount() {
       
    }

    render() {
        let username = localStorage.getItem('username');
        let roleData = localStorage.getItem('roleData');
        roleData = roleData ? JSON.parse(roleData) : {role: {}};
        

        return (
        	<Header className="g_head">
                {
                    // username && location.pathname.indexOf('login') < 0 ? <Menu
                    //     // theme="dark"
                    //     mode="horizontal"
                    //     selectedKeys={[this.props.currentNav]}
                    //     className="m_topNav"
                    // >
                    //     <Menu.Item key="project"><Link to="/project/detail">项目管理</Link></Menu.Item>
                    //     <Menu.Item key="pack"><Link to="/pack/list">打包任务</Link></Menu.Item>
                    // </Menu> : null
                }
                {
                    username && location.pathname.indexOf('login') < 0 ? <Menu
                        // theme="dark"
                        mode="horizontal"
                        className="m_topNav_Setting"
                    >
                        <Menu.Item key="account"><a href="javascript:;"><Icon type="user"/>{username}</a></Menu.Item>
                       
                        <Menu.Item key="logout">
                            <a href="javascript:;" onClick={this.logout} title="注销"><Icon type="logout" style={{margin: 0}} /></a>
                        </Menu.Item>
                    </Menu> : null
                }
	            {this.props.children}
	        </Header>
        );
    }
}

HeaderContainer.propTypes = {
    currentNav: PropTypes.string
};

HeaderContainer.contextTypes = {
    router: PropTypes.object
}


export default HeaderContainer;